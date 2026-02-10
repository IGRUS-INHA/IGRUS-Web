package igrus.web.user.mypage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 전화번호 변경 요청 DTO.
 * 현재 비밀번호 확인 후 새 전화번호로 변경합니다.
 */
@Schema(description = "전화번호 변경 요청")
public record ChangePhoneNumberRequest(
        @Schema(description = "현재 비밀번호")
        @NotBlank(message = "비밀번호는 필수입니다")
        String password,

        @Schema(description = "새 전화번호", example = "010-1234-5678")
        @NotBlank(message = "전화번호는 필수입니다")
        @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{4}$", message = "전화번호는 000-0000-0000 형식이어야 합니다")
        String newPhoneNumber
) {
}
