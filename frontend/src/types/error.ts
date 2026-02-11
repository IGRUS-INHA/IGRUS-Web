/**
 * API 에러 클래스
 * 백엔드 ErrorResponse와 1:1 매핑되는 커스텀 에러 클래스
 */
export class ApiError extends Error {
  public readonly status: number;
  public readonly code: string;
  public readonly timestamp?: string;
  public readonly data?: Record<string, unknown>;

  constructor(
    status: number,
    code: string,
    message: string,
    timestamp?: string | undefined,
    data?: Record<string, unknown> | undefined
  ) {
    super(message);
    this.status = status;
    this.code = code;
    this.timestamp = timestamp;
    this.data = data;
    this.name = 'ApiError';

    // V8 엔진에서 스택 트레이스 올바르게 캡처
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, ApiError);
    }
  }
}

/**
 * 백엔드 ErrorResponse DTO 타입
 */
export interface ErrorResponseDto {
  status?: number;
  code?: string;
  message?: string;
  timestamp?: string;
  [key: string]: unknown;
}
