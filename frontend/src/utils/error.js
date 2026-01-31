/**
 * API 에러 및 알림 메시지 유틸
 */

// 경고 메시지 (warning 토스트용)
export const WARNING_MESSAGES = {
  // 행사
  WAITLIST_REGISTRATION: '정원이 마감되어 대기 신청됩니다.',
  CANCEL_DEADLINE_SOON: (hours) => `취소 가능 시간이 ${hours}시간 미만 남았습니다.`,
  EVENT_ALMOST_FULL: (remaining) => `남은 자리가 ${remaining}명입니다.`,

  // 글 작성
  UNSAVED_CHANGES: '저장하지 않은 내용이 있습니다.',
  LARGE_FILE: '파일 크기가 큽니다. 최대 10MB까지 업로드 가능합니다.',

  // 일반
  SESSION_EXPIRING: '로그인 세션이 곧 만료됩니다.',
};

// 성공 메시지 (success 토스트용)
export const SUCCESS_MESSAGES = {
  // 인증
  LOGIN_SUCCESS: '로그인되었습니다.',
  LOGOUT_SUCCESS: '로그아웃되었습니다.',
  SIGNUP_SUCCESS: '회원가입이 완료되었습니다.',

  // 게시판
  POST_CREATED: '게시글이 등록되었습니다.',
  POST_UPDATED: '게시글이 수정되었습니다.',
  POST_DELETED: '게시글이 삭제되었습니다.',
  COMMENT_CREATED: '댓글이 등록되었습니다.',
  COMMENT_DELETED: '댓글이 삭제되었습니다.',

  // 행사
  EVENT_REGISTERED: '행사 신청이 완료되었습니다.',
  EVENT_WAITLISTED: '대기 신청이 완료되었습니다.',
  EVENT_CANCELLED: '신청이 취소되었습니다.',

  // 일반
  SAVED: '저장되었습니다.',
  COPIED: '클립보드에 복사되었습니다.',
};

// HTTP 상태 코드별 기본 메시지
const HTTP_ERROR_MESSAGES = {
  400: '잘못된 요청입니다.',
  401: '로그인이 필요합니다.',
  403: '접근 권한이 없습니다.',
  404: '요청한 정보를 찾을 수 없습니다.',
  409: '이미 처리된 요청입니다.',
  422: '입력값을 확인해주세요.',
  429: '요청이 너무 많습니다. 잠시 후 다시 시도해주세요.',
  500: '서버 오류가 발생했습니다.',
  502: '서버에 연결할 수 없습니다.',
  503: '서비스 점검 중입니다.',
};

// 서버 에러 코드별 메시지 (백엔드와 맞춰야 함)
const API_ERROR_MESSAGES = {
  // 인증
  INVALID_CREDENTIALS: '학번 또는 비밀번호가 올바르지 않습니다.',
  TOKEN_EXPIRED: '로그인이 만료되었습니다. 다시 로그인해주세요.',
  ACCOUNT_SUSPENDED: '정지된 계정입니다.',
  ACCOUNT_WITHDRAWN: '탈퇴한 계정입니다.',

  // 회원가입
  DUPLICATE_STUDENT_ID: '이미 가입된 학번입니다.',
  DUPLICATE_EMAIL: '이미 사용 중인 이메일입니다.',
  INVALID_VERIFICATION_CODE: '인증 코드가 올바르지 않습니다.',
  VERIFICATION_CODE_EXPIRED: '인증 코드가 만료되었습니다.',

  // 게시판
  POST_NOT_FOUND: '게시글을 찾을 수 없습니다.',
  COMMENT_NOT_FOUND: '댓글을 찾을 수 없습니다.',
  CANNOT_EDIT_POST: '게시글을 수정할 권한이 없습니다.',
  CANNOT_DELETE_POST: '게시글을 삭제할 권한이 없습니다.',

  // 행사
  EVENT_NOT_FOUND: '행사를 찾을 수 없습니다.',
  EVENT_CLOSED: '마감된 행사입니다.',
  EVENT_FULL: '정원이 마감되었습니다.',
  ALREADY_REGISTERED: '이미 신청한 행사입니다.',
  NOT_REGISTERED: '신청 내역이 없습니다.',
  CANCEL_DEADLINE_PASSED: '취소 가능 기간이 지났습니다.',

  // 일반
  PERMISSION_DENIED: '권한이 없습니다.',
  VALIDATION_ERROR: '입력값을 확인해주세요.',
};

/**
 * API 에러에서 사용자 친화적 메시지 추출
 * @param {Error} error - Axios 에러 또는 일반 에러
 * @returns {string} 사용자에게 표시할 메시지
 */
export function getErrorMessage(error) {
  // 네트워크 에러
  if (!error.response) {
    if (error.code === 'ECONNABORTED') {
      return '요청 시간이 초과되었습니다. 다시 시도해주세요.';
    }
    return '네트워크 연결을 확인해주세요.';
  }

  const { status, data } = error.response;

  // 서버에서 보낸 에러 코드가 있는 경우
  if (data?.code && API_ERROR_MESSAGES[data.code]) {
    return API_ERROR_MESSAGES[data.code];
  }

  // 서버에서 보낸 메시지가 있는 경우
  if (data?.message) {
    return data.message;
  }

  // HTTP 상태 코드 기반 기본 메시지
  return HTTP_ERROR_MESSAGES[status] || '오류가 발생했습니다.';
}

/**
 * 에러가 인증 관련인지 확인
 */
export function isAuthError(error) {
  const status = error.response?.status;
  return status === 401 || status === 403;
}

/**
 * 에러가 네트워크 관련인지 확인
 */
export function isNetworkError(error) {
  return !error.response;
}

/**
 * 에러가 유효성 검사 관련인지 확인
 */
export function isValidationError(error) {
  const status = error.response?.status;
  return status === 400 || status === 422;
}

/**
 * 유효성 검사 에러에서 필드별 에러 추출
 * @param {Error} error
 * @returns {Object} { fieldName: errorMessage }
 */
export function getFieldErrors(error) {
  if (!isValidationError(error)) return {};

  const data = error.response?.data;

  // 백엔드 응답 형식에 따라 조정 필요
  if (data?.errors && typeof data.errors === 'object') {
    return data.errors;
  }

  return {};
}
