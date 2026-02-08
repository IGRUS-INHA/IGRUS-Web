/**
 * 준회원 거절 요청
 */
export interface RejectAssociateRequest {
  /**
   * 거절 사유
   * @minLength 1
   * @maxLength 255
   */
  reason: string;
}
