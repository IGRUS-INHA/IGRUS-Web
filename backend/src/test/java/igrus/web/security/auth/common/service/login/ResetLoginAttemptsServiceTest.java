package igrus.web.security.auth.common.service.login;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.LoginAttempt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResetLoginAttemptsService 통합 테스트")
class ResetLoginAttemptsServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private ResetLoginAttemptsService resetLoginAttemptsService;

    private static final String TEST_STUDENT_ID = "20231234";
    private static final int LOCKOUT_MINUTES = 30;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Test
    @DisplayName("기록이 있으면 시도 횟수 초기화")
    void resetAttempts_ExistingRecord_ResetsCount() {
        // given
        LoginAttempt attempt = LoginAttempt.create(TEST_STUDENT_ID);
        attempt.incrementAttempt();
        attempt.incrementAttempt();
        attempt.lock(LOCKOUT_MINUTES);
        transactionTemplate.execute(status -> {
            loginAttemptRepository.save(attempt);
            return null;
        });

        // when
        resetLoginAttemptsService.resetAttempts(TEST_STUDENT_ID);

        // then
        Optional<LoginAttempt> savedAttempt = loginAttemptRepository.findByStudentId(TEST_STUDENT_ID);
        assertThat(savedAttempt).isPresent();
        assertThat(savedAttempt.get().getAttemptCount()).isZero();
        assertThat(savedAttempt.get().isLocked()).isFalse();
    }

    @Test
    @DisplayName("기록이 없으면 아무 작업도 수행하지 않음")
    void resetAttempts_NoRecord_DoesNothing() {
        // when
        resetLoginAttemptsService.resetAttempts(TEST_STUDENT_ID);

        // then
        Optional<LoginAttempt> savedAttempt = loginAttemptRepository.findByStudentId(TEST_STUDENT_ID);
        assertThat(savedAttempt).isEmpty();
    }
}
