/**
 * 준회원 일괄 거절 결과 응답
 */
export interface BulkRejectionResultResponse {
  /** 거절 성공 수 */
  rejectedCount?: number;
  /** 거절 실패 수 */
  failedCount?: number;
  /** 총 요청 수 */
  totalRequested?: number;
}
