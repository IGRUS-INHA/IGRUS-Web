import type { FetchConfig, ApiResponse } from '@/types/api';

export const API_BASE_URL =
  import.meta.env.VITE_API_URL || 'http://localhost:8080';

// =============================================================================
// 토큰 관리
// =============================================================================

function getAccessToken(): string | undefined {
  const token = localStorage.getItem('accessToken');
  return token ?? undefined;
}

function getRefreshToken(): string | undefined {
  const token = localStorage.getItem('refreshToken');
  return token ?? undefined;
}

function setTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);
}

function clearTokens(): void {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
}

// =============================================================================
// 토큰 갱신
// =============================================================================

let isRefreshing = false;
let refreshSubscribers: Array<(token: string) => void> = [];

function subscribeTokenRefresh(callback: (token: string) => void): void {
  refreshSubscribers.push(callback);
}

function onTokenRefreshed(newToken: string): void {
  refreshSubscribers.forEach((callback) => callback(newToken));
  refreshSubscribers = [];
}

async function refreshAccessToken(): Promise<string> {
  const refreshToken = getRefreshToken();

  if (!refreshToken) {
    throw new Error('No refresh token available');
  }

  const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ refreshToken }),
  });

  if (!response.ok) {
    clearTokens();
    throw new Error('Token refresh failed');
  }

  const result = (await response.json()) as ApiResponse<{
    accessToken: string;
    refreshToken: string;
  }>;

  setTokens(result.data.accessToken, result.data.refreshToken);
  return result.data.accessToken;
}

// =============================================================================
// 로그아웃 처리
// =============================================================================

function handleLogout(): void {
  clearTokens();
  // 로그인 페이지로 리다이렉트
  window.location.href = '/login';
}

// =============================================================================
// 커스텀 Fetch
// =============================================================================

export async function customFetch<T>({
  url,
  method,
  params,
  data,
  headers,
  signal,
}: FetchConfig): Promise<T> {
  const queryString = params
    ? `?${new URLSearchParams(
        Object.entries(params)
          .filter(([, v]) => v !== undefined && v !== null)
          .map(([k, v]) => [k, String(v)])
      ).toString()}`
    : '';

  const accessToken = getAccessToken();

  const requestHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    ...headers,
  };

  // Authorization 헤더 자동 주입
  if (accessToken) {
    requestHeaders['Authorization'] = `Bearer ${accessToken}`;
  }

  const response = await fetch(`${API_BASE_URL}${url}${queryString}`, {
    method,
    signal: signal ?? null,
    headers: requestHeaders,
    body: data ? JSON.stringify(data) : null,
  });

  // 401 Unauthorized - 토큰 갱신 시도
  if (response.status === 401) {
    const refreshToken = getRefreshToken();

    // 리프레시 토큰도 없으면 로그아웃
    if (!refreshToken) {
      handleLogout();
      throw new Error('Authentication required');
    }

    // 이미 갱신 중이면 대기
    if (isRefreshing) {
      return new Promise<T>((resolve, reject) => {
        subscribeTokenRefresh((newToken: string) => {
          requestHeaders['Authorization'] = `Bearer ${newToken}`;
          fetch(`${API_BASE_URL}${url}${queryString}`, {
            method,
            signal: signal ?? null,
            headers: requestHeaders,
            body: data ? JSON.stringify(data) : null,
          })
            .then((retryResponse) => {
              if (!retryResponse.ok) {
                throw new Error(`HTTP ${retryResponse.status}`);
              }
              return retryResponse.json() as Promise<T>;
            })
            .then(resolve)
            .catch(reject);
        });
      });
    }

    // 토큰 갱신 시도
    isRefreshing = true;

    try {
      const newAccessToken = await refreshAccessToken();
      isRefreshing = false;
      onTokenRefreshed(newAccessToken);

      // 갱신된 토큰으로 재요청
      requestHeaders['Authorization'] = `Bearer ${newAccessToken}`;
      const retryResponse = await fetch(`${API_BASE_URL}${url}${queryString}`, {
        method,
        signal: signal ?? null,
        headers: requestHeaders,
        body: data ? JSON.stringify(data) : null,
      });

      if (!retryResponse.ok) {
        const errorBody = (await retryResponse.json().catch(() => ({}))) as {
          message?: string;
        };
        throw new Error(errorBody.message ?? `HTTP ${retryResponse.status}`);
      }

      // 204 No Content 처리
      if (retryResponse.status === 204) {
        return undefined as T;
      }

      return (await retryResponse.json()) as T;
    } catch (error) {
      isRefreshing = false;
      refreshSubscribers = [];
      handleLogout();
      throw error;
    }
  }

  // 일반 에러 처리
  if (!response.ok) {
    const errorBody = (await response.json().catch(() => ({}))) as {
      message?: string;
      code?: string;
    };
    const error = new Error(errorBody.message ?? `HTTP ${response.status}`);
    if (errorBody.code) {
      (error as Error & { code: string }).code = errorBody.code;
    }
    throw error;
  }

  // 204 No Content 처리
  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
