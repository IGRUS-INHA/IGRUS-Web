package igrus.web.user.withdrawal.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원 탈퇴 요청")
public record WithdrawRequest(
        @Schema(description = "현재 비밀번호", example = "Password1!")
        @NotBlank(message = "비밀번호는 필수입니다")
        String password,

        @Schema(description = "탈퇴 사유", example = "더 이상 사용하지 않습니다")
        @NotBlank(message = "탈퇴 사유는 필수입니다")
        @Size(max = 500, message = "탈퇴 사유는 500자 이내여야 합니다")
        String reason
) {
}
