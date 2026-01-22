package igrus.web.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordCredential 도메인")
class PasswordCredentialTest {

    private User createTestUser() {
        return User.create("20231234", "홍길동", "test@inha.edu", "010-1234-5678", "컴퓨터공학과", "테스트 동기");
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
        @DisplayName("생성 시 기본 상태는 ACTIVE")
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

        @Test
        @DisplayName("생성 시 approvedAt은 null (미승인 상태)")
        void create_ApprovedAt_IsNull() {
            // given
            User user = createTestUser();
            String passwordHash = "$2a$10$hashedPassword";

            // when
            PasswordCredential credential = PasswordCredential.create(user, passwordHash);

            // then
            assertThat(credential.getApprovedAt()).isNull();
            assertThat(credential.getApprovedBy()).isNull();
            assertThat(credential.isApproved()).isFalse();
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

    @Nested
    @DisplayName("정회원 승인 메서드")
    class ApprovalTest {

        @Test
        @DisplayName("approve 호출 시 승인 정보 기록")
        void approve_WithApproverId_SetsApprovalInfo() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");
            Long approverId = 100L;

            // when
            credential.approve(approverId);

            // then
            assertThat(credential.getApprovedAt()).isNotNull();
            assertThat(credential.getApprovedBy()).isEqualTo(approverId);
            assertThat(credential.isApproved()).isTrue();
        }

        @Test
        @DisplayName("isApproved - 승인 후 true 반환")
        void isApproved_AfterApproval_ReturnsTrue() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");
            credential.approve(100L);

            // then
            assertThat(credential.isApproved()).isTrue();
        }

        @Test
        @DisplayName("isApproved - 승인 전 false 반환")
        void isApproved_BeforeApproval_ReturnsFalse() {
            // given
            User user = createTestUser();
            PasswordCredential credential = PasswordCredential.create(user, "$2a$10$hashedPassword");

            // then
            assertThat(credential.isApproved()).isFalse();
        }
    }
}
