import { ApiError, type ErrorResponseDto } from '@/types/error';
import { useAuthStore } from '@/stores';
import { isTokenExpired } from '@/utils/jwt';
import { queryClient } from '@/lib/queryClient';

// 환경변수가 제대로 로드되지 않으면 에러 발생
if (!import.meta.env.VITE_API_URL) {
  console.error('❌ VITE_API_URL 환경변수가 로드되지 않았습니다!');
  console.error('개발 서버를 재시작하세요: npm run dev');
}


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
  // Zustand store도 동기화
  useAuthStore.setState({ accessToken: result.accessToken });
  return result.accessToken;
}

// =============================================================================
// 로그아웃 처리
// =============================================================================

function handleLogout(): void {
  clearAccessToken();
  // TanStack Query 캐시 초기화 (이전 사용자 데이터 잔류 방지)
  queryClient.clear();
  // Zustand store 정리
  useAuthStore.setState({
    user: undefined,
    accessToken: undefined,
    refreshToken: undefined,
    isAuthenticated: false,
  });
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
  // 선제 토큰 갱신: 만료 60초 전이면 요청 전에 갱신 시도
  const currentToken = getAccessToken();
  if (currentToken && isTokenExpired(currentToken, 60) && !isRefreshing) {
    try {
      await refreshAccessToken();
    } catch {
      // 선제 갱신 실패 시 무시 — 기존 401 핸들러가 처리
    }
  }

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

  // 401 Unauthorized - 토큰 갱신 시도
  // 403 Forbidden - 권한 부족 (토큰 갱신 X, 에러만 던짐)
  // 단, public endpoint (로그인, 회원가입, 이메일 인증 등)는 제외
  const isPublicEndpoint =
    url.includes('/auth/password/login') ||
    url.includes('/auth/password/signup') ||
    url.includes('/auth/password/refresh') ||
    url.includes('/auth/password/verify-email') ||
    url.includes('/auth/password/resend-verification');

  // 403은 권한 부족이므로 토큰 갱신 없이 바로 에러 처리
  if (response.status === 403 && !isPublicEndpoint) {
    const errorBody = (await response.json().catch(() => ({}))) as ErrorResponseDto;

    throw new ApiError(
      403,
      errorBody.code ?? 'HTTP_403',
      errorBody.message ?? '권한이 없습니다',
      errorBody.timestamp
    );
  }

  if (response.status === 401 && !isPublicEndpoint) {
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
                const errorBody = (await retryResponse.json().catch(() => ({}))) as ErrorResponseDto;

                // 403은 권한 부족 (로그아웃 안 함)
                throw new ApiError(
                  retryResponse.status,
                  errorBody.code ?? `HTTP_${retryResponse.status}`,
                  errorBody.message ?? `HTTP ${retryResponse.status}`,
                  errorBody.timestamp
                );
              }

              const retryText = await retryResponse.text();
              const data = retryResponse.status === 204 || !retryText
                ? undefined
                : JSON.parse(retryText);

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
        const errorBody = (await retryResponse.json().catch(() => ({}))) as ErrorResponseDto;

        // 토큰 갱신 후에도 403이면 권한 부족 (로그아웃 안 함)
        if (retryResponse.status === 403) {
          isRefreshing = false;
          refreshSubscribers = [];
          throw new ApiError(
            retryResponse.status,
            errorBody.code ?? `HTTP_${retryResponse.status}`,
            errorBody.message ?? `HTTP ${retryResponse.status}`,
            errorBody.timestamp
          );
        }

        throw new ApiError(
          retryResponse.status,
          errorBody.code ?? `HTTP_${retryResponse.status}`,
          errorBody.message ?? `HTTP ${retryResponse.status}`,
          errorBody.timestamp
        );
      }

      const retryText2 = await retryResponse.text();
      const data = retryResponse.status === 204 || !retryText2
        ? undefined
        : JSON.parse(retryText2);

      return {
        data,
        status: retryResponse.status,
        headers: retryResponse.headers,
      } as T;
    } catch (error) {
      isRefreshing = false;
      refreshSubscribers = [];

      // 403 권한 부족 에러는 로그아웃하지 않음
      if (error instanceof ApiError && error.status === 403) {
        throw error;
      }

      handleLogout();
      throw error;
    }
  }

  // 일반 에러 처리
  if (!response.ok) {
    const errorBody = (await response.json().catch(() => ({}))) as ErrorResponseDto;

    throw new ApiError(
      response.status,
      errorBody.code ?? `HTTP_${response.status}`,
      errorBody.message ?? `HTTP ${response.status}`,
      errorBody.timestamp
    );
  }

  const text = await response.text();
  const data = response.status === 204 || !text ? undefined : JSON.parse(text);

  return {
    data,
    status: response.status,
    headers: response.headers,
  } as T;
}
