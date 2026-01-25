package igrus.web.security.auth.password.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그아웃 요청")
public record PasswordLogoutRequest(
    @Schema(description = "JWT Refresh Token (로그아웃 시 무효화할 토큰)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @NotBlank(message = "리프레시 토큰은 필수입니다")
    String refreshToken
) {}
