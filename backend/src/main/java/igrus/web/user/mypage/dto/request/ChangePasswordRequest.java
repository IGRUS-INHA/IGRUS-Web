package igrus.web.user.mypage.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 변경 요청 DTO.
 * 현재 비밀번호 확인 후 새 비밀번호로 변경.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "현재 비밀번호는 필수입니다")
        String currentPassword,

        @NotBlank(message = "새 비밀번호는 필수입니다")
        String newPassword
) {
}
