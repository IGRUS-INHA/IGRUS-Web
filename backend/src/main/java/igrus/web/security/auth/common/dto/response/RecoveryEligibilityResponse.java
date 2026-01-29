package igrus.web.security.auth.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "계정 복구 가능 여부 응답")
public record RecoveryEligibilityResponse(
    @Schema(description = "복구 가능 여부", example = "true")
    boolean recoverable,

    @Schema(description = "복구 가능 마감 시한 (복구 가능한 경우에만 존재)", example = "2024-02-15T10:30:00Z", nullable = true)
    Instant recoveryDeadline,

    @Schema(description = "안내 메시지", example = "탈퇴한 계정입니다. 복구하시겠습니까?")
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
