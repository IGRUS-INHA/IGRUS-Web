import type { RejectedAssociateInfoResponse } from './rejectedAssociateInfoResponse';

/**
 * 거절된 준회원 목록 페이징 응답 (Spring Page 형태)
 */
export interface RejectedAssociatePageResponse {
  /** 거절된 준회원 목록 */
  content?: RejectedAssociateInfoResponse[];
  /** 전체 요소 수 */
  totalElements?: number;
  /** 전체 페이지 수 */
  totalPages?: number;
  /** 현재 페이지 번호 (0부터 시작) */
  number?: number;
  /** 페이지 크기 */
  size?: number;
  /** 첫 페이지 여부 */
  first?: boolean;
  /** 마지막 페이지 여부 */
  last?: boolean;
  /** 비어있는지 여부 */
  empty?: boolean;
}
