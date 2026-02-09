package igrus.web.user.mypage.service.write;

import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.exception.verification.VerificationAttemptsExceededException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeExpiredException;
import igrus.web.security.auth.common.exception.verification.VerificationCodeInvalidException;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.security.auth.common.service.EmailVerificationAttemptService;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static igrus.web.common.fixture.UserTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * VerifyEmailChangeService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>성공: 인증 코드 확인 후 이메일 변경 성공</li>
 *     <li>실패: 사용자 미존재</li>
 *     <li>실패: 미인증 레코드 미존재</li>
 *     <li>실패: 인증 코드 만료</li>
 *     <li>실패: 시도 횟수 초과</li>
 *     <li>실패: 인증 코드 불일치</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VerifyEmailChangeService 단위 테스트")
class VerifyEmailChangeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private EmailVerificationAttemptService emailVerificationAttemptService;

    @InjectMocks
    private VerifyEmailChangeService verifyEmailChangeService;

    private User memberUser;

    @BeforeEach
    void setUp() {
        memberUser = createMemberWithId();
    }

    @Nested
    @DisplayName("이메일 변경 인증 성공")
    class SuccessTest {

        @DisplayName("인증 코드 확인 후 이메일이 변경된다")
        @Test
        void verifyAndChangeEmail_WithValidCode_ChangesEmail() {
            // given
            Long userId = memberUser.getId();
            String newEmail = "newemail@example.com";
            String code = "123456";
            EmailVerificationRequest request = new EmailVerificationRequest(newEmail, code);

            EmailVerification verification = mock(EmailVerification.class);
            given(verification.isExpired()).willReturn(false);
            given(verification.canAttempt(0)).willReturn(true);
            given(verification.getCode()).willReturn("123456");

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(emailVerificationRepository.findByEmailAndVerifiedFalse(newEmail))
                    .willReturn(Optional.of(verification));

            // when
            verifyEmailChangeService.verifyAndChangeEmail(userId, request);

            // then
            verify(verification).verify();
            assertThat(memberUser.getEmail()).isEqualTo(newEmail);
        }
    }

    @Nested
    @DisplayName("이메일 변경 인증 실패")
    class FailureTest {

        @DisplayName("존재하지 않는 사용자이면 UserNotFoundException 발생")
        @Test
        void verifyAndChangeEmail_WhenUserNotFound_ThrowsUserNotFoundException() {
            // given
            Long nonExistentUserId = 999L;
            EmailVerificationRequest request = new EmailVerificationRequest("new@example.com", "123456");

            given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> verifyEmailChangeService.verifyAndChangeEmail(nonExistentUserId, request))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @DisplayName("미인증 레코드가 없으면 VerificationCodeInvalidException 발생")
        @Test
        void verifyAndChangeEmail_WhenVerificationNotFound_ThrowsVerificationCodeInvalidException() {
            // given
            Long userId = memberUser.getId();
            EmailVerificationRequest request = new EmailVerificationRequest("new@example.com", "123456");

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(emailVerificationRepository.findByEmailAndVerifiedFalse("new@example.com"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> verifyEmailChangeService.verifyAndChangeEmail(userId, request))
                    .isInstanceOf(VerificationCodeInvalidException.class);
        }

        @DisplayName("인증 코드가 만료되면 VerificationCodeExpiredException 발생")
        @Test
        void verifyAndChangeEmail_WhenCodeExpired_ThrowsVerificationCodeExpiredException() {
            // given
            Long userId = memberUser.getId();
            EmailVerificationRequest request = new EmailVerificationRequest("new@example.com", "123456");

            EmailVerification verification = mock(EmailVerification.class);
            given(verification.isExpired()).willReturn(true);

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(emailVerificationRepository.findByEmailAndVerifiedFalse("new@example.com"))
                    .willReturn(Optional.of(verification));

            // when & then
            assertThatThrownBy(() -> verifyEmailChangeService.verifyAndChangeEmail(userId, request))
                    .isInstanceOf(VerificationCodeExpiredException.class);
        }

        @DisplayName("시도 횟수 초과 시 VerificationAttemptsExceededException 발생")
        @Test
        void verifyAndChangeEmail_WhenAttemptsExceeded_ThrowsVerificationAttemptsExceededException() {
            // given
            Long userId = memberUser.getId();
            EmailVerificationRequest request = new EmailVerificationRequest("new@example.com", "123456");

            EmailVerification verification = mock(EmailVerification.class);
            given(verification.isExpired()).willReturn(false);
            given(verification.canAttempt(0)).willReturn(false);

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(emailVerificationRepository.findByEmailAndVerifiedFalse("new@example.com"))
                    .willReturn(Optional.of(verification));

            // when & then
            assertThatThrownBy(() -> verifyEmailChangeService.verifyAndChangeEmail(userId, request))
                    .isInstanceOf(VerificationAttemptsExceededException.class);
        }

        @DisplayName("인증 코드 불일치 시 VerificationCodeInvalidException 발생")
        @Test
        void verifyAndChangeEmail_WithWrongCode_ThrowsVerificationCodeInvalidException() {
            // given
            Long userId = memberUser.getId();
            EmailVerificationRequest request = new EmailVerificationRequest("new@example.com", "999999");

            EmailVerification verification = mock(EmailVerification.class);
            given(verification.getId()).willReturn(1L);
            given(verification.isExpired()).willReturn(false);
            given(verification.canAttempt(0)).willReturn(true);
            given(verification.getCode()).willReturn("123456");

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(emailVerificationRepository.findByEmailAndVerifiedFalse("new@example.com"))
                    .willReturn(Optional.of(verification));

            // when & then
            assertThatThrownBy(() -> verifyEmailChangeService.verifyAndChangeEmail(userId, request))
                    .isInstanceOf(VerificationCodeInvalidException.class);

            verify(emailVerificationAttemptService).incrementAttempts(1L);
        }
    }
}
