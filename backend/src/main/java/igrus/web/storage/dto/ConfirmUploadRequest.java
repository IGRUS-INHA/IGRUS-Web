package igrus.web.storage.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 업로드 완료 확인 요청 DTO.
 *
 * @param objectKey S3 Object Key (필수)
 */
public record ConfirmUploadRequest(
        @NotBlank(message = "Object Key는 필수입니다")
        String objectKey
) {
}
