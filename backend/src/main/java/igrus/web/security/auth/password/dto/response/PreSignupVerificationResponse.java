package igrus.web.security.auth.password.dto.response;

public record PreSignupVerificationResponse(
    String message,

    String email,

    boolean verified,

    String verificationToken
) {
    public static PreSignupVerificationResponse success(String email, String verificationToken) {
        return new PreSignupVerificationResponse(
            "이메일 인증이 완료되었습니다.",
            email,
            true,
            verificationToken
        );
    }
}
