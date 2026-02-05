package igrus.web.user.mypage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 프로필 수정 요청 DTO.
 * 이메일, 전화번호 수정용.
 */
@Schema(description = "프로필 수정 요청")
public record UpdateProfileRequest(
        @Schema(description = "이메일", example = "newemail@example.com")
        @Email(message = "유효한 이메일 형식이 아닙니다")
        String email,

        @Schema(description = "전화번호", example = "010-1234-5678")
        @Size(max = 20, message = "전화번호는 20자 이내여야 합니다")
        @Pattern(regexp = "^$|^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)")
        String phoneNumber
) {
}
