package igrus.web.storage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 업로드용 Presigned URL 생성 요청 DTO.
 *
 * @param fileName    원본 파일명 (필수, 최대 255자)
 * @param contentType Content-Type (필수, allowlist 검증은 서비스에서 수행)
 * @param fileSize    파일 크기 (bytes, 필수, 1 ~ 10,485,760)
 * @param purpose     사용처 (posts, profiles, events)
 */
public record CreatePresignedUrlRequest(
        @NotBlank(message = "파일명은 필수입니다")
        @Size(max = 255, message = "파일명은 255자 이내여야 합니다")
        String fileName,

        @NotBlank(message = "Content-Type은 필수입니다")
        String contentType,

        @NotNull(message = "파일 크기는 필수입니다")
        @Min(value = 1, message = "파일 크기는 1 바이트 이상이어야 합니다")
        @Max(value = 10485760, message = "파일 크기는 10MB를 초과할 수 없습니다")
        Long fileSize,

        @NotBlank(message = "사용처는 필수입니다")
        String purpose
) {
}
