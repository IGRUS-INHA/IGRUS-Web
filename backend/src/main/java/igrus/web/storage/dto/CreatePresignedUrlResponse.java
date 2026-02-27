package igrus.web.storage.dto;

/**
 * 업로드용 Presigned URL 생성 응답 DTO.
 *
 * @param presignedUrl 업로드용 Presigned URL
 * @param objectKey    S3 Object Key
 */
public record CreatePresignedUrlResponse(
        String presignedUrl,
        String objectKey
) {
}
