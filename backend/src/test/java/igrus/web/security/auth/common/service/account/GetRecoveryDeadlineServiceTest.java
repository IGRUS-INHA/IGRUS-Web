package igrus.web.security.auth.common.service.account;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.exception.account.AccountNotRecoverableException;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("GetRecoveryDeadlineService 통합 테스트")
class GetRecoveryDeadlineServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetRecoveryDeadlineService getRecoveryDeadlineService;

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
    @DisplayName("복구 기한 조회")
    class GetDeadlineTest {

        @Test
        @DisplayName("복구 기한 조회 - 탈퇴 사용자")
        void getRecoveryDeadline_withdrawnUser_returnsDeadline() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            Instant deadline = getRecoveryDeadlineService.getRecoveryDeadline(TEST_STUDENT_ID);

            // then
            Instant expectedDeadline = deletedAt.plus(RECOVERY_PERIOD);
            assertThat(deadline).isCloseTo(expectedDeadline, within(1, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("복구 기한 조회 - 활성 사용자 - AccountNotRecoverableException 발생")
        void getRecoveryDeadline_activeUser_throwsAccountNotRecoverableException() {
            // given
            createAndSaveActiveUser(UserRole.MEMBER);

            // when & then
            assertThatThrownBy(() -> getRecoveryDeadlineService.getRecoveryDeadline(TEST_STUDENT_ID))
                    .isInstanceOf(AccountNotRecoverableException.class);
        }

        @Test
        @DisplayName("복구 기한 조회 - 존재하지 않는 사용자 - InvalidCredentialsException 발생")
        void getRecoveryDeadline_nonExistentUser_throwsInvalidCredentialsException() {
            // when & then
            assertThatThrownBy(() -> getRecoveryDeadlineService.getRecoveryDeadline(TEST_STUDENT_ID))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }
}
