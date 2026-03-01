package igrus.web.security.auth.common.dto.response;

import java.time.Instant;

public record RecoveryEligibilityResponse(
    boolean recoverable,

    Instant recoveryDeadline,

    String message
) {
    public static RecoveryEligibilityResponse recoverable(Instant deadline) {
        return new RecoveryEligibilityResponse(
            true,
            deadline,
            "탈퇴한 계정입니다. 복구하시겠습니까?"
        );
    }

    public static RecoveryEligibilityResponse notRecoverable() {
        return new RecoveryEligibilityResponse(
            false,
            null,
            "복구 기간이 만료된 계정입니다"
        );
    }

    public static RecoveryEligibilityResponse notWithdrawn() {
        return new RecoveryEligibilityResponse(
            false,
            null,
            "탈퇴 상태가 아닌 계정입니다"
        );
    }
}
