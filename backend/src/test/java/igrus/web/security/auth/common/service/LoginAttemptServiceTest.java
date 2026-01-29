package igrus.web.security.auth.common.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.LoginAttempt;
import igrus.web.security.auth.common.exception.account.AccountLockedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LoginAttemptService 통합 테스트")
class LoginAttemptServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private LoginAttemptService loginAttemptService;

    private static final String TEST_STUDENT_ID = "20231234";
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;

    @BeforeEach
    void setUp() {
        setUpBase();
        // Set up configuration values
        ReflectionTestUtils.setField(loginAttemptService, "maxAttempts", MAX_ATTEMPTS);
        ReflectionTestUtils.setField(loginAttemptService, "lockoutMinutes", LOCKOUT_MINUTES);
    }

    @Nested
    @DisplayName("checkAccountLocked")
    class CheckAccountLockedTest {

        @Test
        @DisplayName("잠금 기록이 없으면 정상 통과")
        void checkAccountLocked_NoRecord_Passes() {
            // when & then - 예외가 발생하지 않아야 함
            loginAttemptService.checkAccountLocked(TEST_STUDENT_ID);
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
            loginAttemptService.checkAccountLocked(TEST_STUDENT_ID);
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
            assertThatThrownBy(() -> loginAttemptService.checkAccountLocked(TEST_STUDENT_ID))
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
            loginAttemptService.checkAccountLocked(TEST_STUDENT_ID);
        }
    }

    @Nested
    @DisplayName("recordFailedAttempt")
    class RecordFailedAttemptTest {

        @Test
        @DisplayName("첫 번째 실패 시 새 레코드 생성 및 시도 횟수 1")
        void recordFailedAttempt_FirstAttempt_CreatesNewRecord() {
            // when
            loginAttemptService.recordFailedAttempt(TEST_STUDENT_ID);

            // then - 상태 검증
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
            loginAttemptService.recordFailedAttempt(TEST_STUDENT_ID);

            // then - 상태 검증
            Optional<LoginAttempt> savedAttempt = loginAttemptRepository.findByStudentId(TEST_STUDENT_ID);
            assertThat(savedAttempt).isPresent();
            assertThat(savedAttempt.get().getAttemptCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("최대 시도 횟수 도달 시 계정 잠금")
        void recordFailedAttempt_MaxAttemptsReached_LocksAccount() {
            // given
            LoginAttempt attempt = LoginAttempt.create(TEST_STUDENT_ID);
            // 이미 4번 실패 (다음 실패 시 5번으로 최대 도달)
            for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
                attempt.incrementAttempt();
            }
            transactionTemplate.execute(status -> {
                loginAttemptRepository.save(attempt);
                return null;
            });

            // when
            loginAttemptService.recordFailedAttempt(TEST_STUDENT_ID);

            // then - 상태 검증
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
            // 2번 실패 (다음 실패 시 3번)
            attempt.incrementAttempt();
            attempt.incrementAttempt();
            transactionTemplate.execute(status -> {
                loginAttemptRepository.save(attempt);
                return null;
            });

            // when
            loginAttemptService.recordFailedAttempt(TEST_STUDENT_ID);

            // then - 상태 검증
            Optional<LoginAttempt> savedAttempt = loginAttemptRepository.findByStudentId(TEST_STUDENT_ID);
            assertThat(savedAttempt).isPresent();
            assertThat(savedAttempt.get().getAttemptCount()).isEqualTo(3);
            assertThat(savedAttempt.get().isLocked()).isFalse();
        }
    }

    @Nested
    @DisplayName("resetAttempts")
    class ResetAttemptsTest {

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
            loginAttemptService.resetAttempts(TEST_STUDENT_ID);

            // then - 상태 검증
            Optional<LoginAttempt> savedAttempt = loginAttemptRepository.findByStudentId(TEST_STUDENT_ID);
            assertThat(savedAttempt).isPresent();
            assertThat(savedAttempt.get().getAttemptCount()).isZero();
            assertThat(savedAttempt.get().isLocked()).isFalse();
        }

        @Test
        @DisplayName("기록이 없으면 아무 작업도 수행하지 않음")
        void resetAttempts_NoRecord_DoesNothing() {
            // when
            loginAttemptService.resetAttempts(TEST_STUDENT_ID);

            // then - 새 레코드가 생성되지 않음
            Optional<LoginAttempt> savedAttempt = loginAttemptRepository.findByStudentId(TEST_STUDENT_ID);
            assertThat(savedAttempt).isEmpty();
        }
    }
}
