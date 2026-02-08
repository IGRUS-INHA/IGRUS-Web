/**
 * 준회원 일괄 거절 요청
 */
export interface BulkRejectionRequest {
  /**
   * 거절할 사용자 ID 목록
   * @minItems 1
   */
  userIds: number[];
  /**
   * 거절 사유
   * @minLength 1
   * @maxLength 255
   */
  reason: string;
}
