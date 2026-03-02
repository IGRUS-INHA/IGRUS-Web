package igrus.web.security.auth.common.dto.request;

import jakarta.validation.constraints.*;

public record ResendVerificationRequest(
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    String email
) {}
