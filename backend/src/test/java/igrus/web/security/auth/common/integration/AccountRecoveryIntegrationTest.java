package igrus.web.security.auth.common.integration;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.dto.request.AccountRecoveryRequest;
import igrus.web.security.auth.common.dto.response.AccountRecoveryResponse;
import igrus.web.security.auth.common.dto.response.RecoveryEligibilityResponse;
import igrus.web.security.auth.common.exception.account.AccountRecoverableException;
import igrus.web.security.auth.common.exception.account.AccountWithdrawnException;
import igrus.web.security.auth.common.service.AccountRecoveryService;
import igrus.web.security.auth.common.service.AccountRecoveryService.ReRegistrationCheckResult;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.dto.request.PasswordLoginRequest;
import igrus.web.security.auth.password.dto.response.PasswordLoginResponse;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.security.auth.password.service.PasswordAuthService;
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
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * 탈퇴 계정 복구 기능 통합 테스트.
 * <p>
 * 테스트 케이스 문서: docs/test-case/auth/account-recovery-test-cases.md
 * </p>
 */
@DisplayName("계정 복구 통합 테스트")
class AccountRecoveryIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private PasswordAuthService passwordAuthService;

    @Autowired
    private AccountRecoveryService accountRecoveryService;

    private static final long ACCESS_TOKEN_VALIDITY = 3600000L; // 1시간
    private static final long REFRESH_TOKEN_VALIDITY = 604800000L; // 7일
    private static final String TEST_STUDENT_ID = "12345678";
    private static final String TEST_PASSWORD = "password123!";
    private static final Duration RECOVERY_PERIOD = Duration.ofDays(5);

    @BeforeEach
    void setUp() {
        setUpBase();
        ReflectionTestUtils.setField(passwordAuthService, "accessTokenValidity", ACCESS_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(passwordAuthService, "refreshTokenValidity", REFRESH_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(accountRecoveryService, "accessTokenValidity", ACCESS_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(accountRecoveryService, "refreshTokenValidity", REFRESH_TOKEN_VALIDITY);
    }

    private User createAndSaveWithdrawnUser(UserRole role, Instant deletedAt) {
        User user = User.create(
                TEST_STUDENT_ID,
                "홍길동",
                "test@inha.edu",
                "010-1234-5678",
                "컴퓨터공학과",
                "테스트 동기"
        );
        user.changeRole(role);
        user.verifyEmail(); // PENDING_VERIFICATION -> ACTIVE
        User savedUser = userRepository.save(user);

        // Withdraw the user
        savedUser.withdraw();
        ReflectionTestUtils.setField(savedUser, "deleted", true);
        ReflectionTestUtils.setField(savedUser, "deletedAt", deletedAt);
        ReflectionTestUtils.setField(savedUser, "deletedBy", savedUser.getId());

        return userRepository.save(savedUser);
    }

    private PasswordCredential createAndSaveWithdrawnCredential(User user, Instant deletedAt) {
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
        PasswordCredential credential = PasswordCredential.create(user, encodedPassword);
        credential.verifyEmail();
        credential.withdraw();
        ReflectionTestUtils.setField(credential, "deleted", true);
        ReflectionTestUtils.setField(credential, "deletedAt", deletedAt);
        ReflectionTestUtils.setField(credential, "deletedBy", user.getId());

        return passwordCredentialRepository.save(credential);
    }

    private User createAndSaveActiveUser(UserRole role) {
        User user = User.create(
                TEST_STUDENT_ID,
                "홍길동",
                "test@inha.edu",
                "010-1234-5678",
                "컴퓨터공학과",
                "테스트 동기"
        );
        user.changeRole(role);
        user.verifyEmail();
        return userRepository.save(user);
    }

    private PasswordCredential createAndSaveActiveCredential(User user) {
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
        PasswordCredential credential = PasswordCredential.create(user, encodedPassword);
        credential.verifyEmail();
        return passwordCredentialRepository.save(credential);
    }

    @Nested
    @DisplayName("5일 이내 복구 가능")
    class RecoveryWithinFiveDaysTest {

        @Test
        @DisplayName("[REC-001] 탈퇴 직후 로그인 시도 시 AccountRecoverableException 발생 및 복구 안내")
        void login_immediatelyAfterWithdrawal_throwsAccountRecoverableException() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofMinutes(1));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);
            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when & then
            assertThatThrownBy(() -> passwordAuthService.login(request))
                    .isInstanceOf(AccountRecoverableException.class)
                    .satisfies(ex -> {
                        AccountRecoverableException ace = (AccountRecoverableException) ex;
                        assertThat(ace.getStudentId()).isEqualTo(TEST_STUDENT_ID);
                        assertThat(ace.getRecoveryDeadline()).isAfter(Instant.now());
                    });
        }

        @Test
        @DisplayName("[REC-002] 탈퇴 후 3일 경과 시 로그인 시도 시 복구 가능 기한 정확히 표시")
        void login_threeDaysAfterWithdrawal_showsCorrectDeadline() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(3));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);
            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when & then
            assertThatThrownBy(() -> passwordAuthService.login(request))
                    .isInstanceOf(AccountRecoverableException.class)
                    .satisfies(ex -> {
                        AccountRecoverableException ace = (AccountRecoverableException) ex;
                        Instant expectedDeadline = deletedAt.plus(RECOVERY_PERIOD);
                        assertThat(ace.getRecoveryDeadline())
                                .isCloseTo(expectedDeadline, within(1, ChronoUnit.SECONDS));
                    });
        }

        @Test
        @DisplayName("[REC-003] 계정 복구 선택 시 상태 ACTIVE로 전환 및 로그인 성공")
        void recoverAccount_validCredentials_activatesAccount() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);

            // when
            AccountRecoveryResponse response = accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            // then
            assertThat(response.accessToken()).isNotNull();
            assertThat(response.refreshToken()).isNotNull();
            assertThat(response.message()).isEqualTo("계정이 성공적으로 복구되었습니다");

            // 복구 후 상태 확인
            User recoveredUser = userRepository.findByStudentIdIncludingDeleted(TEST_STUDENT_ID).orElseThrow();
            assertThat(recoveredUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(recoveredUser.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("[REC-004] 복구 후 발급된 토큰이 유효함")
        void recoverAccount_success_issuesValidTokens() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(1));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);

            // when
            AccountRecoveryResponse response = accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            // then
            assertThat(response.accessToken()).isNotNull();
            assertThat(response.refreshToken()).isNotNull();
            assertThat(response.expiresIn()).isEqualTo(ACCESS_TOKEN_VALIDITY);

            // RefreshToken이 DB에 저장되었는지 확인
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(response.refreshToken())).isPresent();
        }

        @Test
        @DisplayName("[REC-005] 복구 시 기존 역할(MEMBER) 유지")
        void recoverAccount_preservesMemberRole() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);

            // when
            AccountRecoveryResponse response = accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            // then
            assertThat(response.role()).isEqualTo(UserRole.MEMBER);

            User recoveredUser = userRepository.findByStudentIdIncludingDeleted(TEST_STUDENT_ID).orElseThrow();
            assertThat(recoveredUser.getRole()).isEqualTo(UserRole.MEMBER);
        }

        @Test
        @DisplayName("[REC-005-2] 복구 시 기존 역할(OPERATOR) 유지")
        void recoverAccount_preservesOperatorRole() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            User user = createAndSaveWithdrawnUser(UserRole.OPERATOR, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);

            // when
            AccountRecoveryResponse response = accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            // then
            assertThat(response.role()).isEqualTo(UserRole.OPERATOR);
        }

        @Test
        @DisplayName("[REC-005-3] 복구 시 기존 역할(ADMIN) 유지")
        void recoverAccount_preservesAdminRole() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            User user = createAndSaveWithdrawnUser(UserRole.ADMIN, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);

            // when
            AccountRecoveryResponse response = accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            // then
            assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        }
    }

    @Nested
    @DisplayName("복구 거부")
    class RecoveryDeclineTest {

        @Test
        @DisplayName("[REC-010] 복구 가능 여부 확인만 하고 복구하지 않으면 계정은 탈퇴 상태 유지")
        void checkRecoveryEligibility_noRecovery_accountRemainsWithdrawn() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when - 복구 가능 여부만 확인 (복구는 하지 않음)
            RecoveryEligibilityResponse response = accountRecoveryService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // then
            assertThat(response.recoverable()).isTrue();

            // 계정 상태는 여전히 WITHDRAWN
            User unchangedUser = userRepository.findByStudentIdIncludingDeleted(TEST_STUDENT_ID).orElseThrow();
            assertThat(unchangedUser.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
            assertThat(unchangedUser.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("[REC-011] 복구 거부 후 5일 이내 재시도 시 복구 가능")
        void checkRecoveryEligibility_afterDecline_canRetryWithinFiveDays() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(3));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when - 첫 번째 확인
            RecoveryEligibilityResponse firstResponse = accountRecoveryService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // when - 두 번째 확인 (재시도)
            RecoveryEligibilityResponse secondResponse = accountRecoveryService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // then
            assertThat(firstResponse.recoverable()).isTrue();
            assertThat(secondResponse.recoverable()).isTrue();
            assertThat(secondResponse.recoveryDeadline())
                    .isCloseTo(firstResponse.recoveryDeadline(), within(1, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("5일 경과 후 복구 불가")
    class RecoveryAfterFiveDaysTest {

        @Test
        @DisplayName("[REC-020] 5일 경과 후 로그인 시도 시 AccountWithdrawnException 발생")
        void login_afterFiveDays_throwsAccountWithdrawnException() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(6));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);
            PasswordLoginRequest request = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // when & then
            assertThatThrownBy(() -> passwordAuthService.login(request))
                    .isInstanceOf(AccountWithdrawnException.class);
        }

        @Test
        @DisplayName("[REC-021] 5일 경과 후 복구 시도 시 복구 불가 응답")
        void checkRecoveryEligibility_afterFiveDays_notRecoverable() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(6));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            RecoveryEligibilityResponse response = accountRecoveryService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // then
            assertThat(response.recoverable()).isFalse();
            assertThat(response.recoveryDeadline()).isNull();
            assertThat(response.message()).isEqualTo("복구 기간이 만료된 계정입니다");
        }

        @Test
        @DisplayName("[REC-022] 5일 경과 후 동일 학번 재가입 가능")
        void checkReRegistrationEligibility_afterFiveDays_eligible() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(6));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            ReRegistrationCheckResult result = accountRecoveryService.checkReRegistrationEligibility(TEST_STUDENT_ID);

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
        @DisplayName("[REC-030] 5일 이내 동일 학번 재가입 시도 시 제한 응답")
        void checkReRegistrationEligibility_withinFiveDays_restricted() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            ReRegistrationCheckResult result = accountRecoveryService.checkReRegistrationEligibility(TEST_STUDENT_ID);

            // then
            assertThat(result.isEligible()).isFalse();
            assertThat(result.isAlreadyRegistered()).isFalse();
            assertThat(result.reRegistrationAvailableAt()).isNotNull();
            assertThat(result.message()).isEqualTo("탈퇴 후 5일이 지나야 재가입할 수 있습니다");
        }

        @Test
        @DisplayName("[REC-031] 탈퇴 후 3일 경과 시 재가입 가능일 정확히 표시 (2일 후)")
        void checkReRegistrationEligibility_threeDaysAfter_showsCorrectDate() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(3));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            ReRegistrationCheckResult result = accountRecoveryService.checkReRegistrationEligibility(TEST_STUDENT_ID);

            // then
            assertThat(result.isEligible()).isFalse();
            Instant expectedAvailableAt = deletedAt.plus(RECOVERY_PERIOD);
            assertThat(result.reRegistrationAvailableAt())
                    .isCloseTo(expectedAvailableAt, within(1, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("[REC-032] 탈퇴 후 4일 23시간 경과 시 재가입 제한")
        void checkReRegistrationEligibility_almostFiveDays_stillRestricted() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(4).plusHours(23));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            ReRegistrationCheckResult result = accountRecoveryService.checkReRegistrationEligibility(TEST_STUDENT_ID);

            // then
            assertThat(result.isEligible()).isFalse();
            assertThat(result.reRegistrationAvailableAt()).isNotNull();
            assertThat(result.reRegistrationAvailableAt()).isBefore(Instant.now().plus(Duration.ofHours(2)));
        }

        @Test
        @DisplayName("[REC-033] 정확히 5일 경과 후 재가입 가능")
        void checkReRegistrationEligibility_exactlyFiveDays_eligible() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(5).plusSeconds(1));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            ReRegistrationCheckResult result = accountRecoveryService.checkReRegistrationEligibility(TEST_STUDENT_ID);

            // then
            assertThat(result.isEligible()).isTrue();
            assertThat(result.reRegistrationAvailableAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTest {

        @Test
        @DisplayName("[REC-040] 탈퇴 전 발급된 토큰은 복구 후에도 기존 토큰과 무관하게 새 토큰 발급")
        void recoverAccount_issuesNewTokens() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);

            // when
            AccountRecoveryResponse response = accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            // then
            assertThat(response.accessToken()).isNotNull();
            assertThat(response.refreshToken()).isNotNull();
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(response.refreshToken())).isPresent();
        }

        @Test
        @DisplayName("[REC-041] 복구 후 새 토큰 발급 확인")
        void recoverAccount_afterRecovery_newTokensIssued() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);

            // when
            AccountRecoveryResponse response = accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            // then
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotBlank();
            assertThat(response.userId()).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("[REC-042] 잘못된 비밀번호로 복구 시도 시 InvalidCredentialsException 발생")
        void recoverAccount_withWrongPassword_throwsInvalidCredentialsException() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);
            String wrongPassword = "wrongPassword123!";

            // when & then
            assertThatThrownBy(() -> accountRecoveryService.recoverAccount(TEST_STUDENT_ID, wrongPassword))
                    .isInstanceOf(InvalidCredentialsException.class);

            // 복구되지 않음 확인
            User unchangedUser = userRepository.findByStudentIdIncludingDeleted(TEST_STUDENT_ID).orElseThrow();
            assertThat(unchangedUser.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
            assertThat(unchangedUser.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 학번으로 복구 시도 시 InvalidCredentialsException 발생")
        void recoverAccount_nonExistentStudent_throwsInvalidCredentialsException() {
            // when & then
            assertThatThrownBy(() -> accountRecoveryService.recoverAccount("99999999", TEST_PASSWORD))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("활성 사용자에 대한 복구 시도 시 InvalidCredentialsException 발생")
        void recoverAccount_activeUser_throwsInvalidCredentialsException() {
            // given
            createAndSaveActiveUser(UserRole.MEMBER);

            // when & then
            assertThatThrownBy(() -> accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("활성 사용자 복구 여부 확인 시 notWithdrawn 응답")
        void checkRecoveryEligibility_activeUser_notWithdrawn() {
            // given
            createAndSaveActiveUser(UserRole.MEMBER);

            // when
            RecoveryEligibilityResponse response = accountRecoveryService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // then
            assertThat(response.recoverable()).isFalse();
            assertThat(response.message()).isEqualTo("탈퇴 상태가 아닌 계정입니다");
        }

        @Test
        @DisplayName("활성 사용자 재가입 가능 여부 확인 시 alreadyRegistered 응답")
        void checkReRegistrationEligibility_activeUser_alreadyRegistered() {
            // given
            createAndSaveActiveUser(UserRole.MEMBER);

            // when
            ReRegistrationCheckResult result = accountRecoveryService.checkReRegistrationEligibility(TEST_STUDENT_ID);

            // then
            assertThat(result.isEligible()).isFalse();
            assertThat(result.isAlreadyRegistered()).isTrue();
            assertThat(result.message()).isEqualTo("이미 가입된 학번입니다");
        }

        @Test
        @DisplayName("존재하지 않는 학번 재가입 가능 여부 확인 시 eligible 응답")
        void checkReRegistrationEligibility_nonExistentStudent_eligible() {
            // when
            ReRegistrationCheckResult result = accountRecoveryService.checkReRegistrationEligibility("99999999");

            // then
            assertThat(result.isEligible()).isTrue();
            assertThat(result.isAlreadyRegistered()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 학번 복구 여부 확인 시 notRecoverable 응답")
        void checkRecoveryEligibility_nonExistentStudent_notRecoverable() {
            // when
            RecoveryEligibilityResponse response = accountRecoveryService.checkRecoveryEligibility("99999999");

            // then
            assertThat(response.recoverable()).isFalse();
        }
    }
}
