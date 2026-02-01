/**
 * 행사 신청 정책
 */

// 행사 상태
export const EVENT_STATUS = {
  UPCOMING: 'UPCOMING', // 예정 (신청 가능)
  ONGOING: 'ONGOING', // 진행중
  COMPLETED: 'COMPLETED', // 완료
  CLOSED: 'CLOSED', // 조기 마감
};

export const EVENT_STATUS_LABELS = {
  [EVENT_STATUS.UPCOMING]: '예정',
  [EVENT_STATUS.ONGOING]: '진행중',
  [EVENT_STATUS.COMPLETED]: '완료',
  [EVENT_STATUS.CLOSED]: '마감',
};

// 신청 상태
export const REGISTRATION_STATUS = {
  CONFIRMED: 'CONFIRMED', // 신청 확정
  WAITING: 'WAITING', // 대기 (정원 초과)
  CANCELLED: 'CANCELLED', // 취소됨
};

export const REGISTRATION_STATUS_LABELS = {
  [REGISTRATION_STATUS.CONFIRMED]: '신청 완료',
  [REGISTRATION_STATUS.WAITING]: '대기중',
  [REGISTRATION_STATUS.CANCELLED]: '취소됨',
};

// 신청 정책
export const EVENT_POLICY = {
  // 취소 가능 시간 (밀리초) - 행사 시작 48시간 전까지
  CANCEL_DEADLINE_HOURS: 48,
  CANCEL_DEADLINE_MS: 48 * 60 * 60 * 1000,

  // 대기열 사용 여부
  USE_WAITLIST: true,

  // 자동 승인 여부
  AUTO_APPROVE: true,

  // 같은 행사 중복 신청 불가
  ALLOW_DUPLICATE: false,
};

// 신청 불가 사유
export const REGISTRATION_ERROR = {
  NOT_AUTHENTICATED: 'NOT_AUTHENTICATED', // 로그인 필요
  NO_PERMISSION: 'NO_PERMISSION', // 권한 없음 (정회원 미만)
  ALREADY_REGISTERED: 'ALREADY_REGISTERED', // 이미 신청함
  EVENT_CLOSED: 'EVENT_CLOSED', // 마감됨
  EVENT_STARTED: 'EVENT_STARTED', // 이미 시작됨
  EVENT_COMPLETED: 'EVENT_COMPLETED', // 이미 종료됨
  REGISTRATION_CLOSED: 'REGISTRATION_CLOSED', // 신청 기간 종료
};

export const REGISTRATION_ERROR_MESSAGES = {
  [REGISTRATION_ERROR.NOT_AUTHENTICATED]: '로그인이 필요합니다.',
  [REGISTRATION_ERROR.NO_PERMISSION]: '정회원만 행사에 신청할 수 있습니다.',
  [REGISTRATION_ERROR.ALREADY_REGISTERED]: '이미 신청한 행사입니다.',
  [REGISTRATION_ERROR.EVENT_CLOSED]: '마감된 행사입니다.',
  [REGISTRATION_ERROR.EVENT_STARTED]: '이미 시작된 행사입니다.',
  [REGISTRATION_ERROR.EVENT_COMPLETED]: '종료된 행사입니다.',
  [REGISTRATION_ERROR.REGISTRATION_CLOSED]: '신청 기간이 종료되었습니다.',
};

// 취소 불가 사유
export const CANCEL_ERROR = {
  NOT_REGISTERED: 'NOT_REGISTERED', // 신청 안 함
  DEADLINE_PASSED: 'DEADLINE_PASSED', // 취소 기한 지남
  EVENT_STARTED: 'EVENT_STARTED', // 이미 시작됨
  ALREADY_CANCELLED: 'ALREADY_CANCELLED', // 이미 취소됨
};

export const CANCEL_ERROR_MESSAGES = {
  [CANCEL_ERROR.NOT_REGISTERED]: '신청 내역이 없습니다.',
  [CANCEL_ERROR.DEADLINE_PASSED]: '취소 가능 기간이 지났습니다. (행사 48시간 전까지 취소 가능)',
  [CANCEL_ERROR.EVENT_STARTED]: '이미 시작된 행사는 취소할 수 없습니다.',
  [CANCEL_ERROR.ALREADY_CANCELLED]: '이미 취소된 신청입니다.',
};
