package igrus.web.security.auth.password.service.reset;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.password.domain.PasswordResetToken;
import igrus.web.security.auth.password.exception.PasswordResetTokenExpiredException;
import igrus.web.security.auth.password.exception.PasswordResetTokenInvalidException;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ValidateResetTokenService 통합 테스트")
class ValidateResetTokenServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private ValidateResetTokenService validateResetTokenService;

    @MockitoBean
    private AuthEmailService authEmailService;

    private static final long PASSWORD_RESET_EXPIRY = 1800000L; // 30분

    @BeforeEach
    void setUp() {
        setUpBase();
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

    @Test
    @DisplayName("유효한 토큰 검증 시 true 반환")
    void validateResetToken_WithValidToken_ReturnsTrue() {
        // given
        User user = createAndSaveTestUser("20231234", "test@inha.edu");

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.create(user, token, PASSWORD_RESET_EXPIRY);
        passwordResetTokenRepository.save(resetToken);

        // when
        boolean result = validateResetTokenService.validateResetToken(token);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 토큰 검증 시 예외 발생")
    void validateResetToken_WithNonExistentToken_ThrowsException() {
        // given
        String token = "non-existent-token";

        // when & then
        assertThatThrownBy(() -> validateResetTokenService.validateResetToken(token))
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
        assertThatThrownBy(() -> validateResetTokenService.validateResetToken(token))
                .isInstanceOf(PasswordResetTokenExpiredException.class);
    }
}
