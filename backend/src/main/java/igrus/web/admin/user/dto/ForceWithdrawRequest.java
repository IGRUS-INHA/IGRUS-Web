package igrus.web.admin.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "강제 탈퇴 요청")
public record ForceWithdrawRequest(
        @Schema(description = "강제 탈퇴 사유", example = "동아리 규정 위반")
        @NotBlank(message = "강제 탈퇴 사유는 필수입니다")
        @Size(max = 500, message = "강제 탈퇴 사유는 500자 이내여야 합니다")
        String reason
) {
}
