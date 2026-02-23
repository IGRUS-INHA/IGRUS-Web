package igrus.web.security.auth.password.domain;

import igrus.web.user.domain.Gender;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordCredential 도메인")
class PasswordCredentialTest {

    private User createTestUser() {
        return User.create("20231234", "홍길동", "test@inha.edu", "010-1234-5678", "컴퓨터공학과", "테스트 동기", List.of(), Gender.MALE, 1, EnrollmentStatus.ENROLLED, List.of(), null, null, null);
    }

    @Nested
    @DisplayName("create 정적 팩토리 메서드")
    class CreateTest {

        @Test
        @DisplayName("유효한 정보로 PasswordCredential 생성 성공")
        void create_WithValidInfo_ReturnsPasswordCredential() {
            // given
            User user = createTestUser();
            String passwordHash = "$2a$10$hashedPassword";

            // when
            PasswordCredential credential = PasswordCredential.create(user, passwordHash);

            // then
            assertThat(credential).isNotNull();
            assertThat(credential.getUser()).isEqualTo(user);
            assertThat(credential.getPasswordHash()).isEqualTo(passwordHash);
        }

        @Test
        @DisplayName("생성 시 기본 상태는 ACTIVE (이메일 사전 인증 완료 후 가입)")
        void create_DefaultStatus_IsActive() {
            // given
            User user = createTestUser();
            String passwordHash = "$2a$10$hashedPassword";

            // when
            PasswordCredential credential = PasswordCredential.create(user, passwordHash);

            // then
            assertThat(credential.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(credential.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("비밀번호 관련 메서드")
    class PasswordTest {

        @Test
        @DisplayName("changePassword로 비밀번호 변경 성공")
        void changePassword_WithNewHash_UpdatesPasswordHash() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$oldPassword");
            String newPasswordHash = "$2a$10$newPassword";

            // when
            credential.changePassword(newPasswordHash);

            // then
            assertThat(credential.getPasswordHash()).isEqualTo(newPasswordHash);
        }
    }

    @Nested
    @DisplayName("계정 상태 관련 메서드")
    class StatusTest {

        @Test
        @DisplayName("activate 호출 시 ACTIVE로 변경")
        void activate_ChangesStatusToActive() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");
            credential.suspend(); // 먼저 정지 상태로 변경

            // when
            credential.activate();

            // then
            assertThat(credential.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(credential.isActive()).isTrue();
        }

        @Test
        @DisplayName("suspend 호출 시 SUSPENDED로 변경")
        void suspend_ChangesStatusToSuspended() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");

            // when
            credential.suspend();

            // then
            assertThat(credential.getStatus()).isEqualTo(UserStatus.SUSPENDED);
            assertThat(credential.isSuspended()).isTrue();
        }

        @Test
        @DisplayName("withdraw 호출 시 WITHDRAWN으로 변경")
        void withdraw_ChangesStatusToWithdrawn() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");

            // when
            credential.withdraw();

            // then
            assertThat(credential.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
            assertThat(credential.isWithdrawn()).isTrue();
        }

        @Test
        @DisplayName("isActive - ACTIVE일 때 true 반환")
        void isActive_WhenActive_ReturnsTrue() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");

            // then
            assertThat(credential.isActive()).isTrue();
        }

        @Test
        @DisplayName("isActive - SUSPENDED일 때 false 반환")
        void isActive_WhenSuspended_ReturnsFalse() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");
            credential.suspend();

            // then
            assertThat(credential.isActive()).isFalse();
        }

        @Test
        @DisplayName("isActive - WITHDRAWN일 때 false 반환")
        void isActive_WhenWithdrawn_ReturnsFalse() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");
            credential.withdraw();

            // then
            assertThat(credential.isActive()).isFalse();
        }

        @Test
        @DisplayName("isSuspended - SUSPENDED일 때 true 반환")
        void isSuspended_WhenSuspended_ReturnsTrue() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");
            credential.suspend();

            // then
            assertThat(credential.isSuspended()).isTrue();
        }

        @Test
        @DisplayName("isSuspended - ACTIVE일 때 false 반환")
        void isSuspended_WhenActive_ReturnsFalse() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");

            // then
            assertThat(credential.isSuspended()).isFalse();
        }

        @Test
        @DisplayName("isSuspended - WITHDRAWN일 때 false 반환")
        void isSuspended_WhenWithdrawn_ReturnsFalse() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");
            credential.withdraw();

            // then
            assertThat(credential.isSuspended()).isFalse();
        }

        @Test
        @DisplayName("isWithdrawn - WITHDRAWN일 때 true 반환")
        void isWithdrawn_WhenWithdrawn_ReturnsTrue() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");
            credential.withdraw();

            // then
            assertThat(credential.isWithdrawn()).isTrue();
        }

        @Test
        @DisplayName("isWithdrawn - ACTIVE일 때 false 반환")
        void isWithdrawn_WhenActive_ReturnsFalse() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");

            // then
            assertThat(credential.isWithdrawn()).isFalse();
        }

        @Test
        @DisplayName("isPendingVerification - PENDING_VERIFICATION일 때 true 반환")
        void isPendingVerification_WhenPendingVerification_ReturnsTrue() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");
            ReflectionTestUtils.setField(credential, "status", UserStatus.PENDING_VERIFICATION);

            // then
            assertThat(credential.isPendingVerification()).isTrue();
        }

        @Test
        @DisplayName("isPendingVerification - ACTIVE일 때 false 반환")
        void isPendingVerification_WhenActive_ReturnsFalse() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");

            // then
            assertThat(credential.isPendingVerification()).isFalse();
        }

        @Test
        @DisplayName("verifyEmail 호출 시 PENDING_VERIFICATION에서 ACTIVE로 변경")
        void verifyEmail_WhenPendingVerification_ChangesToActive() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");
            ReflectionTestUtils.setField(credential, "status", UserStatus.PENDING_VERIFICATION);
            assertThat(credential.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);

            // when
            credential.verifyEmail();

            // then
            assertThat(credential.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(credential.isActive()).isTrue();
        }

        @Test
        @DisplayName("verifyEmail - 이미 ACTIVE인 경우 상태 유지")
        void verifyEmail_WhenAlreadyActive_KeepsActive() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");
            assertThat(credential.getStatus()).isEqualTo(UserStatus.ACTIVE);

            // when
            credential.verifyEmail();

            // then
            assertThat(credential.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("isWithdrawn - SUSPENDED일 때 false 반환")
        void isWithdrawn_WhenSuspended_ReturnsFalse() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");
            credential.suspend();

            // then
            assertThat(credential.isWithdrawn()).isFalse();
        }
    }
}
