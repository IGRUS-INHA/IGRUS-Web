package igrus.web.security.auth.password.service.reset;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.password.domain.PasswordResetToken;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("RequestPasswordResetService 통합 테스트")
class RequestPasswordResetServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private RequestPasswordResetService requestPasswordResetService;

    @MockitoBean
    private AuthEmailService authEmailService;

    private static final long PASSWORD_RESET_EXPIRY = 1800000L; // 30분

    @BeforeEach
    void setUp() {
        setUpBase();
        ReflectionTestUtils.setField(requestPasswordResetService, "passwordResetExpiry", PASSWORD_RESET_EXPIRY);
        ReflectionTestUtils.setField(requestPasswordResetService, "frontendUrl", "http://localhost:5173");
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
    @DisplayName("유효한 학번으로 재설정 링크 발송 성공 [PWD-001]")
    void requestPasswordReset_WithValidStudentId_SendsEmail() {
        // given
        String studentId = "20231234";
        User user = createAndSaveTestUser(studentId, "test@inha.edu");

        // when
        requestPasswordResetService.requestPasswordReset(studentId);

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
        requestPasswordResetService.requestPasswordReset(studentId);

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
        requestPasswordResetService.requestPasswordReset(studentId);

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
        requestPasswordResetService.requestPasswordReset(studentId);

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
        requestPasswordResetService.requestPasswordReset(studentId);

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
