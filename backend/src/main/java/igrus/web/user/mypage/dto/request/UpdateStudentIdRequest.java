package igrus.web.user.mypage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 학번 변경 요청 DTO.
 * 임시 학번을 실제 학번으로 변경합니다.
 */
public record UpdateStudentIdRequest(
        @NotBlank(message = "비밀번호는 필수입니다")
        String password,

        @NotBlank(message = "학번은 필수입니다")
        @Pattern(regexp = "^\\d{8}$", message = "학번은 8자리 숫자여야 합니다")
        String newStudentId
) {
}
