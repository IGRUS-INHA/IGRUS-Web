package igrus.web.user.mypage.service.write;

import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.security.auth.password.exception.InvalidPasswordFormatException;
import igrus.web.security.auth.password.exception.SamePasswordException;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.security.auth.password.service.support.ValidatePasswordFormatService;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.mypage.dto.request.ChangePasswordRequest;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * ChangeMyPasswordService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>MP-010: 비밀번호 변경 성공</li>
 *     <li>MP-011: 현재 비밀번호 불일치</li>
 *     <li>MP-012: 새 비밀번호 형식 오류</li>
 *     <li>MP-013: 존재하지 않는 사용자 비밀번호 변경</li>
 *     <li>MP-021: 현재 비밀번호와 새 비밀번호 동일</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeMyPasswordService 단위 테스트")
class ChangeMyPasswordServiceTest {

    @Mock
    private PasswordCredentialRepository passwordCredentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ValidatePasswordFormatService validatePasswordFormatService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private ChangeMyPasswordService changeMyPasswordService;

    private User memberUser;

    @BeforeEach
    void setUp() {
        memberUser = createMemberWithId();
    }

    @Nested
    @DisplayName("비밀번호 변경 테스트")
    class ChangePasswordTest {

        @DisplayName("MP-010: 비밀번호 변경 성공")
        @Test
        void changePassword_WithValidRequest_Success() {
            // given
            Long userId = memberUser.getId();
            ChangePasswordRequest request = new ChangePasswordRequest("currentPw1!", "NewPassword1!");

            PasswordCredential credential = mock(PasswordCredential.class);
            given(credential.getPasswordHash()).willReturn("hashedCurrentPw");

            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("currentPw1!", "hashedCurrentPw")).willReturn(true);
            given(passwordEncoder.encode("NewPassword1!")).willReturn("hashedNewPw");

            // when
            changeMyPasswordService.changePassword(userId, request);

            // then
            verify(validatePasswordFormatService).validatePasswordFormat("NewPassword1!");
            verify(credential).changePassword("hashedNewPw");
            verify(refreshTokenRepository).revokeAllByUserId(userId);
        }

        @DisplayName("MP-011: 현재 비밀번호 불일치 시 InvalidCredentialsException 발생")
        @Test
        void changePassword_WithWrongCurrentPassword_ThrowsInvalidCredentialsException() {
            // given
            Long userId = memberUser.getId();
            ChangePasswordRequest request = new ChangePasswordRequest("wrongPw1!", "NewPassword1!");

            PasswordCredential credential = mock(PasswordCredential.class);
            given(credential.getPasswordHash()).willReturn("hashedCurrentPw");

            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("wrongPw1!", "hashedCurrentPw")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> changeMyPasswordService.changePassword(userId, request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @DisplayName("MP-012: 새 비밀번호 형식 오류 시 InvalidPasswordFormatException 발생")
        @Test
        void changePassword_WithInvalidNewPasswordFormat_ThrowsInvalidPasswordFormatException() {
            // given
            Long userId = memberUser.getId();
            ChangePasswordRequest request = new ChangePasswordRequest("currentPw1!", "weak");

            PasswordCredential credential = mock(PasswordCredential.class);
            given(credential.getPasswordHash()).willReturn("hashedCurrentPw");

            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("currentPw1!", "hashedCurrentPw")).willReturn(true);
            doThrow(new InvalidPasswordFormatException())
                    .when(validatePasswordFormatService).validatePasswordFormat("weak");

            // when & then
            assertThatThrownBy(() -> changeMyPasswordService.changePassword(userId, request))
                    .isInstanceOf(InvalidPasswordFormatException.class);
        }

        @DisplayName("MP-013: 존재하지 않는 사용자 비밀번호 변경 시 UserNotFoundException 발생")
        @Test
        void changePassword_WhenUserNotFound_ThrowsUserNotFoundException() {
            // given
            Long nonExistentUserId = 999L;
            ChangePasswordRequest request = new ChangePasswordRequest("currentPw1!", "NewPassword1!");

            given(passwordCredentialRepository.findByUserId(nonExistentUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> changeMyPasswordService.changePassword(nonExistentUserId, request))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @DisplayName("MP-021: 현재 비밀번호와 새 비밀번호가 동일하면 SamePasswordException 발생")
        @Test
        void changePassword_WithSamePassword_ThrowsSamePasswordException() {
            // given
            Long userId = memberUser.getId();
            ChangePasswordRequest request = new ChangePasswordRequest("samePw1!", "samePw1!");

            PasswordCredential credential = mock(PasswordCredential.class);
            given(credential.getPasswordHash()).willReturn("hashedSamePw");

            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("samePw1!", "hashedSamePw")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> changeMyPasswordService.changePassword(userId, request))
                    .isInstanceOf(SamePasswordException.class);
        }
    }
}
