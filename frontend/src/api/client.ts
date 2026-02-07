export const API_BASE_URL =
  import.meta.env.VITE_API_URL || 'http://localhost:8080';

// =============================================================================
// 토큰 관리
// =============================================================================

function getAccessToken(): string | undefined {
  const token = localStorage.getItem('accessToken');
  return token ?? undefined;
}

function setAccessToken(accessToken: string): void {
  localStorage.setItem('accessToken', accessToken);
}

function clearAccessToken(): void {
  localStorage.removeItem('accessToken');
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
  // Refresh Token은 httpOnly 쿠키로 자동 전송됨
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/password/refresh`, {
    method: 'POST',
    credentials: 'include', // 쿠키 포함
    headers: {
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    clearAccessToken();
    throw new Error('Token refresh failed');
  }

  const result = (await response.json()) as {
    accessToken: string;
    expiresIn: number;
  };

  setAccessToken(result.accessToken);
  return result.accessToken;
}

// =============================================================================
// 로그아웃 처리
// =============================================================================

function handleLogout(): void {
  clearAccessToken();
  // Refresh Token 쿠키는 서버에서 삭제 필요 (로그아웃 API 호출 시)
  // 로그인 페이지로 리다이렉트
  window.location.href = '/login';
}

// =============================================================================
// 커스텀 Fetch
// =============================================================================

export async function customFetch<T>(
  url: string,
  options?: RequestInit
): Promise<T> {
  const accessToken = getAccessToken();

  const requestHeaders: Record<string, string> = {
    ...(options?.headers as Record<string, string>),
  };

  // Authorization 헤더 자동 주입
  if (accessToken) {
    requestHeaders['Authorization'] = `Bearer ${accessToken}`;
  }

  const response = await fetch(`${API_BASE_URL}${url}`, {
    ...options,
    credentials: 'include', // 쿠키 포함 (Refresh Token)
    headers: requestHeaders,
  });

  // 401 Unauthorized 또는 403 Forbidden - 토큰 갱신 시도
  // (403은 토큰 유효하지 않아 익명 사용자로 처리되어 권한 없음으로 나올 수 있음)
  // 단, public endpoint (로그인, 회원가입, 이메일 인증 등)는 제외
  const isPublicEndpoint =
    url.includes('/auth/password/login') ||
    url.includes('/auth/password/signup') ||
    url.includes('/auth/password/refresh') ||
    url.includes('/auth/password/verify-email') ||
    url.includes('/auth/password/resend-verification');

  if ((response.status === 401 || response.status === 403) && !isPublicEndpoint) {
    // 이미 갱신 중이면 대기
    if (isRefreshing) {
      return new Promise<T>((resolve, reject) => {
        subscribeTokenRefresh((newToken: string) => {
          const retryHeaders = {
            ...(options?.headers as Record<string, string>),
            'Authorization': `Bearer ${newToken}`,
          };
          fetch(`${API_BASE_URL}${url}`, {
            ...options,
            credentials: 'include',
            headers: retryHeaders,
          })
            .then(async (retryResponse) => {
              if (!retryResponse.ok) {
                throw new Error(`HTTP ${retryResponse.status}`);
              }

              const data = retryResponse.status === 204
                ? undefined
                : await retryResponse.json();

              resolve({
                data,
                status: retryResponse.status,
                headers: retryResponse.headers,
              } as T);
            })
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
      const retryHeaders = {
        ...(options?.headers as Record<string, string>),
        'Authorization': `Bearer ${newAccessToken}`,
      };
      const retryResponse = await fetch(`${API_BASE_URL}${url}`, {
        ...options,
        credentials: 'include',
        headers: retryHeaders,
      });

      if (!retryResponse.ok) {
        const errorBody = (await retryResponse.json().catch(() => ({}))) as {
          message?: string;
        };
        throw new Error(errorBody.message ?? `HTTP ${retryResponse.status}`);
      }

      const data = retryResponse.status === 204
        ? undefined
        : await retryResponse.json();

      return {
        data,
        status: retryResponse.status,
        headers: retryResponse.headers,
      } as T;
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

  const data = response.status === 204
    ? undefined
    : await response.json();

  return {
    data,
    status: response.status,
    headers: response.headers,
  } as T;
}
