package igrus.web.admin.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForceWithdrawRequest(
        @NotBlank(message = "강제 탈퇴 사유는 필수입니다")
        @Size(max = 500, message = "강제 탈퇴 사유는 500자 이내여야 합니다")
        String reason
) {
}
