package igrus.web.security.auth.common.service.account;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.dto.response.RecoveryEligibilityResponse;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("CheckRecoveryEligibilityService 통합 테스트")
class CheckRecoveryEligibilityServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private CheckRecoveryEligibilityService checkRecoveryEligibilityService;

    private static final String TEST_STUDENT_ID = "12345678";
    private static final Duration RECOVERY_PERIOD = Duration.ofDays(5);

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    private User createAndSaveWithdrawnUser(UserRole role, Instant deletedAt) {
        User user = User.create(
                TEST_STUDENT_ID,
                "홍길동",
                "test@inha.edu",
                "010-1234-5678",
                "컴퓨터공학과",
                "테스트 동기",
                List.of(),
                Gender.MALE,
                1,
                EnrollmentStatus.ENROLLED,
                List.of(), null, null, null
        );
        user.changeRole(role);
        User savedUser = userRepository.save(user);

        savedUser.withdraw();
        ReflectionTestUtils.setField(savedUser, "deleted", true);
        ReflectionTestUtils.setField(savedUser, "deletedAt", deletedAt);
        ReflectionTestUtils.setField(savedUser, "deletedBy", savedUser.getId());

        return userRepository.save(savedUser);
    }

    private User createAndSaveActiveUser(UserRole role) {
        User user = User.create(
                TEST_STUDENT_ID,
                "홍길동",
                "test@inha.edu",
                "010-1234-5678",
                "컴퓨터공학과",
                "테스트 동기",
                List.of(),
                Gender.MALE,
                1,
                EnrollmentStatus.ENROLLED,
                List.of(), null, null, null
        );
        user.changeRole(role);
        return userRepository.save(user);
    }

    @Nested
    @DisplayName("5일 이내 복구 가능")
    class RecoveryWithinFiveDaysTest {

        @Test
        @DisplayName("탈퇴 직후 복구 시도 시 안내 메시지 표시 [REC-001]")
        void checkRecoveryEligibility_immediatelyAfterWithdrawal_showsRecoveryMessage() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofMinutes(1));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            RecoveryEligibilityResponse response = checkRecoveryEligibilityService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // then
            assertThat(response.recoverable()).isTrue();
            assertThat(response.message()).isEqualTo("탈퇴한 계정입니다. 복구하시겠습니까?");
            assertThat(response.recoveryDeadline()).isNotNull();
            assertThat(response.recoveryDeadline()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("5일 이내 복구 확인 화면 표시 - 3일 경과 [REC-002]")
        void checkRecoveryEligibility_threeDaysAfterWithdrawal_showsRemainingDeadline() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(3));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            RecoveryEligibilityResponse response = checkRecoveryEligibilityService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // then
            assertThat(response.recoverable()).isTrue();
            assertThat(response.recoveryDeadline()).isNotNull();
            Instant expectedDeadline = deletedAt.plus(RECOVERY_PERIOD);
            assertThat(response.recoveryDeadline()).isCloseTo(expectedDeadline, within(1, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("복구 거부")
    class RecoveryDeclineTest {

        @Test
        @DisplayName("복구 거부 시 로그인 불가 - 복구하지 않으면 계정은 탈퇴 상태 유지 [REC-010]")
        void checkRecoveryEligibility_declineRecovery_accountRemainsWithdrawn() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            RecoveryEligibilityResponse response = checkRecoveryEligibilityService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // then
            assertThat(response.recoverable()).isTrue();

            User unchangedUser = userRepository.findByStudentIdIncludingDeleted(TEST_STUDENT_ID).orElseThrow();
            assertThat(unchangedUser.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
            assertThat(unchangedUser.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("복구 거부 후 재시도 가능 - 5일 이내이면 복구 화면 다시 표시 [REC-011]")
        void checkRecoveryEligibility_afterDecline_canRetryWithinFiveDays() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(3));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            RecoveryEligibilityResponse firstResponse = checkRecoveryEligibilityService.checkRecoveryEligibility(TEST_STUDENT_ID);
            RecoveryEligibilityResponse secondResponse = checkRecoveryEligibilityService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // then
            assertThat(firstResponse.recoverable()).isTrue();
            assertThat(secondResponse.recoverable()).isTrue();
            assertThat(secondResponse.recoveryDeadline()).isCloseTo(firstResponse.recoveryDeadline(), within(1, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("5일 경과 후 복구 불가")
    class RecoveryAfterFiveDaysTest {

        @Test
        @DisplayName("5일 경과 후 로그인 시도 - 복구 불가 응답 [REC-020]")
        void checkRecoveryEligibility_afterFiveDays_notRecoverable() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(6));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            RecoveryEligibilityResponse response = checkRecoveryEligibilityService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // then
            assertThat(response.recoverable()).isFalse();
            assertThat(response.recoveryDeadline()).isNull();
            assertThat(response.message()).isEqualTo("복구 기간이 만료된 계정입니다");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTest {

        @Test
        @DisplayName("활성 사용자 복구 여부 확인 - notWithdrawn 응답")
        void checkRecoveryEligibility_activeUser_returnsNotWithdrawn() {
            // given
            createAndSaveActiveUser(UserRole.MEMBER);

            // when
            RecoveryEligibilityResponse response = checkRecoveryEligibilityService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // then
            assertThat(response.recoverable()).isFalse();
            assertThat(response.message()).isEqualTo("탈퇴 상태가 아닌 계정입니다");
        }

        @Test
        @DisplayName("존재하지 않는 사용자 복구 여부 확인 - notRecoverable 응답")
        void checkRecoveryEligibility_nonExistentUser_returnsNotRecoverable() {
            // when
            RecoveryEligibilityResponse response = checkRecoveryEligibilityService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // then
            assertThat(response.recoverable()).isFalse();
        }
    }
}
