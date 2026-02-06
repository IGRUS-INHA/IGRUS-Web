package igrus.web.user.mypage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

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
        @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "유효한 전화번호 형식이 아닙니다")
        String phoneNumber
) {
}
