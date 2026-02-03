package igrus.web.security.auth.common.service.login;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.LoginAttempt;
import igrus.web.security.auth.common.exception.account.AccountLockedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CheckAccountLockedService 통합 테스트")
class CheckAccountLockedServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private CheckAccountLockedService checkAccountLockedService;

    private static final String TEST_STUDENT_ID = "20231234";
    private static final int LOCKOUT_MINUTES = 30;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Test
    @DisplayName("잠금 기록이 없으면 정상 통과")
    void checkAccountLocked_NoRecord_Passes() {
        // when & then - 예외가 발생하지 않아야 함
        checkAccountLockedService.checkAccountLocked(TEST_STUDENT_ID);
    }

    @Test
    @DisplayName("잠금 상태가 아니면 정상 통과")
    void checkAccountLocked_NotLocked_Passes() {
        // given
        LoginAttempt attempt = LoginAttempt.create(TEST_STUDENT_ID);
        transactionTemplate.execute(status -> {
            loginAttemptRepository.save(attempt);
            return null;
        });

        // when & then - 예외가 발생하지 않아야 함
        checkAccountLockedService.checkAccountLocked(TEST_STUDENT_ID);
    }

    @Test
    @DisplayName("잠금 상태이면 AccountLockedException 발생")
    void checkAccountLocked_WhenLocked_ThrowsException() {
        // given
        LoginAttempt attempt = LoginAttempt.create(TEST_STUDENT_ID);
        attempt.lock(LOCKOUT_MINUTES);
        transactionTemplate.execute(status -> {
            loginAttemptRepository.save(attempt);
            return null;
        });

        // when & then
        assertThatThrownBy(() -> checkAccountLockedService.checkAccountLocked(TEST_STUDENT_ID))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    @DisplayName("잠금 시간이 만료되면 정상 통과")
    void checkAccountLocked_LockExpired_Passes() {
        // given
        LoginAttempt attempt = LoginAttempt.create(TEST_STUDENT_ID);
        // 과거 시간으로 잠금 설정 (이미 만료됨)
        ReflectionTestUtils.setField(attempt, "lockedUntil", Instant.now().minusSeconds(60));
        transactionTemplate.execute(status -> {
            loginAttemptRepository.save(attempt);
            return null;
        });

        // when & then - 예외가 발생하지 않아야 함
        checkAccountLockedService.checkAccountLocked(TEST_STUDENT_ID);
    }
}
