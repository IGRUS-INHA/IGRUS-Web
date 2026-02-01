/**
 * 게시판 정책 및 상수
 */

// 게시판 종류
export const BOARDS = {
  NOTICES: 'notices',
  GENERAL: 'general',
  INSIGHT: 'insight',
};

export const BOARD_LABELS = {
  [BOARDS.NOTICES]: '공지사항',
  [BOARDS.GENERAL]: '자유게시판',
  [BOARDS.INSIGHT]: '정보공유',
};

// 검색 타입
export const SEARCH_TYPE = {
  TITLE: 'title',
  CONTENT: 'content',
  TITLE_CONTENT: 'title_content', // 제목+내용
};

export const SEARCH_TYPE_LABELS = {
  [SEARCH_TYPE.TITLE]: '제목',
  [SEARCH_TYPE.CONTENT]: '내용',
  [SEARCH_TYPE.TITLE_CONTENT]: '제목+내용',
};

// 정렬 옵션
export const SORT_TYPE = {
  LATEST: 'latest', // 최신순
  POPULAR: 'popular', // 인기순 (좋아요)
};

export const SORT_TYPE_LABELS = {
  [SORT_TYPE.LATEST]: '최신순',
  [SORT_TYPE.POPULAR]: '인기순',
};

// 페이지네이션
export const PAGINATION = {
  DEFAULT_PAGE: 1,
  DEFAULT_SIZE: 20,
  PAGE_SIZES: [10, 20, 30, 50],
};

// 신고 관련
export const REPORT = {
  // 신고 사유
  REASONS: {
    SPAM: 'SPAM',
    ABUSE: 'ABUSE',
    INAPPROPRIATE: 'INAPPROPRIATE',
    COPYRIGHT: 'COPYRIGHT',
    OTHER: 'OTHER',
  },

  // 블라인드 처리 기준 (신고 N회 이상)
  BLIND_THRESHOLD: 5,
};

export const REPORT_REASON_LABELS = {
  [REPORT.REASONS.SPAM]: '스팸/광고',
  [REPORT.REASONS.ABUSE]: '욕설/비방',
  [REPORT.REASONS.INAPPROPRIATE]: '부적절한 내용',
  [REPORT.REASONS.COPYRIGHT]: '저작권 침해',
  [REPORT.REASONS.OTHER]: '기타',
};

// 게시글 상태
export const POST_STATUS = {
  NORMAL: 'NORMAL',
  BLINDED: 'BLINDED', // 신고로 블라인드
  DELETED: 'DELETED', // 삭제됨
};

// 게시판별 카테고리
export const BOARD_CATEGORIES = {
  [BOARDS.NOTICES]: [
    { value: 'general', label: '일반' },
    { value: 'event', label: '행사' },
    { value: 'important', label: '중요' },
  ],
  [BOARDS.GENERAL]: [
    { value: 'free', label: '자유' },
    { value: 'question', label: '질문' },
    { value: 'info', label: '정보' },
  ],
  [BOARDS.INSIGHT]: [
    { value: 'tech', label: '기술' },
    { value: 'career', label: '취업/진로' },
    { value: 'study', label: '스터디' },
    { value: 'review', label: '후기' },
  ],
};

// 글 작성 옵션
export const POST_OPTIONS = {
  // 익명 글 허용 게시판
  ALLOW_ANONYMOUS: [BOARDS.GENERAL],

  // 질문글 허용 게시판
  ALLOW_QUESTION: [BOARDS.GENERAL, BOARDS.INSIGHT],
};
