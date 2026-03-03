package igrus.web.security.auth.password.dto.response;

public record VerificationResendResponse(
    String message,

    String email
) {
    public static VerificationResendResponse success(String email) {
        return new VerificationResendResponse(
            "인증 코드가 재발송되었습니다.",
            email
        );
    }

    public static VerificationResendResponse sent(String email) {
        return new VerificationResendResponse(
            "인증 코드가 발송되었습니다.",
            email
        );
    }
}
