package igrus.web.security.auth.common.service.account;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.user.domain.Gender;
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
import static org.assertj.core.api.Assertions.within;

@DisplayName("CheckReRegistrationEligibilityService 통합 테스트")
class CheckReRegistrationEligibilityServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private CheckReRegistrationEligibilityService checkReRegistrationEligibilityService;

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
                List.of(), null, null, null
        );
        user.changeRole(role);
        return userRepository.save(user);
    }

    @Nested
    @DisplayName("5일 경과 후 재가입 가능")
    class AfterFiveDaysTest {

        @Test
        @DisplayName("5일 경과 후 동일 학번 재가입 가능 - 개인정보 파기 후 상태 [REC-022]")
        void checkReRegistrationEligibility_afterFiveDays_eligible() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(6));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            ReRegistrationCheckResult result = checkReRegistrationEligibilityService.checkReRegistrationEligibility(TEST_STUDENT_ID);

            // then
            assertThat(result.isEligible()).isTrue();
            assertThat(result.isAlreadyRegistered()).isFalse();
            assertThat(result.reRegistrationAvailableAt()).isNull();
        }
    }

    @Nested
    @DisplayName("5일 이내 재가입 제한")
    class ReRegistrationRestrictionTest {

        @Test
        @DisplayName("5일 이내 동일 학번 재가입 시도 - 제한 메시지 및 재가입 가능일 표시 [REC-030]")
        void checkReRegistrationEligibility_withinFiveDays_restricted() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            ReRegistrationCheckResult result = checkReRegistrationEligibilityService.checkReRegistrationEligibility(TEST_STUDENT_ID);

            // then
            assertThat(result.isEligible()).isFalse();
            assertThat(result.isAlreadyRegistered()).isFalse();
            assertThat(result.reRegistrationAvailableAt()).isNotNull();
            assertThat(result.message()).isEqualTo("탈퇴 후 5일이 지나야 재가입할 수 있습니다");
        }

        @Test
        @DisplayName("재가입 가능일 정확히 표시 - 3일 경과 시 2일 후 재가입 가능 [REC-031]")
        void checkReRegistrationEligibility_threeDaysAfter_showsCorrectDate() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(3));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            ReRegistrationCheckResult result = checkReRegistrationEligibilityService.checkReRegistrationEligibility(TEST_STUDENT_ID);

            // then
            assertThat(result.isEligible()).isFalse();
            Instant expectedAvailableAt = deletedAt.plus(RECOVERY_PERIOD);
            assertThat(result.reRegistrationAvailableAt()).isCloseTo(expectedAvailableAt, within(1, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("탈퇴 후 4일 23시간 경과 시 재가입 제한 [REC-032]")
        void checkReRegistrationEligibility_almostFiveDays_stillRestricted() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(4).plusHours(23));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            ReRegistrationCheckResult result = checkReRegistrationEligibilityService.checkReRegistrationEligibility(TEST_STUDENT_ID);

            // then
            assertThat(result.isEligible()).isFalse();
            assertThat(result.reRegistrationAvailableAt()).isNotNull();
            assertThat(result.reRegistrationAvailableAt()).isBefore(Instant.now().plus(Duration.ofHours(2)));
        }

        @Test
        @DisplayName("정확히 5일 경과 후 재가입 가능 [REC-033]")
        void checkReRegistrationEligibility_exactlyFiveDays_eligible() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(5).plusSeconds(1));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            ReRegistrationCheckResult result = checkReRegistrationEligibilityService.checkReRegistrationEligibility(TEST_STUDENT_ID);

            // then
            assertThat(result.isEligible()).isTrue();
            assertThat(result.reRegistrationAvailableAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTest {

        @Test
        @DisplayName("재가입 가능 여부 확인 - 존재하지 않는 사용자 - eligible 응답")
        void checkReRegistrationEligibility_nonExistentUser_eligible() {
            // when
            ReRegistrationCheckResult result = checkReRegistrationEligibilityService.checkReRegistrationEligibility(TEST_STUDENT_ID);

            // then
            assertThat(result.isEligible()).isTrue();
            assertThat(result.isAlreadyRegistered()).isFalse();
        }

        @Test
        @DisplayName("재가입 가능 여부 확인 - 활성 사용자 - alreadyRegistered 응답")
        void checkReRegistrationEligibility_activeUser_alreadyRegistered() {
            // given
            createAndSaveActiveUser(UserRole.MEMBER);

            // when
            ReRegistrationCheckResult result = checkReRegistrationEligibilityService.checkReRegistrationEligibility(TEST_STUDENT_ID);

            // then
            assertThat(result.isEligible()).isFalse();
            assertThat(result.isAlreadyRegistered()).isTrue();
            assertThat(result.message()).isEqualTo("이미 가입된 학번입니다");
        }
    }
}
