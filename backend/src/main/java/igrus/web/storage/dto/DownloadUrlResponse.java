package igrus.web.storage.dto;

/**
 * 다운로드용 Presigned URL 응답 DTO.
 *
 * @param presignedUrl 다운로드용 Presigned URL
 */
public record DownloadUrlResponse(
        String presignedUrl
) {
}
