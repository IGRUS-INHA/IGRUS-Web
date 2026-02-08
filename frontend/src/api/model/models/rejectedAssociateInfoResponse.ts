/**
 * 거절된 준회원 정보 응답
 */
export interface RejectedAssociateInfoResponse {
  /** 사용자 고유 ID */
  userId?: number;
  /** 학번 */
  studentId?: string;
  /** 이름 */
  name?: string;
  /** 학과 */
  department?: string;
  /** 가입 동기 */
  motivation?: string;
  /** 가입 신청 일시 */
  createdAt?: string;
  /** 거절 사유 */
  rejectionReason?: string;
  /** 거절 일시 */
  rejectedAt?: string;
  /** 거절 처리자 ID */
  rejectedBy?: number;
}
