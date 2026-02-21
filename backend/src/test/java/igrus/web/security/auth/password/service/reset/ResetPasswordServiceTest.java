package igrus.web.security.auth.password.service.reset;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.domain.PasswordResetToken;
import igrus.web.security.auth.password.exception.InvalidPasswordFormatException;
import igrus.web.security.auth.password.exception.PasswordResetTokenExpiredException;
import igrus.web.security.auth.password.exception.PasswordResetTokenInvalidException;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ResetPasswordService 통합 테스트")
class ResetPasswordServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private ResetPasswordService resetPasswordService;

    private static final long PASSWORD_RESET_EXPIRY = 1800000L; // 30분

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Nested
    @DisplayName("비밀번호 재설정 성공 (resetPassword)")
    class ResetSuccessTest {

        @Test
        @DisplayName("30분 이내 유효한 토큰으로 비밀번호 변경 성공 [PWD-010]")
        void resetPassword_WithValidToken_ChangesPassword() {
            // given
            String newPassword = "newpass12";
            User user = createAndSaveUser("20231234", "test@inha.edu", UserRole.MEMBER);
            createAndSaveCredential(user, "oldpass12");

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.create(user, token, PASSWORD_RESET_EXPIRY);
            passwordResetTokenRepository.save(resetToken);

            // when
            resetPasswordService.resetPassword(token, newPassword);

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
            String newPassword = "newpass12";
            User user = createAndSaveUser("20231234", "test@inha.edu", UserRole.MEMBER);
            createAndSaveCredential(user, "oldpass12");

            // 여러 개의 토큰 생성
            String token1 = UUID.randomUUID().toString();
            String token2 = UUID.randomUUID().toString();
            PasswordResetToken resetToken1 = PasswordResetToken.create(user, token1, PASSWORD_RESET_EXPIRY);
            PasswordResetToken resetToken2 = PasswordResetToken.create(user, token2, PASSWORD_RESET_EXPIRY);
            passwordResetTokenRepository.save(resetToken1);
            passwordResetTokenRepository.save(resetToken2);

            // when
            resetPasswordService.resetPassword(token1, newPassword);

            // then - 모든 토큰이 무효화됨
            assertThat(passwordResetTokenRepository.findByTokenAndUsedFalse(token1)).isEmpty();
            assertThat(passwordResetTokenRepository.findByTokenAndUsedFalse(token2)).isEmpty();
        }

        @Test
        @DisplayName("비밀번호 재설정 시 모든 리프레시 토큰 무효화 (모든 세션 종료) [PWD-013]")
        void resetPassword_RevokesAllRefreshTokens() {
            // given
            String newPassword = "newpass12";
            User user = createAndSaveUser("20231234", "test@inha.edu", UserRole.MEMBER);
            createAndSaveCredential(user, "oldpass12");

            // 리프레시 토큰 생성
            RefreshToken refreshToken1 = RefreshToken.createInitial(user, "refresh-token-1", 604800000L);
            RefreshToken refreshToken2 = RefreshToken.createInitial(user, "refresh-token-2", 604800000L);
            refreshTokenRepository.save(refreshToken1);
            refreshTokenRepository.save(refreshToken2);

            String resetToken = UUID.randomUUID().toString();
            PasswordResetToken passwordResetToken = PasswordResetToken.create(user, resetToken, PASSWORD_RESET_EXPIRY);
            passwordResetTokenRepository.save(passwordResetToken);

            // when
            resetPasswordService.resetPassword(resetToken, newPassword);

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
            String newPassword = "newpass12";
            User user = createAndSaveUser("20231234", "test@inha.edu", UserRole.MEMBER);
            createAndSaveCredential(user, "oldpass12");

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.create(user, token, 1L);
            // 만료 시간을 과거로 설정
            ReflectionTestUtils.setField(resetToken, "expiresAt", Instant.now().minusSeconds(60));
            passwordResetTokenRepository.save(resetToken);

            // when & then
            assertThatThrownBy(() -> resetPasswordService.resetPassword(token, newPassword))
                    .isInstanceOf(PasswordResetTokenExpiredException.class);
        }

        @Test
        @DisplayName("이미 사용된 토큰으로 재설정 시도 시 실패 [PWD-021]")
        void resetPassword_WithUsedToken_ThrowsException() {
            // given
            String newPassword = "newpass12";
            User user = createAndSaveUser("20231234", "test@inha.edu", UserRole.MEMBER);
            createAndSaveCredential(user, "oldpass12");

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.create(user, token, PASSWORD_RESET_EXPIRY);
            resetToken.markAsUsed();
            passwordResetTokenRepository.save(resetToken);

            // when & then
            assertThatThrownBy(() -> resetPasswordService.resetPassword(token, newPassword))
                    .isInstanceOf(PasswordResetTokenInvalidException.class);
        }

        @Test
        @DisplayName("변조된(존재하지 않는) 토큰으로 재설정 시도 시 실패 [PWD-022]")
        void resetPassword_WithInvalidToken_ThrowsException() {
            // given
            String token = "invalid-token-that-does-not-exist";
            String newPassword = "newpass12";

            // when & then
            assertThatThrownBy(() -> resetPasswordService.resetPassword(token, newPassword))
                    .isInstanceOf(PasswordResetTokenInvalidException.class);
        }

        @Test
        @DisplayName("비밀번호 형식 오류 시 resetPassword 실패")
        void resetPassword_WithInvalidPasswordFormat_ThrowsException() {
            // given
            User user = createAndSaveUser("20231234", "test@inha.edu", UserRole.MEMBER);
            createAndSaveCredential(user, "oldpass12");

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.create(user, token, PASSWORD_RESET_EXPIRY);
            passwordResetTokenRepository.save(resetToken);

            String invalidPassword = "weak"; // 형식 미준수

            // when & then
            assertThatThrownBy(() -> resetPasswordService.resetPassword(token, invalidPassword))
                    .isInstanceOf(InvalidPasswordFormatException.class);
        }
    }
}
