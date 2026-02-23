package igrus.web.event.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 등록 수동 재오픈 요청 DTO.
 *
 * @param reason 재오픈 사유 (필수)
 */
public record ReopenRegistrationRequest(
        @NotBlank(message = "재오픈 사유는 필수입니다")
        String reason
) {
}
