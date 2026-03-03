package igrus.web.user.mypage.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 이메일 변경 요청 DTO.
 * 현재 비밀번호 확인 후 새 이메일로 변경합니다.
 */
public record ChangeEmailRequest(
        @NotBlank(message = "비밀번호는 필수입니다")
        String password,

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "유효한 이메일 형식이 아닙니다")
        String newEmail
) {
}
