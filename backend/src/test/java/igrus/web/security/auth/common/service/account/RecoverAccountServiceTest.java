package igrus.web.security.auth.common.service.account;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.dto.internal.RecoveryResult;
import igrus.web.security.auth.common.exception.account.AccountNotRecoverableException;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RecoverAccountService 통합 테스트")
class RecoverAccountServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private RecoverAccountService recoverAccountService;

    private static final long ACCESS_TOKEN_VALIDITY = 3600000L; // 1시간
    private static final long REFRESH_TOKEN_VALIDITY = 604800000L; // 7일
    private static final String TEST_STUDENT_ID = "12345678";
    private static final String TEST_PASSWORD = "password123!";

    @BeforeEach
    void setUp() {
        setUpBase();
        ReflectionTestUtils.setField(recoverAccountService, "accessTokenValidity", ACCESS_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(recoverAccountService, "refreshTokenValidity", REFRESH_TOKEN_VALIDITY);
    }

    private User createAndSaveWithdrawnUser(UserRole role, Instant deletedAt) {
        User user = User.create(
                TEST_STUDENT_ID,
                "홍길동",
                "test@inha.edu",
                "010-1234-5678",
                "컴퓨터공학과",
                "테스트 동기",
                Gender.MALE,
                1
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
                Gender.MALE,
                1
        );
        user.changeRole(role);
        return userRepository.save(user);
    }

    private PasswordCredential createAndSaveWithdrawnCredential(User user, Instant deletedAt) {
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
        PasswordCredential credential = PasswordCredential.create(user, encodedPassword);

        credential.withdraw();
        ReflectionTestUtils.setField(credential, "deleted", true);
        ReflectionTestUtils.setField(credential, "deletedAt", deletedAt);
        ReflectionTestUtils.setField(credential, "deletedBy", user.getId());

        return passwordCredentialRepository.save(credential);
    }

    @Nested
    @DisplayName("5일 이내 복구 가능")
    class RecoveryWithinFiveDaysTest {

        @Test
        @DisplayName("계정 복구 선택 시 상태 전환 - ACTIVE로 변경 및 로그인 성공 [REC-003]")
        void recoverAccount_validCredentials_activatesAccountAndReturnsTokens() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);

            // when
            RecoveryResult response = recoverAccountService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isNotNull();
            assertThat(response.refreshToken()).isNotNull();

            User recoveredUser = userRepository.findByStudentIdIncludingDeleted(TEST_STUDENT_ID).orElseThrow();
            assertThat(recoveredUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(recoveredUser.isDeleted()).isFalse();

            PasswordCredential recoveredCredential = passwordCredentialRepository.findByUserIdIncludingDeleted(user.getId()).orElseThrow();
            assertThat(recoveredCredential.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(recoveredCredential.isDeleted()).isFalse();

            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(response.refreshToken())).isPresent();
        }

        @Test
        @DisplayName("복구 후 정상적인 서비스 이용 가능 - 토큰 발급 확인 [REC-004]")
        void recoverAccount_success_issuesValidTokens() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(1));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);

            // when
            RecoveryResult response = recoverAccountService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            // then
            assertThat(response.accessToken()).isNotNull();
            assertThat(response.refreshToken()).isNotNull();
            assertThat(response.userId()).isEqualTo(user.getId());
            assertThat(response.accessTokenValidity()).isEqualTo(ACCESS_TOKEN_VALIDITY);
        }

        @Test
        @DisplayName("복구 시 기존 역할 유지 - MEMBER 역할로 탈퇴 후 복구 [REC-005]")
        void recoverAccount_preservesOriginalRole() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);

            // when
            RecoveryResult response = recoverAccountService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            // then
            assertThat(response.role()).isEqualTo(UserRole.MEMBER);

            User recoveredUser = userRepository.findByStudentIdIncludingDeleted(TEST_STUDENT_ID).orElseThrow();
            assertThat(recoveredUser.getRole()).isEqualTo(UserRole.MEMBER);
        }
    }

    @Nested
    @DisplayName("5일 경과 후 복구 불가")
    class RecoveryAfterFiveDaysTest {

        @Test
        @DisplayName("5일 경과 후 복구 시도 - AccountNotRecoverableException 발생 [REC-021]")
        void recoverAccount_afterFiveDays_throwsAccountNotRecoverableException() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(6));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);

            // when & then
            assertThatThrownBy(() -> recoverAccountService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD))
                    .isInstanceOf(AccountNotRecoverableException.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTest {

        @Test
        @DisplayName("탈퇴 전 활성화된 토큰은 복구와 무관 - 새 토큰 발급 [REC-040]")
        void recoverAccount_issuesNewTokensRegardlessOfPreviousTokens() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);

            // when
            RecoveryResult response = recoverAccountService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            // then
            assertThat(response.accessToken()).isNotNull();
            assertThat(response.refreshToken()).isNotNull();
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(response.refreshToken())).isPresent();
        }

        @Test
        @DisplayName("잘못된 비밀번호로 복구 시도 - InvalidCredentialsException 발생, 복구 화면 미표시 [REC-042]")
        void recoverAccount_withWrongPassword_throwsInvalidCredentialsException() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);
            String wrongPassword = "wrongPassword";

            // when & then
            assertThatThrownBy(() -> recoverAccountService.recoverAccount(TEST_STUDENT_ID, wrongPassword))
                    .isInstanceOf(InvalidCredentialsException.class);

            User unchangedUser = userRepository.findByStudentIdIncludingDeleted(TEST_STUDENT_ID).orElseThrow();
            assertThat(unchangedUser.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
            assertThat(unchangedUser.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 사용자 복구 시도 - InvalidCredentialsException 발생")
        void recoverAccount_nonExistentUser_throwsInvalidCredentialsException() {
            // when & then
            assertThatThrownBy(() -> recoverAccountService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("활성 사용자 복구 시도 - InvalidCredentialsException 발생")
        void recoverAccount_activeUser_throwsInvalidCredentialsException() {
            // given
            createAndSaveActiveUser(UserRole.MEMBER);

            // when & then
            assertThatThrownBy(() -> recoverAccountService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("비밀번호 정보 없는 탈퇴 사용자 복구 시도 - InvalidCredentialsException 발생")
        void recoverAccount_noPasswordCredential_throwsInvalidCredentialsException() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when & then
            assertThatThrownBy(() -> recoverAccountService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("OPERATOR 역할 유지 복구")
        void recoverAccount_operatorRole_preservesRole() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            User user = User.create(
                    TEST_STUDENT_ID,
                    "홍길동",
                    "test@inha.edu",
                    "010-1234-5678",
                    "컴퓨터공학과",
                    "테스트 동기",
                    Gender.MALE,
                    1
            );
            user.changeRole(UserRole.OPERATOR);
            User savedUser = userRepository.save(user);
            savedUser.withdraw();
            ReflectionTestUtils.setField(savedUser, "deleted", true);
            ReflectionTestUtils.setField(savedUser, "deletedAt", deletedAt);
            ReflectionTestUtils.setField(savedUser, "deletedBy", savedUser.getId());
            userRepository.save(savedUser);

            String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
            PasswordCredential credential = PasswordCredential.create(savedUser, encodedPassword);
            credential.withdraw();
            ReflectionTestUtils.setField(credential, "deleted", true);
            ReflectionTestUtils.setField(credential, "deletedAt", deletedAt);
            ReflectionTestUtils.setField(credential, "deletedBy", savedUser.getId());
            passwordCredentialRepository.save(credential);

            // when
            RecoveryResult response = recoverAccountService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            // then
            assertThat(response.role()).isEqualTo(UserRole.OPERATOR);
        }

        @Test
        @DisplayName("ADMIN 역할 유지 복구")
        void recoverAccount_adminRole_preservesRole() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            User user = User.create(
                    TEST_STUDENT_ID,
                    "홍길동",
                    "test@inha.edu",
                    "010-1234-5678",
                    "컴퓨터공학과",
                    "테스트 동기",
                    Gender.MALE,
                    1
            );
            user.changeRole(UserRole.ADMIN);
            User savedUser = userRepository.save(user);
            savedUser.withdraw();
            ReflectionTestUtils.setField(savedUser, "deleted", true);
            ReflectionTestUtils.setField(savedUser, "deletedAt", deletedAt);
            ReflectionTestUtils.setField(savedUser, "deletedBy", savedUser.getId());
            userRepository.save(savedUser);

            String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
            PasswordCredential credential = PasswordCredential.create(savedUser, encodedPassword);
            credential.withdraw();
            ReflectionTestUtils.setField(credential, "deleted", true);
            ReflectionTestUtils.setField(credential, "deletedAt", deletedAt);
            ReflectionTestUtils.setField(credential, "deletedBy", savedUser.getId());
            passwordCredentialRepository.save(credential);

            // when
            RecoveryResult response = recoverAccountService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            // then
            assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        }
    }
}
