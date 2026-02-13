package igrus.web.user.mypage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 학번 변경 요청 DTO.
 * 임시 학번을 실제 학번으로 변경합니다.
 */
@Schema(description = "학번 변경 요청")
public record UpdateStudentIdRequest(
        @Schema(description = "현재 비밀번호")
        @NotBlank(message = "비밀번호는 필수입니다")
        String password,

        @Schema(description = "새 학번 (8자리 숫자)", example = "12345678")
        @NotBlank(message = "학번은 필수입니다")
        @Pattern(regexp = "^\\d{8}$", message = "학번은 8자리 숫자여야 합니다")
        String newStudentId
) {
}
