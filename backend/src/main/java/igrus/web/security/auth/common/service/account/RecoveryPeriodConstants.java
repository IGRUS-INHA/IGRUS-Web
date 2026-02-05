package igrus.web.security.auth.common.service.account;

import igrus.web.user.domain.User;

import java.time.Duration;
import java.time.Instant;

/**
 * 계정 복구 기간 관련 상수 및 유틸리티.
 * <p>
 * 탈퇴 후 5일 이내에 계정을 복구할 수 있는 기간을 관리합니다.
 * 개인정보보호법 파기 기한(5일)과 일치시켜 법적 정합성을 확보합니다.
 */
public final class RecoveryPeriodConstants {

    public static final Duration RECOVERY_PERIOD = Duration.ofDays(5);

    private RecoveryPeriodConstants() {
    }

    /**
     * 복구 가능 기한을 계산합니다.
     *
     * @param user 사용자
     * @return 복구 가능 기한 (deletedAt + 5일)
     */
    public static Instant getRecoveryDeadline(User user) {
        Instant deletedAt = user.getDeletedAt();
        if (deletedAt == null) {
            // deletedAt이 null인 경우 (논리적 오류 방어)
            return Instant.MIN;
        }
        return deletedAt.plus(RECOVERY_PERIOD);
    }
}
