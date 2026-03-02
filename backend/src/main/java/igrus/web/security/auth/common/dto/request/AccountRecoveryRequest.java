package igrus.web.security.auth.common.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AccountRecoveryRequest(
    @NotBlank(message = "학번은 필수입니다")
    @Pattern(regexp = "^\\d{8}$", message = "학번은 8자리 숫자여야 합니다")
    String studentId,

    @NotBlank(message = "비밀번호는 필수입니다")
    String password
) {}
