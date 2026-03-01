package igrus.web.security.auth.password.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
    @NotBlank(message = "토큰은 필수입니다")
    String token,

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, max = 72, message = "비밀번호는 8~72자여야 합니다")
    String newPassword
) {}
