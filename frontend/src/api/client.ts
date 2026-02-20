import { ApiError, type ErrorResponseDto } from '@/types/error';
import { useAuthStore } from '@/stores';
import { isTokenExpired } from '@/utils/jwt';
import { queryClient } from '@/lib/queryClient';

// VITE_API_URL이 비어있으면 상대 경로 사용 (Vite 프록시 경유)
// 프로덕션에서는 VITE_API_URL에 실제 API 도메인을 설정
export const API_BASE_URL = import.meta.env.VITE_API_URL || '';

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

let refreshPromise: Promise<string> | null = null;

/**
 * 토큰 갱신을 보장하는 싱글턴 함수.
 * 이미 갱신 중이면 기존 Promise를 반환하여 중복 요청을 방지한다.
 */
function ensureRefresh(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = refreshAccessToken()
      .finally(() => { refreshPromise = null; });
  }
  return refreshPromise;
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
  if (currentToken && isTokenExpired(currentToken, 60) && !refreshPromise) {
    try {
      await ensureRefresh();
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
    // 먼저 응답 본문을 파싱하여 에러 유형을 확인
    const errorBody401 = (await response.json().catch(() => ({}))) as ErrorResponseDto;

    // 토큰 문제가 아닌 비즈니스 로직 에러(예: 비밀번호 변경 시 현재 비밀번호 불일치)는
    // 토큰 갱신 없이 바로 에러를 던짐
    const tokenErrorCodes: ReadonlySet<string> = new Set([
      'ACCESS_TOKEN_INVALID',
      'ACCESS_TOKEN_EXPIRED',
      'INVALID_TOKEN_TYPE',
      'TOKEN_EXPIRED',
    ]);

    if (errorBody401.code && !tokenErrorCodes.has(errorBody401.code)) {
      throw new ApiError(
        401,
        errorBody401.code,
        errorBody401.message ?? 'HTTP 401',
        errorBody401.timestamp
      );
    }

    try {
      const newAccessToken = await ensureRefresh();

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

      return {
        data,
        status: retryResponse.status,
        headers: retryResponse.headers,
      } as T;
    } catch (error) {
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
    const { status: _s, code, message, timestamp, ...extraData } = errorBody;

    throw new ApiError(
      response.status,
      (code as string | undefined) ?? `HTTP_${response.status}`,
      (message as string | undefined) ?? `HTTP ${response.status}`,
      timestamp as string | undefined,
      Object.keys(extraData).length > 0 ? (extraData as Record<string, unknown>) : undefined
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
