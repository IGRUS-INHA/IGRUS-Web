/**
 * API 에러 및 알림 메시지 유틸
 */

import { ApiError } from "@/types/error";

// ==================== 메시지 상수 ====================

// 경고 메시지 (warning 토스트용)
export const WARNING_MESSAGES = {
  // 행사
  WAITLIST_REGISTRATION: "정원이 마감되어 대기 신청됩니다.",
  CANCEL_DEADLINE_SOON: (hours: number): string =>
    `취소 가능 시간이 ${hours}시간 미만 남았습니다.`,
  EVENT_ALMOST_FULL: (remaining: number): string =>
    `남은 자리가 ${remaining}명입니다.`,

  // 글 작성
  UNSAVED_CHANGES: "저장하지 않은 내용이 있습니다.",
  LARGE_FILE: "파일 크기가 큽니다. 최대 10MB까지 업로드 가능합니다.",

  // 일반
  SESSION_EXPIRING: "로그인 세션이 곧 만료됩니다.",
} as const;

// 성공 메시지 (success 토스트용)
export const SUCCESS_MESSAGES = {
  // 인증
  LOGIN_SUCCESS: "로그인되었습니다.",
  LOGOUT_SUCCESS: "로그아웃되었습니다.",
  SIGNUP_SUCCESS: "회원가입이 완료되었습니다.",

  // 게시판
  POST_CREATED: "게시글이 등록되었습니다.",
  POST_UPDATED: "게시글이 수정되었습니다.",
  POST_DELETED: "게시글이 삭제되었습니다.",
  COMMENT_CREATED: "댓글이 등록되었습니다.",
  COMMENT_DELETED: "댓글이 삭제되었습니다.",

  // 행사
  EVENT_REGISTERED: "행사 신청이 완료되었습니다.",
  EVENT_WAITLISTED: "대기 신청이 완료되었습니다.",
  EVENT_CANCELLED: "신청이 취소되었습니다.",

  // 일반
  SAVED: "저장되었습니다.",
  COPIED: "클립보드에 복사되었습니다.",
} as const;

// HTTP 상태 코드별 기본 메시지
const HTTP_ERROR_MESSAGES: Record<number, string> = {
  400: "잘못된 요청입니다.",
  401: "로그인이 필요합니다.",
  403: "접근 권한이 없습니다.",
  404: "요청한 정보를 찾을 수 없습니다.",
  409: "이미 처리된 요청입니다.",
  410: "삭제된 리소스입니다.",
  423: "계정이 잠겼습니다.",
  429: "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
  500: "서버 오류가 발생했습니다.",
  502: "서버에 연결할 수 없습니다.",
  503: "서비스 점검 중입니다.",
};

// 백엔드 에러 코드별 메시지 (148개 전체 매핑)
const API_ERROR_MESSAGES: Record<string, string> = {
  // Common (5개)
  METHOD_NOT_ALLOWED: "허용되지 않은 요청 방식입니다.",
  INTERNAL_SERVER_ERROR: "서버 내부 오류가 발생했습니다.",
  INVALID_TYPE_VALUE: "유효하지 않은 타입입니다.",
  ACCESS_DENIED: "접근 권한이 없습니다.",

  // User (7개)
  USER_NOT_FOUND: "사용자를 찾을 수 없습니다.",
  DUPLICATE_EMAIL: "이미 사용 중인 이메일입니다.",
  INVALID_PASSWORD: "비밀번호가 올바르지 않습니다.",
  SAME_ROLE_CHANGE: "이미 동일한 역할입니다.",
  INVALID_STUDENT_ID: "학번 형식이 올바르지 않습니다.",
  INVALID_EMAIL_FORMAT: "이메일 형식이 올바르지 않습니다.",
  INVALID_GRADE: "학년이 올바르지 않습니다.",

  // Inquiry (11개)
  INQUIRY_NOT_FOUND: "문의를 찾을 수 없습니다.",
  INQUIRY_ACCESS_DENIED: "문의 접근 권한이 없습니다.",
  INQUIRY_ALREADY_REPLIED: "이미 답변된 문의입니다.",
  INQUIRY_INVALID_PASSWORD: "비밀번호가 올바르지 않습니다.",
  INQUIRY_MAX_ATTACHMENTS_EXCEEDED: "첨부파일 개수 제한을 초과했습니다.",
  INQUIRY_NUMBER_GENERATION_FAILED: "문의 번호 생성에 실패했습니다.",
  GUEST_INQUIRY_EMAIL_REQUIRED: "이메일을 입력해주세요.",
  GUEST_INQUIRY_NAME_REQUIRED: "이름을 입력해주세요.",
  GUEST_INQUIRY_PASSWORD_REQUIRED: "비밀번호를 입력해주세요.",
  INQUIRY_REPLY_NOT_FOUND: "답변을 찾을 수 없습니다.",
  INVALID_STATUS_TRANSITION: "잘못된 상태 변경입니다.",

  // User Suspension (5개)
  SUSPENSION_INVALID_PERIOD: "정지 기간이 올바르지 않습니다.",
  SUSPENSION_ALREADY_LIFTED: "이미 정지 해제된 계정입니다.",
  SUSPENSION_REASON_REQUIRED: "정지 사유를 입력해주세요.",
  SUSPENSION_CANNOT_EXTEND: "정지를 연장할 수 없습니다.",
  SUSPENSION_EXTEND_INVALID_DATE: "연장 일자가 올바르지 않습니다.",

  // JWT (4개)
  JWT_SECRET_KEY_TOO_SHORT: "JWT 비밀키가 너무 짧습니다.",
  ACCESS_TOKEN_INVALID: "액세스 토큰이 유효하지 않습니다.",
  ACCESS_TOKEN_EXPIRED: "액세스 토큰이 만료되었습니다.",
  INVALID_TOKEN_TYPE: "토큰 타입이 올바르지 않습니다.",

  // Auth (25개)
  INVALID_CREDENTIALS: "학번 또는 비밀번호가 올바르지 않습니다.",
  EMAIL_NOT_VERIFIED: "이메일 인증이 필요합니다.",
  EMAIL_ALREADY_VERIFIED: "이미 인증된 이메일입니다.",
  VERIFICATION_CODE_EXPIRED: "인증 코드가 만료되었습니다.",
  VERIFICATION_CODE_INVALID: "인증 코드가 올바르지 않습니다.",
  VERIFICATION_ATTEMPTS_EXCEEDED: "인증 시도 횟수를 초과했습니다.",
  DUPLICATE_STUDENT_ID: "이미 가입된 학번입니다.",
  DUPLICATE_PHONE_NUMBER: "이미 사용 중인 전화번호입니다.",
  INVALID_PASSWORD_FORMAT: "비밀번호 형식이 올바르지 않습니다.",
  SAME_PASSWORD: "현재 비밀번호와 동일합니다.",
  PRIVACY_CONSENT_REQUIRED: "개인정보 수집 동의가 필요합니다.",
  ACCOUNT_SUSPENDED: "정지된 계정입니다.",
  ACCOUNT_WITHDRAWN: "탈퇴한 계정입니다.",
  ACCOUNT_RECOVERABLE: "복구 가능한 계정입니다.",
  ACCOUNT_NOT_RECOVERABLE: "복구할 수 없는 계정입니다.",
  REFRESH_TOKEN_INVALID: "리프레시 토큰이 유효하지 않습니다.",
  REFRESH_TOKEN_EXPIRED: "세션이 만료되었습니다. 다시 로그인해주세요.",
  PASSWORD_RESET_TOKEN_INVALID: "비밀번호 재설정 토큰이 유효하지 않습니다.",
  PASSWORD_RESET_TOKEN_EXPIRED: "비밀번호 재설정 링크가 만료되었습니다.",
  EMAIL_SEND_FAILED: "이메일 전송에 실패했습니다.",
  RECENT_WITHDRAWAL_EXISTS: "최근 탈퇴 내역이 있습니다.",
  VERIFICATION_RESEND_RATE_LIMITED:
    "인증 메일 재전송은 잠시 후 다시 시도해주세요.",
  ACCOUNT_LOCKED:
    "로그인 실패 횟수 초과(5회)로 인해\n계정이 10분간 잠금 처리되었습니다.",
  TOKEN_EXPIRED: "로그인이 만료되었습니다.",

  // Member Approval (4개)
  ADMIN_REQUIRED: "관리자 권한이 필요합니다.",
  USER_NOT_ASSOCIATE: "준회원이 아닙니다.",
  LAST_ADMIN_CANNOT_CHANGE: "마지막 관리자는 권한을 변경할 수 없습니다.",
  BULK_APPROVAL_EMPTY: "승인할 회원을 선택해주세요.",

  // Board (4개)
  BOARD_NOT_FOUND: "게시판을 찾을 수 없습니다.",
  BOARD_ACCESS_DENIED: "게시판 접근 권한이 없습니다.",
  BOARD_READ_DENIED: "게시판 읽기 권한이 없습니다.",
  BOARD_WRITE_DENIED: "게시판 쓰기 권한이 없습니다.",

  // Post (11개)
  POST_NOT_FOUND: "게시글을 찾을 수 없습니다.",
  POST_ACCESS_DENIED: "게시글 접근 권한이 없습니다.",
  POST_TITLE_TOO_LONG: "제목이 너무 깁니다.",
  POST_IMAGE_LIMIT_EXCEEDED: "이미지 개수 제한을 초과했습니다.",
  POST_RATE_LIMIT_EXCEEDED:
    "게시글 작성 제한을 초과했습니다. 잠시 후 다시 시도해주세요.",
  POST_INVALID_ANONYMOUS_OPTION: "익명 설정이 올바르지 않습니다.",
  POST_INVALID_QUESTION_OPTION: "질문 설정이 올바르지 않습니다.",
  POST_INVALID_VISIBILITY_OPTION: "공개 설정이 올바르지 않습니다.",
  POST_DELETED: "삭제된 게시글입니다.",
  POST_ANONYMOUS_UNCHANGEABLE: "익명 설정은 변경할 수 없습니다.",
  CANNOT_EDIT_POST: "게시글을 수정할 권한이 없습니다.",
  CANNOT_DELETE_POST: "게시글을 삭제할 권한이 없습니다.",

  // Comment (8개)
  COMMENT_NOT_FOUND: "댓글을 찾을 수 없습니다.",
  COMMENT_ACCESS_DENIED: "댓글 접근 권한이 없습니다.",
  COMMENT_CONTENT_TOO_LONG: "댓글이 너무 깁니다.",
  COMMENT_CONTENT_EMPTY: "댓글 내용을 입력해주세요.",
  REPLY_TO_REPLY_NOT_ALLOWED: "대댓글에는 답글을 달 수 없습니다.",
  POST_DELETED_CANNOT_COMMENT: "삭제된 게시글에는 댓글을 달 수 없습니다.",
  ANONYMOUS_NOT_ALLOWED: "공지 게시판에서 익명 댓글은 허용되지 않습니다.",

  // Comment Like (3개)
  CANNOT_LIKE_OWN_COMMENT: "자신의 댓글에는 좋아요를 누를 수 없습니다.",
  ALREADY_LIKED_COMMENT: "이미 좋아요를 누른 댓글입니다.",
  LIKE_NOT_FOUND: "좋아요를 찾을 수 없습니다.",

  // Comment Report (3개)
  ALREADY_REPORTED_COMMENT: "이미 신고한 댓글입니다.",
  INVALID_REPORT_REASON: "신고 사유가 올바르지 않습니다.",
  COMMENT_REPORT_NOT_FOUND: "신고를 찾을 수 없습니다.",

  // Post Like (2개)
  POST_LIKE_ALREADY_EXISTS: "이미 좋아요를 누른 게시글입니다.",
  POST_LIKE_NOT_FOUND: "좋아요를 찾을 수 없습니다.",

  // Bookmark (2개)
  BOOKMARK_ALREADY_EXISTS: "이미 북마크한 게시글입니다.",
  BOOKMARK_NOT_FOUND: "북마크를 찾을 수 없습니다.",

  // Semester Member (4개)
  SEMESTER_MEMBER_NOT_FOUND: "학기 회원 정보를 찾을 수 없습니다.",
  SEMESTER_MEMBER_ALREADY_EXISTS: "이미 등록된 학기 회원입니다.",
  SEMESTER_INVALID_SEMESTER: "학기가 올바르지 않습니다.",
  SEMESTER_INVALID_YEAR: "년도가 올바르지 않습니다.",

  // Event (15개)
  EVENT_NOT_FOUND: "행사를 찾을 수 없습니다.",
  EVENT_ACCESS_DENIED: "행사 접근 권한이 없습니다.",
  EVENT_INVALID_DATE: "행사 날짜가 올바르지 않습니다.",
  EVENT_INVALID_CAPACITY: "행사 정원이 올바르지 않습니다.",
  EVENT_ALREADY_REGISTERED: "이미 신청한 행사입니다.",
  EVENT_REGISTRATION_CLOSED: "신청 기간이 종료되었습니다.",
  EVENT_REGISTRATION_NOT_FOUND: "행사 신청 내역을 찾을 수 없습니다.",
  EVENT_CAPACITY_FULL: "행사 정원이 마감되었습니다.",
  EVENT_ASSOCIATE_NOT_ALLOWED: "준회원은 신청할 수 없습니다.",
  EVENT_ALREADY_CANCELED: "이미 취소된 신청입니다.",
  EVENT_NOT_MANUAL_APPROVE: "수동 승인 행사가 아닙니다.",
  EVENT_INVALID_REGISTRATION_STATUS: "신청 상태가 올바르지 않습니다.",
  EVENT_OPERATOR_REQUIRED: "행사 운영자 권한이 필요합니다.",
  EVENT_NOT_OPEN: "공개되지 않은 행사입니다.",
  EVENT_NOT_IN_REGISTRATION_PERIOD: "신청 기간이 아닙니다.",
  EVENT_CLOSED: "마감된 행사입니다.",
  EVENT_FULL: "정원이 마감되었습니다.",
  ALREADY_REGISTERED: "이미 신청한 행사입니다.",
  NOT_REGISTERED: "신청 내역이 없습니다.",
  CANCEL_DEADLINE_PASSED: "취소 가능 기간이 지났습니다.",
  EVENT_SURVEY_RESPONSE_REQUIRED:
    "설문 응답이 필요합니다. 설문을 먼저 작성해주세요.",
  EVENT_SURVEY_NOT_READY: "설문이 아직 시작되지 않았습니다.",

  // 일반
  PERMISSION_DENIED: "권한이 없습니다.",
  VALIDATION_ERROR: "입력값을 확인해주세요.",
};

// ==================== 기본 유틸 함수 ====================

/**
 * ApiError 인스턴스인지 확인하는 타입 가드
 */
export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

/**
 * 특정 에러 코드를 가진 에러인지 확인
 */
export function hasErrorCode(error: unknown, code: string): boolean {
  return isApiError(error) && error.code === code;
}

/**
 * 에러 코드 추출
 */
export function getErrorCode(error: unknown): string | undefined {
  return isApiError(error) ? error.code : undefined;
}

/**
 * 사용자 친화적 에러 메시지 추출
 */
export function getErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    // API_ERROR_MESSAGES에 매핑된 메시지가 있으면 우선 사용
    const mappedMessage = API_ERROR_MESSAGES[error.code];
    if (mappedMessage) {
      return mappedMessage;
    }
    // 백엔드에서 보낸 메시지가 있으면 사용 (필드명 접두사 제거)
    if (error.message) {
      return error.message.replace(/^\w+:\s*/, "");
    }
    // HTTP 상태 코드 기반 기본 메시지
    const httpMessage = HTTP_ERROR_MESSAGES[error.status];
    return httpMessage ?? "오류가 발생했습니다.";
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "알 수 없는 오류가 발생했습니다.";
}

// ==================== HTTP 상태 기반 헬퍼 ====================

/**
 * 401 Unauthorized 에러인지 확인
 */
export function isUnauthorizedError(error: unknown): boolean {
  return isApiError(error) && error.status === 401;
}

/**
 * 403 Forbidden 에러인지 확인 (권한 없음)
 */
export function isForbiddenError(error: unknown): boolean {
  return isApiError(error) && error.status === 403;
}

/**
 * 404 Not Found 에러인지 확인
 */
export function isNotFoundError(error: unknown): boolean {
  return isApiError(error) && error.status === 404;
}

/**
 * 409 Conflict 에러인지 확인 (중복/충돌)
 */
export function isConflictError(error: unknown): boolean {
  return isApiError(error) && error.status === 409;
}

/**
 * 410 Gone 에러인지 확인 (영구 삭제)
 */
export function isGoneError(error: unknown): boolean {
  return isApiError(error) && error.status === 410;
}

/**
 * 429 Too Many Requests 에러인지 확인 (요청 초과)
 */
export function isRateLimitError(error: unknown): boolean {
  return isApiError(error) && error.status === 429;
}

/**
 * 5xx 서버 에러인지 확인
 */
export function isServerError(error: unknown): boolean {
  return isApiError(error) && error.status >= 500 && error.status < 600;
}

// ==================== 게시판(Board) 관련 헬퍼 ====================

/**
 * BOARD_NOT_FOUND 에러인지 확인
 */
export function isBoardNotFound(error: unknown): boolean {
  return hasErrorCode(error, "BOARD_NOT_FOUND");
}

/**
 * BOARD_ACCESS_DENIED 에러인지 확인
 */
export function isBoardAccessDenied(error: unknown): boolean {
  return hasErrorCode(error, "BOARD_ACCESS_DENIED");
}

/**
 * BOARD_READ_DENIED 에러인지 확인 (게시판 읽기 권한 없음)
 */
export function isBoardReadDenied(error: unknown): boolean {
  return hasErrorCode(error, "BOARD_READ_DENIED");
}

/**
 * BOARD_WRITE_DENIED 에러인지 확인 (게시판 쓰기 권한 없음)
 */
export function isBoardWriteDenied(error: unknown): boolean {
  return hasErrorCode(error, "BOARD_WRITE_DENIED");
}

// ==================== 게시글(Post) 관련 헬퍼 ====================

/**
 * POST_NOT_FOUND 에러인지 확인
 */
export function isPostNotFound(error: unknown): boolean {
  return hasErrorCode(error, "POST_NOT_FOUND");
}

/**
 * POST_ACCESS_DENIED 에러인지 확인
 */
export function isPostAccessDenied(error: unknown): boolean {
  return hasErrorCode(error, "POST_ACCESS_DENIED");
}

/**
 * POST_DELETED 에러인지 확인 (삭제된 게시글)
 */
export function isPostDeleted(error: unknown): boolean {
  return hasErrorCode(error, "POST_DELETED");
}

/**
 * POST_TITLE_TOO_LONG 에러인지 확인
 */
export function isPostTitleTooLong(error: unknown): boolean {
  return hasErrorCode(error, "POST_TITLE_TOO_LONG");
}

/**
 * POST_IMAGE_LIMIT_EXCEEDED 에러인지 확인
 */
export function isPostImageLimitExceeded(error: unknown): boolean {
  return hasErrorCode(error, "POST_IMAGE_LIMIT_EXCEEDED");
}

/**
 * POST_RATE_LIMIT_EXCEEDED 에러인지 확인
 */
export function isPostRateLimitExceeded(error: unknown): boolean {
  return hasErrorCode(error, "POST_RATE_LIMIT_EXCEEDED");
}

/**
 * POST_INVALID_ANONYMOUS_OPTION 에러인지 확인
 */
export function isPostInvalidAnonymousOption(error: unknown): boolean {
  return hasErrorCode(error, "POST_INVALID_ANONYMOUS_OPTION");
}

/**
 * POST_INVALID_QUESTION_OPTION 에러인지 확인
 */
export function isPostInvalidQuestionOption(error: unknown): boolean {
  return hasErrorCode(error, "POST_INVALID_QUESTION_OPTION");
}

/**
 * POST_INVALID_VISIBILITY_OPTION 에러인지 확인
 */
export function isPostInvalidVisibilityOption(error: unknown): boolean {
  return hasErrorCode(error, "POST_INVALID_VISIBILITY_OPTION");
}

/**
 * POST_ANONYMOUS_UNCHANGEABLE 에러인지 확인
 */
export function isPostAnonymousUnchangeable(error: unknown): boolean {
  return hasErrorCode(error, "POST_ANONYMOUS_UNCHANGEABLE");
}

/**
 * POST_LIKE_ALREADY_EXISTS 에러인지 확인
 */
export function isPostLikeAlreadyExists(error: unknown): boolean {
  return hasErrorCode(error, "POST_LIKE_ALREADY_EXISTS");
}

// ==================== 댓글(Comment) 관련 헬퍼 ====================

/**
 * COMMENT_NOT_FOUND 에러인지 확인
 */
export function isCommentNotFound(error: unknown): boolean {
  return hasErrorCode(error, "COMMENT_NOT_FOUND");
}

/**
 * COMMENT_ACCESS_DENIED 에러인지 확인
 */
export function isCommentAccessDenied(error: unknown): boolean {
  return hasErrorCode(error, "COMMENT_ACCESS_DENIED");
}

/**
 * COMMENT_CONTENT_EMPTY 에러인지 확인
 */
export function isCommentContentEmpty(error: unknown): boolean {
  return hasErrorCode(error, "COMMENT_CONTENT_EMPTY");
}

/**
 * COMMENT_CONTENT_TOO_LONG 에러인지 확인
 */
export function isCommentContentTooLong(error: unknown): boolean {
  return hasErrorCode(error, "COMMENT_CONTENT_TOO_LONG");
}

/**
 * REPLY_TO_REPLY_NOT_ALLOWED 에러인지 확인
 */
export function isReplyToReplyNotAllowed(error: unknown): boolean {
  return hasErrorCode(error, "REPLY_TO_REPLY_NOT_ALLOWED");
}

/**
 * POST_DELETED_CANNOT_COMMENT 에러인지 확인
 */
export function isPostDeletedCannotComment(error: unknown): boolean {
  return hasErrorCode(error, "POST_DELETED_CANNOT_COMMENT");
}

/**
 * ANONYMOUS_NOT_ALLOWED 에러인지 확인
 */
export function isCommentAnonymousNotAllowed(error: unknown): boolean {
  return hasErrorCode(error, "ANONYMOUS_NOT_ALLOWED");
}

/**
 * CANNOT_LIKE_OWN_COMMENT 에러인지 확인
 */
export function isCannotLikeOwnComment(error: unknown): boolean {
  return hasErrorCode(error, "CANNOT_LIKE_OWN_COMMENT");
}

// ==================== 인증(Auth) 관련 헬퍼 ====================

/**
 * INVALID_CREDENTIALS 에러인지 확인
 */
export function isInvalidCredentials(error: unknown): boolean {
  return hasErrorCode(error, "INVALID_CREDENTIALS");
}

/**
 * ACCOUNT_SUSPENDED 에러인지 확인
 */
export function isAccountSuspended(error: unknown): boolean {
  return hasErrorCode(error, "ACCOUNT_SUSPENDED");
}

/**
 * ACCOUNT_WITHDRAWN 에러인지 확인
 */
export function isAccountWithdrawn(error: unknown): boolean {
  return hasErrorCode(error, "ACCOUNT_WITHDRAWN");
}

/**
 * ACCOUNT_LOCKED 에러인지 확인
 */
export function isAccountLocked(error: unknown): boolean {
  return hasErrorCode(error, "ACCOUNT_LOCKED");
}

/**
 * EMAIL_NOT_VERIFIED 에러인지 확인
 */
export function isEmailNotVerified(error: unknown): boolean {
  return hasErrorCode(error, "EMAIL_NOT_VERIFIED");
}

/**
 * EMAIL_NOT_VERIFIED 에러에서 이메일 주소를 추출
 */
export function getEmailFromVerificationError(
  error: unknown,
): string | undefined {
  if (isEmailNotVerified(error) && isApiError(error) && error.data) {
    const email = error.data["email"];
    return typeof email === "string" ? email : undefined;
  }
  return undefined;
}

/**
 * TOKEN_EXPIRED 또는 REFRESH_TOKEN_EXPIRED 에러인지 확인
 */
export function isTokenExpiredError(error: unknown): boolean {
  return (
    hasErrorCode(error, "TOKEN_EXPIRED") ||
    hasErrorCode(error, "REFRESH_TOKEN_EXPIRED")
  );
}

// ==================== 행사(Event) 관련 헬퍼 ====================

/**
 * EVENT_NOT_FOUND 에러인지 확인
 */
export function isEventNotFound(error: unknown): boolean {
  return hasErrorCode(error, "EVENT_NOT_FOUND");
}

/**
 * EVENT_ACCESS_DENIED 에러인지 확인
 */
export function isEventAccessDenied(error: unknown): boolean {
  return hasErrorCode(error, "EVENT_ACCESS_DENIED");
}

/**
 * EVENT_ALREADY_REGISTERED 에러인지 확인
 */
export function isEventAlreadyRegistered(error: unknown): boolean {
  return hasErrorCode(error, "EVENT_ALREADY_REGISTERED");
}

/**
 * EVENT_CAPACITY_FULL 에러인지 확인
 */
export function isEventCapacityFull(error: unknown): boolean {
  return hasErrorCode(error, "EVENT_CAPACITY_FULL");
}

/**
 * EVENT_REGISTRATION_CLOSED 에러인지 확인
 */
export function isEventRegistrationClosed(error: unknown): boolean {
  return hasErrorCode(error, "EVENT_REGISTRATION_CLOSED");
}

/**
 * EVENT_OPERATOR_REQUIRED 에러인지 확인
 */
export function isEventOperatorRequired(error: unknown): boolean {
  return hasErrorCode(error, "EVENT_OPERATOR_REQUIRED");
}

// ==================== 관리자(Admin) 관련 헬퍼 ====================

/**
 * ADMIN_REQUIRED 에러인지 확인
 */
export function isAdminRequired(error: unknown): boolean {
  return hasErrorCode(error, "ADMIN_REQUIRED");
}

// ==================== 기타 유틸리티 ====================

/**
 * 디버깅용 에러 정보 추출
 */
export function getErrorInfo(error: unknown): {
  message: string;
  code?: string | undefined;
  status?: number | undefined;
  timestamp?: string | undefined;
} {
  if (isApiError(error)) {
    return {
      message: error.message,
      code: error.code,
      status: error.status,
      timestamp: error.timestamp ?? undefined,
    };
  }

  return {
    message: error instanceof Error ? error.message : String(error),
  };
}
