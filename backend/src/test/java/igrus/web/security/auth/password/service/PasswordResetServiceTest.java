package igrus.web.security.auth.password.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.domain.PasswordResetToken;
import igrus.web.security.auth.password.exception.InvalidPasswordFormatException;
import igrus.web.security.auth.password.exception.PasswordResetTokenExpiredException;
import igrus.web.security.auth.password.exception.PasswordResetTokenInvalidException;
import igrus.web.user.domain.Gender;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("PasswordResetService 통합 테스트")
class PasswordResetServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private PasswordResetService passwordResetService;

    @MockitoBean
    private AuthEmailService authEmailService;

    private static final long PASSWORD_RESET_EXPIRY = 1800000L; // 30분

    @BeforeEach
    void setUp() {
        setUpBase();
        ReflectionTestUtils.setField(passwordResetService, "passwordResetExpiry", PASSWORD_RESET_EXPIRY);
        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "http://localhost:5173");
    }

    private User createAndSaveTestUser(String studentId, String email) {
        User user = User.create(
                studentId,
                "홍길동",
                email,
                "010-1234-5678",
                "컴퓨터공학과",
                "테스트 동기",
                Gender.MALE,
                1
        );
        user.changeRole(UserRole.MEMBER);
        return userRepository.save(user);
    }

    private PasswordCredential createAndSaveCredential(User user) {
        String encodedPassword = passwordEncoder.encode("OldPassword1!");
        PasswordCredential credential = PasswordCredential.create(user, encodedPassword);
        return passwordCredentialRepository.save(credential);
    }

    @Nested
    @DisplayName("재설정 링크 발송 (requestPasswordReset)")
    class ResetLinkSendTest {

        @Test
        @DisplayName("유효한 학번으로 재설정 링크 발송 성공 [PWD-001]")
        void requestPasswordReset_WithValidStudentId_SendsEmail() {
            // given
            String studentId = "20231234";
            User user = createAndSaveTestUser(studentId, "test@inha.edu");

            // when
            passwordResetService.requestPasswordReset(studentId);

            // then - 토큰이 DB에 저장되었는지 확인
            List<PasswordResetToken> tokens = passwordResetTokenRepository.findAll();
            assertThat(tokens).hasSize(1);
            assertThat(tokens.get(0).getUser().getId()).isEqualTo(user.getId());
            assertThat(tokens.get(0).isUsed()).isFalse();

            // 외부 의존성 상호작용 검증
            verify(authEmailService).sendPasswordResetEmail(eq(user.getEmail()), anyString());
        }

        @Test
        @DisplayName("존재하지 않는 학번으로 요청 시 이메일 발송하지 않음 (보안상 동일 응답) [PWD-002]")
        void requestPasswordReset_WithNonExistentStudentId_DoesNotSendEmail() {
            // given
            String studentId = "99999999";

            // when
            passwordResetService.requestPasswordReset(studentId);

            // then - 토큰이 저장되지 않음
            List<PasswordResetToken> tokens = passwordResetTokenRepository.findAll();
            assertThat(tokens).isEmpty();

            verify(authEmailService, never()).sendPasswordResetEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("재설정 링크는 30분 유효 토큰으로 생성 [PWD-003]")
        void requestPasswordReset_CreatesTokenWith30MinuteExpiry() {
            // given
            String studentId = "20231234";
            createAndSaveTestUser(studentId, "test@inha.edu");

            // when
            passwordResetService.requestPasswordReset(studentId);

            // then
            List<PasswordResetToken> tokens = passwordResetTokenRepository.findAll();
            assertThat(tokens).hasSize(1);

            PasswordResetToken savedToken = tokens.get(0);
            assertThat(savedToken.isUsed()).isFalse();

            // 만료 시간이 약 30분 후인지 확인
            Instant expectedExpiry = Instant.now().plusMillis(PASSWORD_RESET_EXPIRY);
            assertThat(savedToken.getExpiresAt())
                    .isBetween(expectedExpiry.minusSeconds(5), expectedExpiry.plusSeconds(5));
        }

        @Test
        @DisplayName("재설정 요청 시 이메일 즉시 발송 호출 [PWD-004]")
        void requestPasswordReset_CallsEmailServiceImmediately() {
            // given
            String studentId = "20231234";
            User user = createAndSaveTestUser(studentId, "test@inha.edu");

            ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);

            // when
            passwordResetService.requestPasswordReset(studentId);

            // then
            verify(authEmailService).sendPasswordResetEmail(eq(user.getEmail()), linkCaptor.capture());

            String resetLink = linkCaptor.getValue();
            assertThat(resetLink).startsWith("http://localhost:5173/reset-password?token=");
        }

        @Test
        @DisplayName("재설정 요청 시 기존 토큰 무효화 후 새 토큰 생성")
        void requestPasswordReset_InvalidatesOldTokensAndCreatesNew() {
            // given
            String studentId = "20231234";
            User user = createAndSaveTestUser(studentId, "test@inha.edu");

            // 기존 토큰 생성
            String oldToken = UUID.randomUUID().toString();
            PasswordResetToken existingToken = PasswordResetToken.create(user, oldToken, PASSWORD_RESET_EXPIRY);
            passwordResetTokenRepository.save(existingToken);

            // when
            passwordResetService.requestPasswordReset(studentId);

            // then - 기존 토큰은 무효화됨
            Optional<PasswordResetToken> oldTokenFromDb = passwordResetTokenRepository.findByTokenAndUsedFalse(oldToken);
            assertThat(oldTokenFromDb).isEmpty();

            // 새 토큰이 생성됨 (유효한 것만 조회하면 1개)
            List<PasswordResetToken> allTokens = passwordResetTokenRepository.findAll();
            long validTokenCount = allTokens.stream()
                    .filter(t -> !t.isUsed())
                    .count();
            assertThat(validTokenCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("비밀번호 재설정 성공 (resetPassword)")
    class ResetSuccessTest {

        @Test
        @DisplayName("30분 이내 유효한 토큰으로 비밀번호 변경 성공 [PWD-010]")
        void resetPassword_WithValidToken_ChangesPassword() {
            // given
            String newPassword = "NewPassword1!";
            User user = createAndSaveTestUser("20231234", "test@inha.edu");
            createAndSaveCredential(user);

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.create(user, token, PASSWORD_RESET_EXPIRY);
            passwordResetTokenRepository.save(resetToken);

            // when
            passwordResetService.resetPassword(token, newPassword);

            // then
            PasswordCredential credential = passwordCredentialRepository.findByUserId(user.getId()).orElseThrow();
            assertThat(passwordEncoder.matches(newPassword, credential.getPasswordHash())).isTrue();

            // 토큰이 사용됨으로 표시됨
            PasswordResetToken usedToken = passwordResetTokenRepository.findById(resetToken.getId()).orElseThrow();
            assertThat(usedToken.isUsed()).isTrue();
        }

        @Test
        @DisplayName("비밀번호 재설정 시 모든 기존 비밀번호 재설정 토큰 무효화 [PWD-013]")
        void resetPassword_InvalidatesAllPasswordResetTokens() {
            // given
            String newPassword = "NewPassword1!";
            User user = createAndSaveTestUser("20231234", "test@inha.edu");
            createAndSaveCredential(user);

            // 여러 개의 토큰 생성
            String token1 = UUID.randomUUID().toString();
            String token2 = UUID.randomUUID().toString();
            PasswordResetToken resetToken1 = PasswordResetToken.create(user, token1, PASSWORD_RESET_EXPIRY);
            PasswordResetToken resetToken2 = PasswordResetToken.create(user, token2, PASSWORD_RESET_EXPIRY);
            passwordResetTokenRepository.save(resetToken1);
            passwordResetTokenRepository.save(resetToken2);

            // when
            passwordResetService.resetPassword(token1, newPassword);

            // then - 모든 토큰이 무효화됨
            assertThat(passwordResetTokenRepository.findByTokenAndUsedFalse(token1)).isEmpty();
            assertThat(passwordResetTokenRepository.findByTokenAndUsedFalse(token2)).isEmpty();
        }

        @Test
        @DisplayName("비밀번호 재설정 시 모든 리프레시 토큰 무효화 (모든 세션 종료) [PWD-013]")
        void resetPassword_RevokesAllRefreshTokens() {
            // given
            String newPassword = "NewPassword1!";
            User user = createAndSaveTestUser("20231234", "test@inha.edu");
            createAndSaveCredential(user);

            // 리프레시 토큰 생성
            RefreshToken refreshToken1 = RefreshToken.create(user, "refresh-token-1", 604800000L);
            RefreshToken refreshToken2 = RefreshToken.create(user, "refresh-token-2", 604800000L);
            refreshTokenRepository.save(refreshToken1);
            refreshTokenRepository.save(refreshToken2);

            String resetToken = UUID.randomUUID().toString();
            PasswordResetToken passwordResetToken = PasswordResetToken.create(user, resetToken, PASSWORD_RESET_EXPIRY);
            passwordResetTokenRepository.save(passwordResetToken);

            // when
            passwordResetService.resetPassword(resetToken, newPassword);

            // then - 모든 리프레시 토큰이 무효화됨
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token-1")).isEmpty();
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse("refresh-token-2")).isEmpty();
        }
    }

    @Nested
    @DisplayName("비밀번호 재설정 실패 (resetPassword)")
    class ResetFailureTest {

        @Test
        @DisplayName("30분 경과 후 만료된 토큰으로 재설정 시도 시 실패 [PWD-020]")
        void resetPassword_WithExpiredToken_ThrowsException() {
            // given
            String newPassword = "NewPassword1!";
            User user = createAndSaveTestUser("20231234", "test@inha.edu");
            createAndSaveCredential(user);

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.create(user, token, 1L);
            // 만료 시간을 과거로 설정
            ReflectionTestUtils.setField(resetToken, "expiresAt", Instant.now().minusSeconds(60));
            passwordResetTokenRepository.save(resetToken);

            // when & then
            assertThatThrownBy(() -> passwordResetService.resetPassword(token, newPassword))
                    .isInstanceOf(PasswordResetTokenExpiredException.class);
        }

        @Test
        @DisplayName("이미 사용된 토큰으로 재설정 시도 시 실패 [PWD-021]")
        void resetPassword_WithUsedToken_ThrowsException() {
            // given
            String newPassword = "NewPassword1!";
            User user = createAndSaveTestUser("20231234", "test@inha.edu");
            createAndSaveCredential(user);

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.create(user, token, PASSWORD_RESET_EXPIRY);
            resetToken.markAsUsed();
            passwordResetTokenRepository.save(resetToken);

            // when & then
            assertThatThrownBy(() -> passwordResetService.resetPassword(token, newPassword))
                    .isInstanceOf(PasswordResetTokenInvalidException.class);
        }

        @Test
        @DisplayName("변조된(존재하지 않는) 토큰으로 재설정 시도 시 실패 [PWD-022]")
        void resetPassword_WithInvalidToken_ThrowsException() {
            // given
            String token = "invalid-token-that-does-not-exist";
            String newPassword = "NewPassword1!";

            // when & then
            assertThatThrownBy(() -> passwordResetService.resetPassword(token, newPassword))
                    .isInstanceOf(PasswordResetTokenInvalidException.class);
        }
    }

    @Nested
    @DisplayName("새 비밀번호 검증 (validatePasswordFormat)")
    class NewPasswordValidationTest {

        @Test
        @DisplayName("유효한 형식의 새 비밀번호 검증 통과 [PWD-030]")
        void validatePasswordFormat_WithValidPassword_Passes() {
            // given
            String validPassword = "ValidPass1!";

            // when & then (예외가 발생하지 않으면 성공)
            passwordResetService.validatePasswordFormat(validPassword);
        }

        @Test
        @DisplayName("8자 미만 비밀번호 검증 실패 [PWD-031]")
        void validatePasswordFormat_WithShortPassword_ThrowsException() {
            // given
            String shortPassword = "Pass1!"; // 7자 미만

            // when & then
            assertThatThrownBy(() -> passwordResetService.validatePasswordFormat(shortPassword))
                    .isInstanceOf(InvalidPasswordFormatException.class);
        }

        @Test
        @DisplayName("대문자 미포함 비밀번호 검증 실패 [PWD-032]")
        void validatePasswordFormat_WithoutUppercase_ThrowsException() {
            // given
            String noUppercase = "password1!"; // 대문자 없음

            // when & then
            assertThatThrownBy(() -> passwordResetService.validatePasswordFormat(noUppercase))
                    .isInstanceOf(InvalidPasswordFormatException.class);
        }

        @Test
        @DisplayName("소문자 미포함 비밀번호 검증 실패 [PWD-033]")
        void validatePasswordFormat_WithoutLowercase_ThrowsException() {
            // given
            String noLowercase = "PASSWORD1!"; // 소문자 없음

            // when & then
            assertThatThrownBy(() -> passwordResetService.validatePasswordFormat(noLowercase))
                    .isInstanceOf(InvalidPasswordFormatException.class);
        }

        @Test
        @DisplayName("숫자 미포함 비밀번호 검증 실패 [PWD-034]")
        void validatePasswordFormat_WithoutDigit_ThrowsException() {
            // given
            String noDigit = "Password!@"; // 숫자 없음

            // when & then
            assertThatThrownBy(() -> passwordResetService.validatePasswordFormat(noDigit))
                    .isInstanceOf(InvalidPasswordFormatException.class);
        }

        @Test
        @DisplayName("특수문자 미포함 비밀번호 검증 실패 [PWD-035]")
        void validatePasswordFormat_WithoutSpecialChar_ThrowsException() {
            // given
            String noSpecialChar = "Password123"; // 특수문자 없음

            // when & then
            assertThatThrownBy(() -> passwordResetService.validatePasswordFormat(noSpecialChar))
                    .isInstanceOf(InvalidPasswordFormatException.class);
        }

        @Test
        @DisplayName("null 비밀번호 검증 실패")
        void validatePasswordFormat_WithNull_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> passwordResetService.validatePasswordFormat(null))
                    .isInstanceOf(InvalidPasswordFormatException.class);
        }

        @Test
        @DisplayName("비밀번호 형식 오류 시 resetPassword 실패")
        void resetPassword_WithInvalidPasswordFormat_ThrowsException() {
            // given
            User user = createAndSaveTestUser("20231234", "test@inha.edu");
            createAndSaveCredential(user);

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.create(user, token, PASSWORD_RESET_EXPIRY);
            passwordResetTokenRepository.save(resetToken);

            String invalidPassword = "weak"; // 형식 미준수

            // when & then
            assertThatThrownBy(() -> passwordResetService.resetPassword(token, invalidPassword))
                    .isInstanceOf(InvalidPasswordFormatException.class);
        }
    }

    @Nested
    @DisplayName("토큰 검증 (validateResetToken)")
    class ValidateResetTokenTest {

        @Test
        @DisplayName("유효한 토큰 검증 시 true 반환")
        void validateResetToken_WithValidToken_ReturnsTrue() {
            // given
            User user = createAndSaveTestUser("20231234", "test@inha.edu");

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.create(user, token, PASSWORD_RESET_EXPIRY);
            passwordResetTokenRepository.save(resetToken);

            // when
            boolean result = passwordResetService.validateResetToken(token);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 토큰 검증 시 예외 발생")
        void validateResetToken_WithNonExistentToken_ThrowsException() {
            // given
            String token = "non-existent-token";

            // when & then
            assertThatThrownBy(() -> passwordResetService.validateResetToken(token))
                    .isInstanceOf(PasswordResetTokenInvalidException.class);
        }

        @Test
        @DisplayName("만료된 토큰 검증 시 예외 발생")
        void validateResetToken_WithExpiredToken_ThrowsException() {
            // given
            User user = createAndSaveTestUser("20231234", "test@inha.edu");

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.create(user, token, 1L);
            // 만료 시간을 과거로 설정
            ReflectionTestUtils.setField(resetToken, "expiresAt", Instant.now().minusSeconds(60));
            passwordResetTokenRepository.save(resetToken);

            // when & then
            assertThatThrownBy(() -> passwordResetService.validateResetToken(token))
                    .isInstanceOf(PasswordResetTokenExpiredException.class);
        }
    }
}
