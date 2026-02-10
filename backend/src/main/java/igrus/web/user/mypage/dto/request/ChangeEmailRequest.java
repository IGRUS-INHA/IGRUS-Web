package igrus.web.user.mypage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 이메일 변경 요청 DTO.
 * 현재 비밀번호 확인 후 새 이메일로 변경합니다.
 */
@Schema(description = "이메일 변경 요청")
public record ChangeEmailRequest(
        @Schema(description = "현재 비밀번호")
        @NotBlank(message = "비밀번호는 필수입니다")
        String password,

        @Schema(description = "새 이메일", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "유효한 이메일 형식이 아닙니다")
        String newEmail
) {
}
