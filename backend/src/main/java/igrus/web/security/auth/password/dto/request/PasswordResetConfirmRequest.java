package igrus.web.security.auth.password.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "비밀번호 재설정 확인 요청")
public record PasswordResetConfirmRequest(
    @Schema(description = "재설정 토큰", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "토큰은 필수입니다")
    String token,

    @Schema(description = "새 비밀번호 (영문 대/소문자, 숫자, 특수문자 포함 8자 이상)", example = "NewPass1!", format = "password")
    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, max = 72, message = "비밀번호는 8~72자여야 합니다")
    String newPassword
) {}
