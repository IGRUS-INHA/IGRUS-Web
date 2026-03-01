package igrus.web.security.auth.password.dto.response;

public record DuplicateCheckResponse(
        boolean available,

        String message
) {
    public static DuplicateCheckResponse studentIdAvailable() {
        return new DuplicateCheckResponse(true, "사용 가능한 학번입니다");
    }

    public static DuplicateCheckResponse emailAvailable() {
        return new DuplicateCheckResponse(true, "사용 가능한 이메일입니다");
    }

    public static DuplicateCheckResponse phoneNumberAvailable() {
        return new DuplicateCheckResponse(true, "사용 가능한 전화번호입니다");
    }
}
