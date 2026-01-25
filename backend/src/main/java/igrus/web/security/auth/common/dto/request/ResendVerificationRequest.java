package igrus.web.security.auth.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "인증 코드 재발송 요청")
public record ResendVerificationRequest(
    @Schema(description = "인증 코드를 재발송할 이메일 주소", example = "user@inha.edu", format = "email")
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    String email
) {}
