/**
 * 행사 신청 정책
 */

// 행사 상태
export const EVENT_STATUS = {
  UPCOMING: "UPCOMING", // 예정 (신청 가능)
  ONGOING: "ONGOING", // 진행중
  COMPLETED: "COMPLETED", // 완료
  CLOSED: "CLOSED", // 조기 마감
} as const;

export const EVENT_STATUS_LABELS = {
  [EVENT_STATUS.UPCOMING]: "예정",
  [EVENT_STATUS.ONGOING]: "진행중",
  [EVENT_STATUS.COMPLETED]: "완료",
  [EVENT_STATUS.CLOSED]: "마감",
} as const;

// 행사 필터 옵션 (UI용)
export const EVENT_FILTER_STATUS = {
  ALL: "all",
  OPEN: "OPEN",
  ONGOING: "ONGOING",
  COMPLETED: "COMPLETED",
} as const;

export type EventFilterStatus =
  (typeof EVENT_FILTER_STATUS)[keyof typeof EVENT_FILTER_STATUS];

export const EVENT_FILTER_LABELS = {
  [EVENT_FILTER_STATUS.ALL]: "전체",
  [EVENT_FILTER_STATUS.OPEN]: "모집중",
  [EVENT_FILTER_STATUS.ONGOING]: "진행중",
  [EVENT_FILTER_STATUS.COMPLETED]: "종료",
} as const;

// 신청 상태
export const REGISTRATION_STATUS = {
  CONFIRMED: "CONFIRMED", // 신청 확정
  WAITING: "WAITING", // 대기 (정원 초과)
  CANCELLED: "CANCELLED", // 취소됨
} as const;

export const REGISTRATION_STATUS_LABELS = {
  [REGISTRATION_STATUS.CONFIRMED]: "신청 완료",
  [REGISTRATION_STATUS.WAITING]: "대기중",
  [REGISTRATION_STATUS.CANCELLED]: "취소됨",
} as const;

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
} as const;

// 신청 불가 사유
export const REGISTRATION_ERROR = {
  NOT_AUTHENTICATED: "NOT_AUTHENTICATED", // 로그인 필요
  NO_PERMISSION: "NO_PERMISSION", // 권한 없음 (정회원 미만)
  ALREADY_REGISTERED: "ALREADY_REGISTERED", // 이미 신청함
  EVENT_CLOSED: "EVENT_CLOSED", // 마감됨
  EVENT_STARTED: "EVENT_STARTED", // 이미 시작됨
  EVENT_COMPLETED: "EVENT_COMPLETED", // 이미 종료됨
  REGISTRATION_CLOSED: "REGISTRATION_CLOSED", // 신청 기간 종료
} as const;

export const REGISTRATION_ERROR_MESSAGES = {
  [REGISTRATION_ERROR.NOT_AUTHENTICATED]: "로그인이 필요합니다.",
  [REGISTRATION_ERROR.NO_PERMISSION]: "정회원만 행사에 신청할 수 있습니다.",
  [REGISTRATION_ERROR.ALREADY_REGISTERED]: "이미 신청한 행사입니다.",
  [REGISTRATION_ERROR.EVENT_CLOSED]: "마감된 행사입니다.",
  [REGISTRATION_ERROR.EVENT_STARTED]: "이미 시작된 행사입니다.",
  [REGISTRATION_ERROR.EVENT_COMPLETED]: "종료된 행사입니다.",
  [REGISTRATION_ERROR.REGISTRATION_CLOSED]: "신청 기간이 종료되었습니다.",
} as const;

// 취소 불가 사유
export const CANCEL_ERROR = {
  NOT_REGISTERED: "NOT_REGISTERED", // 신청 안 함
  DEADLINE_PASSED: "DEADLINE_PASSED", // 취소 기한 지남
  EVENT_STARTED: "EVENT_STARTED", // 이미 시작됨
  ALREADY_CANCELLED: "ALREADY_CANCELLED", // 이미 취소됨
} as const;

export const CANCEL_ERROR_MESSAGES = {
  [CANCEL_ERROR.NOT_REGISTERED]: "신청 내역이 없습니다.",
  [CANCEL_ERROR.DEADLINE_PASSED]:
    "취소 가능 기간이 지났습니다. (행사 48시간 전까지 취소 가능)",
  [CANCEL_ERROR.EVENT_STARTED]: "이미 시작된 행사는 취소할 수 없습니다.",
  [CANCEL_ERROR.ALREADY_CANCELLED]: "이미 취소된 신청입니다.",
} as const;

// 장소 프리셋
export const EVENT_LOCATIONS = [
  "본관 1호관",
  "60주년기념관",
  "2호관(2동)",
  "2호관(2남)",
  "2호관(2북)",
  "4호관",
  "5호관(5동)",
  "5호관(5서)",
  "5호관(5남)",
  "5호관(5북)",
  "6호관",
  "7호관(학생회관)",
  "9호관",
  "하이테크센터",
  "하와이교포기념관",
  "평생교육관/미래융합대학",
  "제1생활관(웅비재)",
  "제2, 3생활관(비룡재)",
  "김현태 인하드림센터",
  "인하드림센터 2·3관",
  "서호관",
  "나빌레관",
  "정석학술정보관",
  "대운동장",
  "농구장",
  "온라인",
] as const;

// 신청 기간 프리셋
// endTimeOffsetHours: 행사 시작 시간 기준 상대 오프셋 (예: -2 → 행사 2시간 전)
export const REGISTRATION_PERIOD_PRESETS = [
  {
    value: "default",
    label: "기본",
    description: "행사 7일 전 ~ 1일 전",
    startDaysBefore: 7,
    endDaysBefore: 1,
    startTime: "09:00",
    endTime: "23:59",
  },
  {
    value: "short",
    label: "단기",
    description: "행사 3일 전 ~ 당일 2시간 전",
    startDaysBefore: 3,
    endDaysBefore: 0,
    startTime: "09:00",
    endTime: "18:00",
    endTimeOffsetHours: -2,
  },
  {
    value: "custom",
    label: "직접 설정",
    description: "날짜와 시간을 직접 입력",
  },
] as const;

export type RegistrationPeriodPresetValue =
  (typeof REGISTRATION_PERIOD_PRESETS)[number]["value"];
