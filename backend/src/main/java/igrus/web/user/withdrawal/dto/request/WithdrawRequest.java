package igrus.web.user.withdrawal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WithdrawRequest(
        @NotBlank(message = "비밀번호는 필수입니다")
        String password,

        @NotBlank(message = "탈퇴 사유는 필수입니다")
        @Size(max = 500, message = "탈퇴 사유는 500자 이내여야 합니다")
        String reason
) {
}
