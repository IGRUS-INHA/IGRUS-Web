package igrus.web.user.mypage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 변경 요청 DTO.
 * 현재 비밀번호 확인 후 새 비밀번호로 변경.
 */
@Schema(description = "비밀번호 변경 요청")
public record ChangePasswordRequest(
        @Schema(description = "현재 비밀번호", example = "CurrentPass123!")
        @NotBlank(message = "현재 비밀번호는 필수입니다")
        String currentPassword,

        @Schema(description = "새 비밀번호 (8자 이상, 영문 대소문자, 숫자, 특수문자 포함)", example = "NewPass456!")
        @NotBlank(message = "새 비밀번호는 필수입니다")
        String newPassword
) {
}
