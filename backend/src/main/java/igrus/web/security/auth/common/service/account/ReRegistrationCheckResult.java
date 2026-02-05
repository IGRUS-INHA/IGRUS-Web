package igrus.web.security.auth.common.service.account;

import java.time.Instant;

/**
 * 재가입 가능 여부 확인 결과
 */
public record ReRegistrationCheckResult(
        boolean isEligible,
        boolean isAlreadyRegistered,
        Instant reRegistrationAvailableAt,
        String message
) {
    public static ReRegistrationCheckResult eligible() {
        return new ReRegistrationCheckResult(true, false, null, null);
    }

    public static ReRegistrationCheckResult alreadyRegistered() {
        return new ReRegistrationCheckResult(false, true, null, "이미 가입된 학번입니다");
    }

    public static ReRegistrationCheckResult restricted(Instant reRegistrationAvailableAt) {
        return new ReRegistrationCheckResult(
                false,
                false,
                reRegistrationAvailableAt,
                "탈퇴 후 5일이 지나야 재가입할 수 있습니다"
        );
    }
}
