package igrus.web.security.auth.common.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.dto.internal.RecoveryResult;
import igrus.web.security.auth.common.dto.response.RecoveryEligibilityResponse;
import igrus.web.security.auth.common.exception.account.AccountNotRecoverableException;
import igrus.web.security.auth.common.service.AccountRecoveryService.ReRegistrationCheckResult;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.security.jwt.JwtTokenProvider;
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

@DisplayName("AccountRecoveryService 통합 테스트")
class AccountRecoveryServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private AccountRecoveryService accountRecoveryService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final long ACCESS_TOKEN_VALIDITY = 3600000L; // 1시간
    private static final long REFRESH_TOKEN_VALIDITY = 604800000L; // 7일
    private static final String TEST_STUDENT_ID = "12345678";
    private static final String TEST_PASSWORD = "password123!";
    private static final Duration RECOVERY_PERIOD = Duration.ofDays(5);

    @BeforeEach
    void setUp() {
        setUpBase();
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
        User savedUser = userRepository.save(user);

        // Withdraw the user
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
                "테스트 동기"
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
        @DisplayName("탈퇴 직후 복구 시도 시 안내 메시지 표시 [REC-001]")
        void checkRecoveryEligibility_immediatelyAfterWithdrawal_showsRecoveryMessage() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofMinutes(1));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            RecoveryEligibilityResponse response = accountRecoveryService.checkRecoveryEligibility(TEST_STUDENT_ID);

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
            RecoveryEligibilityResponse response = accountRecoveryService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // then
            assertThat(response.recoverable()).isTrue();
            assertThat(response.recoveryDeadline()).isNotNull();
            Instant expectedDeadline = deletedAt.plus(RECOVERY_PERIOD);
            assertThat(response.recoveryDeadline()).isCloseTo(expectedDeadline, within(1, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("계정 복구 선택 시 상태 전환 - ACTIVE로 변경 및 로그인 성공 [REC-003]")
        void recoverAccount_validCredentials_activatesAccountAndReturnsTokens() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);

            // when
            RecoveryResult response = accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isNotNull();
            assertThat(response.refreshToken()).isNotNull();

            // User 상태 확인 - DB에서 다시 조회
            User recoveredUser = userRepository.findByStudentIdIncludingDeleted(TEST_STUDENT_ID).orElseThrow();
            assertThat(recoveredUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(recoveredUser.isDeleted()).isFalse();

            // PasswordCredential 상태 확인
            PasswordCredential recoveredCredential = passwordCredentialRepository.findByUserIdIncludingDeleted(user.getId()).orElseThrow();
            assertThat(recoveredCredential.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(recoveredCredential.isDeleted()).isFalse();

            // RefreshToken이 저장되었는지 확인
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
            RecoveryResult response = accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

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
            RecoveryResult response = accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            // then
            assertThat(response.role()).isEqualTo(UserRole.MEMBER);

            User recoveredUser = userRepository.findByStudentIdIncludingDeleted(TEST_STUDENT_ID).orElseThrow();
            assertThat(recoveredUser.getRole()).isEqualTo(UserRole.MEMBER);
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
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when - 복구 가능 여부만 확인 (실제 복구는 하지 않음)
            RecoveryEligibilityResponse response = accountRecoveryService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // then
            assertThat(response.recoverable()).isTrue();

            // User 상태는 여전히 WITHDRAWN
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

            // when - 첫 번째 확인
            RecoveryEligibilityResponse firstResponse = accountRecoveryService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // when - 두 번째 확인 (재시도)
            RecoveryEligibilityResponse secondResponse = accountRecoveryService.checkRecoveryEligibility(TEST_STUDENT_ID);

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
            RecoveryEligibilityResponse response = accountRecoveryService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // then
            assertThat(response.recoverable()).isFalse();
            assertThat(response.recoveryDeadline()).isNull();
            assertThat(response.message()).isEqualTo("복구 기간이 만료된 계정입니다");
        }

        @Test
        @DisplayName("5일 경과 후 복구 시도 - AccountNotRecoverableException 발생 [REC-021]")
        void recoverAccount_afterFiveDays_throwsAccountNotRecoverableException() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(6));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);

            // when & then
            assertThatThrownBy(() -> accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD))
                    .isInstanceOf(AccountNotRecoverableException.class);
        }

        @Test
        @DisplayName("5일 경과 후 동일 학번 재가입 가능 - 개인정보 파기 후 상태 [REC-022]")
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
        @DisplayName("5일 이내 동일 학번 재가입 시도 - 제한 메시지 및 재가입 가능일 표시 [REC-030]")
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
        @DisplayName("재가입 가능일 정확히 표시 - 3일 경과 시 2일 후 재가입 가능 [REC-031]")
        void checkReRegistrationEligibility_threeDaysAfter_showsCorrectDate() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(3));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            ReRegistrationCheckResult result = accountRecoveryService.checkReRegistrationEligibility(TEST_STUDENT_ID);

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
            ReRegistrationCheckResult result = accountRecoveryService.checkReRegistrationEligibility(TEST_STUDENT_ID);

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
        @DisplayName("탈퇴 전 활성화된 토큰은 복구와 무관 - 새 토큰 발급 [REC-040]")
        void recoverAccount_issuesNewTokensRegardlessOfPreviousTokens() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            User user = createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            createAndSaveWithdrawnCredential(user, deletedAt);

            // when
            RecoveryResult response = accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

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
            assertThatThrownBy(() -> accountRecoveryService.recoverAccount(TEST_STUDENT_ID, wrongPassword))
                    .isInstanceOf(InvalidCredentialsException.class);

            // 복구 진행되지 않음
            User unchangedUser = userRepository.findByStudentIdIncludingDeleted(TEST_STUDENT_ID).orElseThrow();
            assertThat(unchangedUser.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
            assertThat(unchangedUser.isDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("추가 Edge Cases")
    class AdditionalEdgeCasesTest {

        @Test
        @DisplayName("존재하지 않는 사용자 복구 시도 - InvalidCredentialsException 발생")
        void recoverAccount_nonExistentUser_throwsInvalidCredentialsException() {
            // when & then
            assertThatThrownBy(() -> accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("활성 사용자 복구 시도 - InvalidCredentialsException 발생")
        void recoverAccount_activeUser_throwsInvalidCredentialsException() {
            // given
            createAndSaveActiveUser(UserRole.MEMBER);

            // when & then
            assertThatThrownBy(() -> accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("활성 사용자 복구 여부 확인 - notWithdrawn 응답")
        void checkRecoveryEligibility_activeUser_returnsNotWithdrawn() {
            // given
            createAndSaveActiveUser(UserRole.MEMBER);

            // when
            RecoveryEligibilityResponse response = accountRecoveryService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // then
            assertThat(response.recoverable()).isFalse();
            assertThat(response.message()).isEqualTo("탈퇴 상태가 아닌 계정입니다");
        }

        @Test
        @DisplayName("비밀번호 정보 없는 탈퇴 사용자 복구 시도 - InvalidCredentialsException 발생")
        void recoverAccount_noPasswordCredential_throwsInvalidCredentialsException() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);
            // PasswordCredential 없음

            // when & then
            assertThatThrownBy(() -> accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 복구 여부 확인 - notRecoverable 응답")
        void checkRecoveryEligibility_nonExistentUser_returnsNotRecoverable() {
            // when
            RecoveryEligibilityResponse response = accountRecoveryService.checkRecoveryEligibility(TEST_STUDENT_ID);

            // then
            assertThat(response.recoverable()).isFalse();
        }

        @Test
        @DisplayName("복구 기한 조회 - 탈퇴 사용자")
        void getRecoveryDeadline_withdrawnUser_returnsDeadline() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(2));
            createAndSaveWithdrawnUser(UserRole.MEMBER, deletedAt);

            // when
            Instant deadline = accountRecoveryService.getRecoveryDeadline(TEST_STUDENT_ID);

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
            assertThatThrownBy(() -> accountRecoveryService.getRecoveryDeadline(TEST_STUDENT_ID))
                    .isInstanceOf(AccountNotRecoverableException.class);
        }

        @Test
        @DisplayName("복구 기한 조회 - 존재하지 않는 사용자 - InvalidCredentialsException 발생")
        void getRecoveryDeadline_nonExistentUser_throwsInvalidCredentialsException() {
            // when & then
            assertThatThrownBy(() -> accountRecoveryService.getRecoveryDeadline(TEST_STUDENT_ID))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("재가입 가능 여부 확인 - 존재하지 않는 사용자 - eligible 응답")
        void checkReRegistrationEligibility_nonExistentUser_eligible() {
            // when
            ReRegistrationCheckResult result = accountRecoveryService.checkReRegistrationEligibility(TEST_STUDENT_ID);

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
            ReRegistrationCheckResult result = accountRecoveryService.checkReRegistrationEligibility(TEST_STUDENT_ID);

            // then
            assertThat(result.isEligible()).isFalse();
            assertThat(result.isAlreadyRegistered()).isTrue();
            assertThat(result.message()).isEqualTo("이미 가입된 학번입니다");
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
                    "테스트 동기"
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
            RecoveryResult response = accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

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
                    "테스트 동기"
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
            RecoveryResult response = accountRecoveryService.recoverAccount(TEST_STUDENT_ID, TEST_PASSWORD);

            // then
            assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        }
    }
}
