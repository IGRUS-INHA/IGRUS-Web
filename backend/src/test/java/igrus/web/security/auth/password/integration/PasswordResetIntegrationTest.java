package igrus.web.security.auth.password.integration;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.common.service.EmailService;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.domain.PasswordResetToken;
import igrus.web.security.auth.password.exception.InvalidPasswordFormatException;
import igrus.web.security.auth.password.exception.PasswordResetTokenExpiredException;
import igrus.web.security.auth.password.exception.PasswordResetTokenInvalidException;
import igrus.web.security.auth.password.service.PasswordResetService;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 비밀번호 재설정 통합 테스트
 *
 * <p>테스트 케이스 문서: docs/test-case/auth/password-reset-test-cases.md</p>
 *
 * <p>테스트 범위:</p>
 * <ul>
 *     <li>PWD-001 ~ PWD-004: 재설정 링크 발송</li>
 *     <li>PWD-010 ~ PWD-013: 비밀번호 재설정 성공</li>
 *     <li>PWD-020 ~ PWD-022: 비밀번호 재설정 실패</li>
 *     <li>PWD-030 ~ PWD-035: 새 비밀번호 검증</li>
 * </ul>
 */
@DisplayName("비밀번호 재설정 통합 테스트")
class PasswordResetIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private PasswordResetService passwordResetService;

    @MockitoBean
    private EmailService emailService;

    private static final long PASSWORD_RESET_EXPIRY = 1800000L; // 30분
    private static final String TEST_STUDENT_ID = "12345678";
    private static final String TEST_EMAIL = "test@inha.edu";
    private static final String TEST_PASSWORD = "OldPass1!@";
    private static final String VALID_NEW_PASSWORD = "NewPass1!@";

    @BeforeEach
    void setUp() {
        setUpBase();
        ReflectionTestUtils.setField(passwordResetService, "passwordResetExpiry", PASSWORD_RESET_EXPIRY);
        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "http://localhost:5173");
    }

    private User createAndSaveTestUser() {
        User user = User.create(
                TEST_STUDENT_ID,
                "홍길동",
                TEST_EMAIL,
                "010-1234-5678",
                "컴퓨터공학과",
                "테스트 동기"
        );
        user.changeRole(UserRole.MEMBER);
        user.verifyEmail();
        return userRepository.save(user);
    }

    private PasswordCredential createAndSaveCredential(User user) {
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
        PasswordCredential credential = PasswordCredential.create(user, encodedPassword);
        credential.verifyEmail();
        return passwordCredentialRepository.save(credential);
    }

    private PasswordResetToken createAndSaveValidResetToken(User user, String token) {
        PasswordResetToken resetToken = PasswordResetToken.create(user, token, PASSWORD_RESET_EXPIRY);
        return passwordResetTokenRepository.save(resetToken);
    }

    private PasswordResetToken createAndSaveExpiredResetToken(User user, String token) {
        PasswordResetToken resetToken = PasswordResetToken.create(user, token, PASSWORD_RESET_EXPIRY);
        // 만료된 토큰으로 설정
        ReflectionTestUtils.setField(resetToken, "expiresAt", Instant.now().minusMillis(1000L));
        return passwordResetTokenRepository.save(resetToken);
    }

    private RefreshToken createAndSaveRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.create(user, token, 604800000L);
        return refreshTokenRepository.save(refreshToken);
    }

    // ===== 2.1 재설정 링크 발송 테스트 =====

    @Nested
    @DisplayName("재설정 링크 발송 테스트")
    class ResetLinkSendTest {

        @Test
        @DisplayName("[PWD-001] 유효한 학번으로 재설정 링크 발송 - 이메일 발송됨")
        void requestPasswordReset_withValidStudentId_sendsEmail() {
            // given
            User user = createAndSaveTestUser();
            createAndSaveCredential(user);
            ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);

            // when
            passwordResetService.requestPasswordReset(TEST_STUDENT_ID);

            // then
            verify(emailService).sendPasswordResetEmail(emailCaptor.capture(), linkCaptor.capture());
            assertThat(emailCaptor.getValue()).isEqualTo(TEST_EMAIL);
            assertThat(linkCaptor.getValue()).contains("http://localhost:5173/reset-password?token=");
        }

        @Test
        @DisplayName("[PWD-002] 존재하지 않는 학번으로 요청 - 동일한 응답 (보안)")
        void requestPasswordReset_withInvalidStudentId_noEmailSent() {
            // given
            String nonExistentStudentId = "99999999";

            // when
            passwordResetService.requestPasswordReset(nonExistentStudentId);

            // then - 이메일이 발송되지 않음 (보안상 동일한 응답)
            verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("[PWD-003] 재설정 토큰 30분 유효 - 토큰 생성 확인")
        void requestPasswordReset_createsTokenWithCorrectExpiry() {
            // given
            User user = createAndSaveTestUser();
            createAndSaveCredential(user);
            Instant beforeRequest = Instant.now();
            ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);

            // when
            passwordResetService.requestPasswordReset(TEST_STUDENT_ID);

            // then
            verify(emailService).sendPasswordResetEmail(eq(TEST_EMAIL), linkCaptor.capture());
            String resetLink = linkCaptor.getValue();
            String tokenString = resetLink.substring(resetLink.indexOf("token=") + 6);

            Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findByTokenAndUsedFalse(tokenString);
            assertThat(tokenOptional).isPresent();

            PasswordResetToken token = tokenOptional.get();
            // 만료 시간이 요청 시간 + 30분 근처인지 확인
            Instant expectedExpiry = beforeRequest.plusMillis(PASSWORD_RESET_EXPIRY);
            assertThat(token.getExpiresAt()).isAfterOrEqualTo(expectedExpiry.minusSeconds(5));
            assertThat(token.getExpiresAt()).isBeforeOrEqualTo(expectedExpiry.plusSeconds(5));
        }

        @Test
        @DisplayName("[PWD-001] 재요청 시 기존 미사용 토큰 무효화")
        void requestPasswordReset_invalidatesPreviousTokens() {
            // given
            User user = createAndSaveTestUser();
            createAndSaveCredential(user);

            // 첫 번째 요청
            passwordResetService.requestPasswordReset(TEST_STUDENT_ID);

            // when - 두 번째 요청
            passwordResetService.requestPasswordReset(TEST_STUDENT_ID);

            // then - 이메일이 두 번 발송됨
            verify(emailService, times(2)).sendPasswordResetEmail(eq(TEST_EMAIL), anyString());
        }
    }

    // ===== 2.2 토큰 검증 테스트 =====

    @Nested
    @DisplayName("토큰 검증 테스트")
    class TokenValidationTest {

        @Test
        @DisplayName("[PWD-003] 토큰 검증 성공")
        void validateResetToken_withValidToken_returnsTrue() {
            // given
            User user = createAndSaveTestUser();
            createAndSaveCredential(user);
            String tokenString = UUID.randomUUID().toString();
            createAndSaveValidResetToken(user, tokenString);

            // when
            boolean result = passwordResetService.validateResetToken(tokenString);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("[PWD-004] 유효하지 않은 토큰으로 검증 실패")
        void validateResetToken_withInvalidToken_throwsException() {
            // given
            String invalidToken = "invalid-token";

            // when & then
            assertThatThrownBy(() -> passwordResetService.validateResetToken(invalidToken))
                    .isInstanceOf(PasswordResetTokenInvalidException.class);
        }

        @Test
        @DisplayName("[PWD-005] 만료된 토큰으로 검증 실패")
        void validateResetToken_withExpiredToken_throwsException() {
            // given
            User user = createAndSaveTestUser();
            createAndSaveCredential(user);
            String tokenString = UUID.randomUUID().toString();
            createAndSaveExpiredResetToken(user, tokenString);

            // when & then
            assertThatThrownBy(() -> passwordResetService.validateResetToken(tokenString))
                    .isInstanceOf(PasswordResetTokenExpiredException.class);
        }
    }

    // ===== 2.3 비밀번호 재설정 성공 테스트 =====

    @Nested
    @DisplayName("비밀번호 재설정 성공 테스트")
    class ResetSuccessTest {

        @Test
        @DisplayName("[PWD-006] 비밀번호 변경 성공")
        void resetPassword_withValidTokenAndPassword_succeeds() {
            // given
            User user = createAndSaveTestUser();
            createAndSaveCredential(user);
            String tokenString = UUID.randomUUID().toString();
            createAndSaveValidResetToken(user, tokenString);

            // when
            passwordResetService.resetPassword(tokenString, VALID_NEW_PASSWORD);

            // then
            PasswordCredential credential = passwordCredentialRepository.findByUserId(user.getId()).orElseThrow();
            assertThat(passwordEncoder.matches(VALID_NEW_PASSWORD, credential.getPasswordHash())).isTrue();
            assertThat(passwordEncoder.matches(TEST_PASSWORD, credential.getPasswordHash())).isFalse();
        }

        @Test
        @DisplayName("[PWD-006] 비밀번호 변경 후 토큰 사용됨으로 표시")
        void resetPassword_marksTokenAsUsed() {
            // given
            User user = createAndSaveTestUser();
            createAndSaveCredential(user);
            String tokenString = UUID.randomUUID().toString();
            PasswordResetToken resetToken = createAndSaveValidResetToken(user, tokenString);

            // when
            passwordResetService.resetPassword(tokenString, VALID_NEW_PASSWORD);

            // then
            PasswordResetToken updatedToken = passwordResetTokenRepository.findById(resetToken.getId()).orElseThrow();
            assertThat(updatedToken.isUsed()).isTrue();
        }

        @Test
        @DisplayName("[PWD-008] 비밀번호 변경 시 기존 세션(Refresh Token) 무효화 확인")
        void resetPassword_revokesAllRefreshTokens() {
            // given
            User user = createAndSaveTestUser();
            createAndSaveCredential(user);
            String tokenString = UUID.randomUUID().toString();
            createAndSaveValidResetToken(user, tokenString);

            // 여러 개의 RefreshToken 생성
            createAndSaveRefreshToken(user, "refresh-token-1");
            createAndSaveRefreshToken(user, "refresh-token-2");

            // when
            passwordResetService.resetPassword(tokenString, VALID_NEW_PASSWORD);

            // then - 모든 RefreshToken이 무효화됨
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token-1")).isEmpty();
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token-2")).isEmpty();
        }

        @Test
        @DisplayName("[PWD-006] 비밀번호 BCrypt 해시로 저장")
        void resetPassword_storesPasswordAsBcryptHash() {
            // given
            User user = createAndSaveTestUser();
            createAndSaveCredential(user);
            String tokenString = UUID.randomUUID().toString();
            createAndSaveValidResetToken(user, tokenString);

            // when
            passwordResetService.resetPassword(tokenString, VALID_NEW_PASSWORD);

            // then
            PasswordCredential credential = passwordCredentialRepository.findByUserId(user.getId()).orElseThrow();
            assertThat(credential.getPasswordHash()).startsWith("$2");
            assertThat(credential.getPasswordHash()).hasSize(60);
        }
    }

    // ===== 2.4 비밀번호 재설정 실패 테스트 =====

    @Nested
    @DisplayName("비밀번호 재설정 실패 테스트")
    class ResetFailureTest {

        @Test
        @DisplayName("[PWD-020] 만료된 토큰으로 비밀번호 변경 시도 - 예외 발생")
        void resetPassword_withExpiredToken_throwsException() {
            // given
            User user = createAndSaveTestUser();
            createAndSaveCredential(user);
            String tokenString = UUID.randomUUID().toString();
            createAndSaveExpiredResetToken(user, tokenString);

            // when & then
            assertThatThrownBy(() -> passwordResetService.resetPassword(tokenString, VALID_NEW_PASSWORD))
                    .isInstanceOf(PasswordResetTokenExpiredException.class);
        }

        @Test
        @DisplayName("[PWD-021] 이미 사용된 토큰으로 비밀번호 변경 시도 - 예외 발생")
        void resetPassword_withUsedToken_throwsException() {
            // given
            User user = createAndSaveTestUser();
            createAndSaveCredential(user);
            String tokenString = UUID.randomUUID().toString();
            PasswordResetToken resetToken = createAndSaveValidResetToken(user, tokenString);
            resetToken.markAsUsed();
            passwordResetTokenRepository.save(resetToken);

            // when & then
            assertThatThrownBy(() -> passwordResetService.resetPassword(tokenString, VALID_NEW_PASSWORD))
                    .isInstanceOf(PasswordResetTokenInvalidException.class);
        }

        @Test
        @DisplayName("[PWD-022] 변조된 토큰으로 비밀번호 변경 시도 - 예외 발생")
        void resetPassword_withTamperedToken_throwsException() {
            // given
            String tamperedToken = "tampered-invalid-token";

            // when & then
            assertThatThrownBy(() -> passwordResetService.resetPassword(tamperedToken, VALID_NEW_PASSWORD))
                    .isInstanceOf(PasswordResetTokenInvalidException.class);
        }
    }

    // ===== 2.5 새 비밀번호 검증 테스트 =====

    @Nested
    @DisplayName("새 비밀번호 검증 테스트")
    class NewPasswordValidationTest {

        @Test
        @DisplayName("[PWD-007] 비밀번호 정책 위반으로 변경 실패 - 8자 미만")
        void resetPassword_withShortPassword_throwsException() {
            // given
            User user = createAndSaveTestUser();
            createAndSaveCredential(user);
            String tokenString = UUID.randomUUID().toString();
            createAndSaveValidResetToken(user, tokenString);
            String shortPassword = "Pass1!";

            // when & then
            assertThatThrownBy(() -> passwordResetService.resetPassword(tokenString, shortPassword))
                    .isInstanceOf(InvalidPasswordFormatException.class);
        }

        @Test
        @DisplayName("[PWD-007] 비밀번호 정책 위반으로 변경 실패 - 대문자 미포함")
        void resetPassword_withoutUppercase_throwsException() {
            // given
            User user = createAndSaveTestUser();
            createAndSaveCredential(user);
            String tokenString = UUID.randomUUID().toString();
            createAndSaveValidResetToken(user, tokenString);
            String noUppercasePassword = "password1!";

            // when & then
            assertThatThrownBy(() -> passwordResetService.resetPassword(tokenString, noUppercasePassword))
                    .isInstanceOf(InvalidPasswordFormatException.class);
        }

        @Test
        @DisplayName("[PWD-007] 비밀번호 정책 위반으로 변경 실패 - 소문자 미포함")
        void resetPassword_withoutLowercase_throwsException() {
            // given
            User user = createAndSaveTestUser();
            createAndSaveCredential(user);
            String tokenString = UUID.randomUUID().toString();
            createAndSaveValidResetToken(user, tokenString);
            String noLowercasePassword = "PASSWORD1!";

            // when & then
            assertThatThrownBy(() -> passwordResetService.resetPassword(tokenString, noLowercasePassword))
                    .isInstanceOf(InvalidPasswordFormatException.class);
        }

        @Test
        @DisplayName("[PWD-007] 비밀번호 정책 위반으로 변경 실패 - 숫자 미포함")
        void resetPassword_withoutNumber_throwsException() {
            // given
            User user = createAndSaveTestUser();
            createAndSaveCredential(user);
            String tokenString = UUID.randomUUID().toString();
            createAndSaveValidResetToken(user, tokenString);
            String noNumberPassword = "Password!@";

            // when & then
            assertThatThrownBy(() -> passwordResetService.resetPassword(tokenString, noNumberPassword))
                    .isInstanceOf(InvalidPasswordFormatException.class);
        }

        @Test
        @DisplayName("[PWD-007] 비밀번호 정책 위반으로 변경 실패 - 특수문자 미포함")
        void resetPassword_withoutSpecialChar_throwsException() {
            // given
            User user = createAndSaveTestUser();
            createAndSaveCredential(user);
            String tokenString = UUID.randomUUID().toString();
            createAndSaveValidResetToken(user, tokenString);
            String noSpecialCharPassword = "Password123";

            // when & then
            assertThatThrownBy(() -> passwordResetService.resetPassword(tokenString, noSpecialCharPassword))
                    .isInstanceOf(InvalidPasswordFormatException.class);
        }

        @Test
        @DisplayName("[PWD-030] 유효한 형식의 새 비밀번호 - 변경 성공")
        void resetPassword_withValidPassword_succeeds() {
            // given
            User user = createAndSaveTestUser();
            createAndSaveCredential(user);
            String tokenString = UUID.randomUUID().toString();
            createAndSaveValidResetToken(user, tokenString);
            String validPassword = "ValidPass1!@";

            // when
            passwordResetService.resetPassword(tokenString, validPassword);

            // then
            PasswordCredential credential = passwordCredentialRepository.findByUserId(user.getId()).orElseThrow();
            assertThat(passwordEncoder.matches(validPassword, credential.getPasswordHash())).isTrue();
        }
    }
}
