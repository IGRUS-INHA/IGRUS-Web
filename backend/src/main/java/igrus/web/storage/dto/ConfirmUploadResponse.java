package igrus.web.storage.dto;

/**
 * 업로드 완료 확인 응답 DTO.
 *
 * @param status    파일 업로드 상태 (COMPLETED 또는 FAILED)
 * @param objectKey S3 Object Key
 * @param reason    실패 사유 (FAILED인 경우에만 포함, 성공 시 null)
 */
public record ConfirmUploadResponse(
        String status,
        String objectKey,
        String reason
) {
    /**
     * 성공 응답을 생성한다.
     */
    public static ConfirmUploadResponse success(String objectKey) {
        return new ConfirmUploadResponse("COMPLETED", objectKey, null);
    }

    /**
     * 실패 응답을 생성한다.
     */
    public static ConfirmUploadResponse failure(String objectKey, String reason) {
        return new ConfirmUploadResponse("FAILED", objectKey, reason);
    }
}
