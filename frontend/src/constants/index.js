// 사용자 역할
export const ROLES = {
  ASSOCIATE: 'ASSOCIATE', // 준회원
  MEMBER: 'MEMBER', // 정회원
  OPERATOR: 'OPERATOR', // 운영진
  ADMIN: 'ADMIN', // 관리자
};

// 역할 한글명
export const ROLE_LABELS = {
  [ROLES.ASSOCIATE]: '준회원',
  [ROLES.MEMBER]: '정회원',
  [ROLES.OPERATOR]: '운영진',
  [ROLES.ADMIN]: '관리자',
};

// 계정 상태
export const USER_STATUS = {
  ACTIVE: 'ACTIVE',
  SUSPENDED: 'SUSPENDED',
  WITHDRAWN: 'WITHDRAWN',
};

// 게시판 종류
export const BOARDS = {
  NOTICES: 'notices', // 공지사항
  GENERAL: 'general', // 자유게시판
  INSIGHT: 'insight', // 정보공유
};

export const BOARD_LABELS = {
  [BOARDS.NOTICES]: '공지사항',
  [BOARDS.GENERAL]: '자유게시판',
  [BOARDS.INSIGHT]: '정보공유',
};

// 행사 상태
export const EVENT_STATUS = {
  UPCOMING: 'UPCOMING',
  ONGOING: 'ONGOING',
  COMPLETED: 'COMPLETED',
  CLOSED: 'CLOSED',
};

export const EVENT_STATUS_LABELS = {
  [EVENT_STATUS.UPCOMING]: '예정',
  [EVENT_STATUS.ONGOING]: '진행중',
  [EVENT_STATUS.COMPLETED]: '완료',
  [EVENT_STATUS.CLOSED]: '마감',
};

// 문의 유형
export const INQUIRY_TYPES = {
  JOIN: 'JOIN',
  EVENT: 'EVENT',
  REPORT: 'REPORT',
  ACCOUNT: 'ACCOUNT',
  OTHER: 'OTHER',
};

export const INQUIRY_TYPE_LABELS = {
  [INQUIRY_TYPES.JOIN]: '가입문의',
  [INQUIRY_TYPES.EVENT]: '행사문의',
  [INQUIRY_TYPES.REPORT]: '신고',
  [INQUIRY_TYPES.ACCOUNT]: '계정문의',
  [INQUIRY_TYPES.OTHER]: '기타',
};

// 문의 상태
export const INQUIRY_STATUS = {
  PENDING: 'PENDING',
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED',
};

export const INQUIRY_STATUS_LABELS = {
  [INQUIRY_STATUS.PENDING]: '접수',
  [INQUIRY_STATUS.IN_PROGRESS]: '처리중',
  [INQUIRY_STATUS.COMPLETED]: '완료',
};

// 페이지네이션
export const DEFAULT_PAGE_SIZE = 20;

// 파일 업로드 제한
export const FILE_LIMITS = {
  IMAGE_MAX_SIZE: 10 * 1024 * 1024, // 10MB
  IMAGE_MAX_COUNT: 5,
  ATTACHMENT_MAX_COUNT: 3,
};
