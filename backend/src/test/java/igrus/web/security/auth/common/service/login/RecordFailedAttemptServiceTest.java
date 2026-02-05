package igrus.web.security.auth.common.service.login;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.LoginAttempt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecordFailedAttemptService 통합 테스트")
class RecordFailedAttemptServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private RecordFailedAttemptService recordFailedAttemptService;

    private static final String TEST_STUDENT_ID = "20231234";
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;

    @BeforeEach
    void setUp() {
        setUpBase();
        ReflectionTestUtils.setField(recordFailedAttemptService, "maxAttempts", MAX_ATTEMPTS);
        ReflectionTestUtils.setField(recordFailedAttemptService, "lockoutMinutes", LOCKOUT_MINUTES);
    }

    @Test
    @DisplayName("첫 번째 실패 시 새 레코드 생성 및 시도 횟수 1")
    void recordFailedAttempt_FirstAttempt_CreatesNewRecord() {
        // when
        recordFailedAttemptService.recordFailedAttempt(TEST_STUDENT_ID);

        // then
        Optional<LoginAttempt> savedAttempt = loginAttemptRepository.findByStudentId(TEST_STUDENT_ID);
        assertThat(savedAttempt).isPresent();
        assertThat(savedAttempt.get().getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("기존 레코드가 있으면 시도 횟수 증가")
    void recordFailedAttempt_ExistingRecord_IncrementsCount() {
        // given
        LoginAttempt attempt = LoginAttempt.create(TEST_STUDENT_ID);
        transactionTemplate.execute(status -> {
            loginAttemptRepository.save(attempt);
            return null;
        });

        // when
        recordFailedAttemptService.recordFailedAttempt(TEST_STUDENT_ID);

        // then
        Optional<LoginAttempt> savedAttempt = loginAttemptRepository.findByStudentId(TEST_STUDENT_ID);
        assertThat(savedAttempt).isPresent();
        assertThat(savedAttempt.get().getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("최대 시도 횟수 도달 시 계정 잠금")
    void recordFailedAttempt_MaxAttemptsReached_LocksAccount() {
        // given
        LoginAttempt attempt = LoginAttempt.create(TEST_STUDENT_ID);
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            attempt.incrementAttempt();
        }
        transactionTemplate.execute(status -> {
            loginAttemptRepository.save(attempt);
            return null;
        });

        // when
        recordFailedAttemptService.recordFailedAttempt(TEST_STUDENT_ID);

        // then
        Optional<LoginAttempt> savedAttempt = loginAttemptRepository.findByStudentId(TEST_STUDENT_ID);
        assertThat(savedAttempt).isPresent();
        assertThat(savedAttempt.get().getAttemptCount()).isEqualTo(MAX_ATTEMPTS);
        assertThat(savedAttempt.get().isLocked()).isTrue();
    }

    @Test
    @DisplayName("최대 시도 횟수 미만이면 계정 잠금 안 함")
    void recordFailedAttempt_BelowMaxAttempts_DoesNotLock() {
        // given
        LoginAttempt attempt = LoginAttempt.create(TEST_STUDENT_ID);
        attempt.incrementAttempt();
        attempt.incrementAttempt();
        transactionTemplate.execute(status -> {
            loginAttemptRepository.save(attempt);
            return null;
        });

        // when
        recordFailedAttemptService.recordFailedAttempt(TEST_STUDENT_ID);

        // then
        Optional<LoginAttempt> savedAttempt = loginAttemptRepository.findByStudentId(TEST_STUDENT_ID);
        assertThat(savedAttempt).isPresent();
        assertThat(savedAttempt.get().getAttemptCount()).isEqualTo(3);
        assertThat(savedAttempt.get().isLocked()).isFalse();
    }
}
