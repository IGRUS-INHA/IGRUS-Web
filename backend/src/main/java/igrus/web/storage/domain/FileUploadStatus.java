package igrus.web.storage.domain;

/**
 * 파일 업로드 상태.
 * <ul>
 *   <li>REQUESTED - Presigned URL이 발급되어 업로드 대기 중</li>
 *   <li>CONFIRMING - 업로드 완료 알림을 받아 S3 HEAD 검증 중</li>
 *   <li>COMPLETED - S3 HEAD 검증 성공, 업로드 완료</li>
 *   <li>FAILED - 업로드 또는 검증 실패</li>
 *   <li>EXPIRED - Presigned URL 만료로 업로드 불가</li>
 * </ul>
 */
public enum FileUploadStatus {
    REQUESTED,
    CONFIRMING,
    COMPLETED,
    FAILED,
    EXPIRED
}
