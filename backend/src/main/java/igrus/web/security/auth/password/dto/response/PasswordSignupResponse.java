package igrus.web.security.auth.password.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답")
public record PasswordSignupResponse(
    @Schema(description = "처리 결과 메시지", example = "회원가입이 완료되었습니다. 관리자 승인 후 로그인 가능합니다.")
    String message,

    @Schema(description = "가입 요청한 이메일 주소", example = "user@inha.edu")
    String email,

    @Schema(description = "임시 학번 (임시 학번 가입 시에만 포함)", example = "99260001")
    String temporaryStudentId
) {
    public static PasswordSignupResponse signupCompleted(String email) {
        return new PasswordSignupResponse(
            "회원가입이 완료되었습니다. 관리자 승인 후 로그인 가능합니다.",
            email,
            null
        );
    }

    public static PasswordSignupResponse signupCompletedWithTempId(String email, String tempStudentId) {
        return new PasswordSignupResponse(
            "회원가입이 완료되었습니다. 관리자 승인 후 로그인 가능합니다.",
            email,
            tempStudentId
        );
    }
}
