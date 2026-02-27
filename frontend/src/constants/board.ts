/**
 * 게시판 정책 및 상수
 */

// 게시판 종류
export const BOARDS = {
  NOTICES: 'NOTICES',
  GENERAL: 'GENERAL',
  INSIGHT: 'INSIGHT',
} as const;

// 빌드타임 플래그에 따라 활성화된 게시판 목록
export const ENABLED_BOARDS = __FEATURE_COMMUNITY__
  ? [BOARDS.NOTICES, BOARDS.GENERAL, BOARDS.INSIGHT]
  : [BOARDS.NOTICES];

export const BOARD_LABELS = {
  [BOARDS.NOTICES]: '공지사항',
  [BOARDS.GENERAL]: '자유게시판',
  [BOARDS.INSIGHT]: '정보공유',
} as const;

// 검색 타입
export const SEARCH_TYPE = {
  TITLE: 'title',
  CONTENT: 'content',
  TITLE_CONTENT: 'title_content', // 제목+내용
} as const;

export const SEARCH_TYPE_LABELS = {
  [SEARCH_TYPE.TITLE]: '제목',
  [SEARCH_TYPE.CONTENT]: '내용',
  [SEARCH_TYPE.TITLE_CONTENT]: '제목+내용',
} as const;

// 정렬 옵션
export const SORT_TYPE = {
  LATEST: 'latest', // 최신순
  POPULAR: 'popular', // 인기순 (좋아요)
} as const;

export const SORT_TYPE_LABELS = {
  [SORT_TYPE.LATEST]: '최신순',
  [SORT_TYPE.POPULAR]: '인기순',
} as const;

// 페이지네이션
export const PAGINATION = {
  DEFAULT_PAGE: 1,
  DEFAULT_SIZE: 20,
  PAGE_SIZES: [10, 20, 30, 50],
} as const;

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
} as const;

export const REPORT_REASON_LABELS = {
  [REPORT.REASONS.SPAM]: '스팸/광고',
  [REPORT.REASONS.ABUSE]: '욕설/비방',
  [REPORT.REASONS.INAPPROPRIATE]: '부적절한 내용',
  [REPORT.REASONS.COPYRIGHT]: '저작권 침해',
  [REPORT.REASONS.OTHER]: '기타',
} as const;

// 게시글 상태
export const POST_STATUS = {
  NORMAL: 'NORMAL',
  BLINDED: 'BLINDED', // 신고로 블라인드
  DELETED: 'DELETED', // 삭제됨
} as const;

// 게시글 폼 검증 스키마 (PostWritePage, PostEditPage 공유)
import { z } from 'zod';

// 질문글 허용 게시판
export const ALLOW_QUESTION = [BOARDS.GENERAL] as const;

// 게시판별 카테고리
export const BOARD_CATEGORIES: Record<string, { value: string; label: string }[]> = {
  [BOARDS.NOTICES]: [{ value: 'general', label: '일반' }],
  [BOARDS.GENERAL]: [{ value: 'general', label: '일반' }],
  [BOARDS.INSIGHT]: [{ value: 'general', label: '일반' }],
};

// 게시글 작성 옵션
export const POST_OPTIONS = {
  ALLOW_ANONYMOUS: [BOARDS.GENERAL] as const,
  ALLOW_QUESTION: [BOARDS.GENERAL] as const,
} as const;

export const postFormSchema = z.object({
  title: z.string().min(1, '제목을 입력해주세요').max(100, '제목은 100자 이내로 입력해주세요'),
  content: z.string().min(1, '내용을 입력해주세요'),
  category: z.string().min(1, '카테고리를 선택해주세요'),
  isAnonymous: z.boolean().optional(),
  isQuestion: z.boolean().optional(),
  isVisibleToAssociate: z.boolean().optional(),
  imageUrls: z.array(z.string().url()).max(5).optional(),
});

export type PostFormData = z.infer<typeof postFormSchema>;
