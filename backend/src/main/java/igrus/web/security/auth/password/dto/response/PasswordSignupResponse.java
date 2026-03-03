package igrus.web.security.auth.password.dto.response;

public record PasswordSignupResponse(
    String message,

    String email,

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
