package igrus.web.security.auth.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "이메일 인증 요청")
public record EmailVerificationRequest(
    @Schema(description = "인증할 이메일 주소", example = "user@inha.edu", format = "email")
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    String email,

    @Schema(description = "6자리 인증 코드", example = "123456")
    @NotBlank(message = "인증 코드는 필수입니다")
    @Size(min = 6, max = 6, message = "인증 코드는 6자리입니다")
    String code
) {}
