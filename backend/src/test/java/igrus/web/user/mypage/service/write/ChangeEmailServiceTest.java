package igrus.web.user.mypage.service.write;

import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.security.auth.password.service.support.VerificationCodeGenerator;
import igrus.web.user.domain.User;
import igrus.web.user.exception.DuplicateEmailException;
import igrus.web.user.exception.SameEmailException;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.mypage.dto.request.ChangeEmailRequest;
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static igrus.web.common.fixture.UserTestFixture.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * ChangeEmailService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>성공: 비밀번호 확인 후 인증 코드 발송 성공</li>
 *     <li>실패: 사용자 미존재</li>
 *     <li>실패: PasswordCredential 미존재</li>
 *     <li>실패: 비밀번호 불일치</li>
 *     <li>실패: 현재 이메일과 동일</li>
 *     <li>실패: 새 이메일 중복</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeEmailService 단위 테스트")
class ChangeEmailServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordCredentialRepository passwordCredentialRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthEmailService authEmailService;

    @Mock
    private VerificationCodeGenerator verificationCodeGenerator;

    @InjectMocks
    private ChangeEmailService changeEmailService;

    private User memberUser;

    @BeforeEach
    void setUp() {
        memberUser = createMemberWithId();
    }

    @Nested
    @DisplayName("이메일 변경 요청 성공")
    class SuccessTest {

        @DisplayName("비밀번호 확인 후 새 이메일로 인증 코드가 발송된다")
        @Test
        void changeEmail_WithValidRequest_SendsVerificationCode() {
            // given
            Long userId = memberUser.getId();
            String newEmail = "newemail@example.com";
            ChangeEmailRequest request = new ChangeEmailRequest("currentPw1!", newEmail);

            PasswordCredential credential = mock(PasswordCredential.class);
            given(credential.getPasswordHash()).willReturn("hashedPw");

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("currentPw1!", "hashedPw")).willReturn(true);
            given(userRepository.existsByEmail(newEmail)).willReturn(false);
            given(emailVerificationRepository.findByEmailAndVerifiedFalse(newEmail)).willReturn(Optional.empty());
            given(verificationCodeGenerator.generateVerificationCode()).willReturn("123456");

            // when
            changeEmailService.changeEmail(userId, request);

            // then
            verify(emailVerificationRepository).save(any(EmailVerification.class));
            verify(authEmailService).sendVerificationEmail(eq(newEmail), eq("123456"));
        }

        @DisplayName("기존 미인증 레코드가 있으면 삭제 후 새로 발송한다")
        @Test
        void changeEmail_WithExistingUnverifiedRecord_DeletesAndSendsNew() {
            // given
            Long userId = memberUser.getId();
            String newEmail = "newemail@example.com";
            ChangeEmailRequest request = new ChangeEmailRequest("currentPw1!", newEmail);

            PasswordCredential credential = mock(PasswordCredential.class);
            given(credential.getPasswordHash()).willReturn("hashedPw");

            EmailVerification oldVerification = mock(EmailVerification.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("currentPw1!", "hashedPw")).willReturn(true);
            given(userRepository.existsByEmail(newEmail)).willReturn(false);
            given(emailVerificationRepository.findByEmailAndVerifiedFalse(newEmail)).willReturn(Optional.of(oldVerification));
            given(verificationCodeGenerator.generateVerificationCode()).willReturn("654321");

            // when
            changeEmailService.changeEmail(userId, request);

            // then
            verify(emailVerificationRepository).delete(oldVerification);
            verify(emailVerificationRepository).save(any(EmailVerification.class));
            verify(authEmailService).sendVerificationEmail(eq(newEmail), eq("654321"));
        }
    }

    @Nested
    @DisplayName("이메일 변경 요청 실패")
    class FailureTest {

        @DisplayName("존재하지 않는 사용자이면 UserNotFoundException 발생")
        @Test
        void changeEmail_WhenUserNotFound_ThrowsUserNotFoundException() {
            // given
            Long nonExistentUserId = 999L;
            ChangeEmailRequest request = new ChangeEmailRequest("currentPw1!", "new@example.com");

            given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> changeEmailService.changeEmail(nonExistentUserId, request))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @DisplayName("PasswordCredential이 없으면 UserNotFoundException 발생")
        @Test
        void changeEmail_WhenCredentialNotFound_ThrowsUserNotFoundException() {
            // given
            Long userId = memberUser.getId();
            ChangeEmailRequest request = new ChangeEmailRequest("currentPw1!", "new@example.com");

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> changeEmailService.changeEmail(userId, request))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @DisplayName("비밀번호 불일치 시 InvalidCredentialsException 발생")
        @Test
        void changeEmail_WithWrongPassword_ThrowsInvalidCredentialsException() {
            // given
            Long userId = memberUser.getId();
            ChangeEmailRequest request = new ChangeEmailRequest("wrongPw!", "new@example.com");

            PasswordCredential credential = mock(PasswordCredential.class);
            given(credential.getPasswordHash()).willReturn("hashedPw");

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("wrongPw!", "hashedPw")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> changeEmailService.changeEmail(userId, request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @DisplayName("현재 이메일과 동일하면 SameEmailException 발생")
        @Test
        void changeEmail_WithSameEmail_ThrowsSameEmailException() {
            // given
            Long userId = memberUser.getId();
            String currentEmail = memberUser.getEmail();
            ChangeEmailRequest request = new ChangeEmailRequest("currentPw1!", currentEmail);

            PasswordCredential credential = mock(PasswordCredential.class);
            given(credential.getPasswordHash()).willReturn("hashedPw");

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("currentPw1!", "hashedPw")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> changeEmailService.changeEmail(userId, request))
                    .isInstanceOf(SameEmailException.class);
        }

        @DisplayName("새 이메일이 이미 다른 사용자에게 등록되어 있으면 DuplicateEmailException 발생")
        @Test
        void changeEmail_WithDuplicateEmail_ThrowsDuplicateEmailException() {
            // given
            Long userId = memberUser.getId();
            String duplicateEmail = "duplicate@example.com";
            ChangeEmailRequest request = new ChangeEmailRequest("currentPw1!", duplicateEmail);

            PasswordCredential credential = mock(PasswordCredential.class);
            given(credential.getPasswordHash()).willReturn("hashedPw");

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("currentPw1!", "hashedPw")).willReturn(true);
            given(userRepository.existsByEmail(duplicateEmail)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> changeEmailService.changeEmail(userId, request))
                    .isInstanceOf(DuplicateEmailException.class);
        }
    }
}
