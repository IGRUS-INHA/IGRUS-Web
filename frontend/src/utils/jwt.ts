/**
 * JWT 토큰 유틸리티
 * 클라이언트 사이드에서 토큰 만료 체크를 위한 디코딩 함수
 * (서명 검증은 서버에서만 수행)
 */

export interface JwtPayload {
  exp: number; // 만료 시간 (Unix timestamp, 초 단위)
  iat: number; // 발행 시간
  sub: string; // 사용자 ID
  [key: string]: unknown;
}

/**
 * JWT 토큰 디코딩 (서명 검증 없음)
 * @param token - JWT 토큰
 * @returns JWT payload 또는 undefined (디코딩 실패 시)
 */
export function decodeJwt(token: string): JwtPayload | undefined {
  try {
    const parts = token.split(".");
    if (parts.length !== 3) {
      return undefined;
    }

    // payload는 두 번째 부분
    const payload = parts[1];
    if (!payload) return undefined;

    // Base64Url 디코딩: JWT는 '-', '_' 사용 → 표준 base64로 변환
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const decoded = atob(base64);

    return JSON.parse(decoded) as JwtPayload;
  } catch {
    return undefined;
  }
}

/**
 * JWT 토큰 만료 여부 체크
 * @param token - JWT 토큰
 * @param bufferSeconds - 만료 전 버퍼 시간 (초), 기본 60초
 * @returns true: 만료됨, false: 유효함
 */
export function isTokenExpired(
  token: string | undefined,
  bufferSeconds = 60,
): boolean {
  if (!token) return true;

  const payload = decodeJwt(token);
  if (!payload?.exp) return true;

  const now = Math.floor(Date.now() / 1000); // 현재 시간 (초)
  return payload.exp - now <= bufferSeconds;
}

/**
 * JWT 토큰 만료까지 남은 시간 (초)
 * @param token - JWT 토큰
 * @returns 남은 시간 (초), 만료되었거나 exp가 없으면 0
 */
export function getTokenTTL(token: string | undefined): number {
  if (!token) return 0;

  const payload = decodeJwt(token);
  if (!payload?.exp) return 0;

  const now = Math.floor(Date.now() / 1000);
  return Math.max(0, payload.exp - now);
}
