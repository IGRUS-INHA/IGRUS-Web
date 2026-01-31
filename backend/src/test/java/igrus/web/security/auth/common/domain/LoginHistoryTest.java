package igrus.web.security.auth.common.domain;

import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoginHistory 단위 테스트")
class LoginHistoryTest {

    private static final String TEST_STUDENT_ID = "12345678";
    private static final String TEST_IP_ADDRESS = "192.168.1.100";
    private static final String TEST_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    @Nested
    @DisplayName("success")
    class SuccessTest {

        @Test
        @DisplayName("성공 히스토리 생성 시 success가 true")
        void success_CreatesHistoryWithSuccessTrue() {
            // given
            User user = createTestUser();

            // when
            LoginHistory history = LoginHistory.success(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);

            // then
            assertThat(history.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("성공 히스토리 생성 시 failureReason이 null")
        void success_CreatesHistoryWithNullFailureReason() {
            // given
            User user = createTestUser();

            // when
            LoginHistory history = LoginHistory.success(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);

            // then
            assertThat(history.getFailureReason()).isNull();
        }

        @Test
        @DisplayName("성공 히스토리 생성 시 user가 설정됨")
        void success_CreatesHistoryWithUser() {
            // given
            User user = createTestUser();

            // when
            LoginHistory history = LoginHistory.success(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);

            // then
            assertThat(history.getUser()).isEqualTo(user);
        }

        @Test
        @DisplayName("성공 히스토리 생성 시 모든 필드가 올바르게 설정됨")
        void success_CreatesHistoryWithAllFields() {
            // given
            User user = createTestUser();

            // when
            LoginHistory history = LoginHistory.success(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);

            // then
            assertThat(history.getStudentId()).isEqualTo(TEST_STUDENT_ID);
            assertThat(history.getIpAddress()).isEqualTo(TEST_IP_ADDRESS);
            assertThat(history.getUserAgent()).isEqualTo(TEST_USER_AGENT);
            assertThat(history.getAttemptedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("failure - 사용자 없음")
    class FailureWithoutUserTest {

        @Test
        @DisplayName("사용자 없는 실패 히스토리 생성 시 success가 false")
        void failure_CreatesHistoryWithSuccessFalse() {
            // when
            LoginHistory history = LoginHistory.failure(TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                    LoginFailureReason.INVALID_CREDENTIALS);

            // then
            assertThat(history.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("사용자 없는 실패 히스토리 생성 시 user가 null")
        void failure_CreatesHistoryWithNullUser() {
            // when
            LoginHistory history = LoginHistory.failure(TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                    LoginFailureReason.INVALID_CREDENTIALS);

            // then
            assertThat(history.getUser()).isNull();
        }

        @Test
        @DisplayName("실패 히스토리 생성 시 failureReason이 설정됨")
        void failure_CreatesHistoryWithFailureReason() {
            // when
            LoginHistory history = LoginHistory.failure(TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                    LoginFailureReason.ACCOUNT_LOCKED);

            // then
            assertThat(history.getFailureReason()).isEqualTo(LoginFailureReason.ACCOUNT_LOCKED);
        }
    }

    @Nested
    @DisplayName("failure - 사용자 있음")
    class FailureWithUserTest {

        @Test
        @DisplayName("사용자 있는 실패 히스토리 생성 시 user가 설정됨")
        void failure_CreatesHistoryWithUser() {
            // given
            User user = createTestUser();

            // when
            LoginHistory history = LoginHistory.failure(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                    LoginFailureReason.EMAIL_NOT_VERIFIED);

            // then
            assertThat(history.getUser()).isEqualTo(user);
            assertThat(history.isSuccess()).isFalse();
            assertThat(history.getFailureReason()).isEqualTo(LoginFailureReason.EMAIL_NOT_VERIFIED);
        }
    }

    @Nested
    @DisplayName("User-Agent 처리")
    class UserAgentTest {

        @Test
        @DisplayName("500자 이하 User-Agent는 그대로 저장")
        void truncate_ShortUserAgent_SavesAsIs() {
            // given
            String shortUserAgent = "Short User-Agent";

            // when
            LoginHistory history = LoginHistory.failure(TEST_STUDENT_ID, TEST_IP_ADDRESS, shortUserAgent,
                    LoginFailureReason.INVALID_CREDENTIALS);

            // then
            assertThat(history.getUserAgent()).isEqualTo(shortUserAgent);
        }

        @Test
        @DisplayName("500자 초과 User-Agent는 잘림")
        void truncate_LongUserAgent_Truncates() {
            // given
            String longUserAgent = "A".repeat(600);

            // when
            LoginHistory history = LoginHistory.failure(TEST_STUDENT_ID, TEST_IP_ADDRESS, longUserAgent,
                    LoginFailureReason.INVALID_CREDENTIALS);

            // then
            assertThat(history.getUserAgent()).hasSize(500);
            assertThat(history.getUserAgent()).isEqualTo("A".repeat(500));
        }

        @Test
        @DisplayName("null User-Agent는 null로 저장")
        void truncate_NullUserAgent_SavesNull() {
            // when
            LoginHistory history = LoginHistory.failure(TEST_STUDENT_ID, TEST_IP_ADDRESS, null,
                    LoginFailureReason.INVALID_CREDENTIALS);

            // then
            assertThat(history.getUserAgent()).isNull();
        }
    }

    @Nested
    @DisplayName("모든 실패 사유 테스트")
    class AllFailureReasonsTest {

        @Test
        @DisplayName("INVALID_CREDENTIALS 사유로 기록")
        void failure_InvalidCredentials() {
            LoginHistory history = LoginHistory.failure(TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                    LoginFailureReason.INVALID_CREDENTIALS);
            assertThat(history.getFailureReason()).isEqualTo(LoginFailureReason.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("ACCOUNT_LOCKED 사유로 기록")
        void failure_AccountLocked() {
            LoginHistory history = LoginHistory.failure(TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                    LoginFailureReason.ACCOUNT_LOCKED);
            assertThat(history.getFailureReason()).isEqualTo(LoginFailureReason.ACCOUNT_LOCKED);
        }

        @Test
        @DisplayName("ACCOUNT_SUSPENDED 사유로 기록")
        void failure_AccountSuspended() {
            LoginHistory history = LoginHistory.failure(TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                    LoginFailureReason.ACCOUNT_SUSPENDED);
            assertThat(history.getFailureReason()).isEqualTo(LoginFailureReason.ACCOUNT_SUSPENDED);
        }

        @Test
        @DisplayName("ACCOUNT_WITHDRAWN 사유로 기록")
        void failure_AccountWithdrawn() {
            LoginHistory history = LoginHistory.failure(TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                    LoginFailureReason.ACCOUNT_WITHDRAWN);
            assertThat(history.getFailureReason()).isEqualTo(LoginFailureReason.ACCOUNT_WITHDRAWN);
        }

        @Test
        @DisplayName("EMAIL_NOT_VERIFIED 사유로 기록")
        void failure_EmailNotVerified() {
            LoginHistory history = LoginHistory.failure(TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                    LoginFailureReason.EMAIL_NOT_VERIFIED);
            assertThat(history.getFailureReason()).isEqualTo(LoginFailureReason.EMAIL_NOT_VERIFIED);
        }

        @Test
        @DisplayName("ACCOUNT_RECOVERABLE 사유로 기록")
        void failure_AccountRecoverable() {
            LoginHistory history = LoginHistory.failure(TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                    LoginFailureReason.ACCOUNT_RECOVERABLE);
            assertThat(history.getFailureReason()).isEqualTo(LoginFailureReason.ACCOUNT_RECOVERABLE);
        }
    }

    private User createTestUser() {
        User user = User.create(
                TEST_STUDENT_ID,
                "테스트유저",
                "test@inha.edu",
                "010-1234-5678",
                "컴퓨터공학과",
                "테스트 동기",
                Gender.MALE,
                1
        );
        user.changeRole(UserRole.MEMBER);
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
