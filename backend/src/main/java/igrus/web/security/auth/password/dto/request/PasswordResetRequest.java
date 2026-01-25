package igrus.web.security.auth.password.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "비밀번호 재설정 요청")
public record PasswordResetRequest(
    @Schema(description = "학번 (8자리 숫자)", example = "12345678")
    @NotBlank(message = "학번은 필수입니다")
    @Pattern(regexp = "^\\d{8}$", message = "학번은 8자리 숫자여야 합니다")
    String studentId
) {}
