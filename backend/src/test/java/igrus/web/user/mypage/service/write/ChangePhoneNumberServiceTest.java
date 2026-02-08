package igrus.web.user.mypage.service.write;

import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.DuplicatePhoneNumberException;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.mypage.dto.request.ChangePhoneNumberRequest;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * ChangePhoneNumberService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>성공: 비밀번호 확인 후 전화번호 변경 성공</li>
 *     <li>실패: 사용자 미존재</li>
 *     <li>실패: 비밀번호 불일치</li>
 *     <li>실패: 현재 전화번호와 동일</li>
 *     <li>실패: 새 전화번호 중복</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChangePhoneNumberService 단위 테스트")
class ChangePhoneNumberServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordCredentialRepository passwordCredentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ChangePhoneNumberService changePhoneNumberService;

    private User memberUser;

    @BeforeEach
    void setUp() {
        memberUser = createMemberWithId();
    }

    @Nested
    @DisplayName("전화번호 변경 성공")
    class SuccessTest {

        @DisplayName("비밀번호 확인 후 전화번호가 정상적으로 변경된다")
        @Test
        void changePhoneNumber_WithValidRequest_Success() {
            // given
            Long userId = memberUser.getId();
            String newPhoneNumber = "010-9999-8888";
            ChangePhoneNumberRequest request = new ChangePhoneNumberRequest("currentPw1!", newPhoneNumber);

            PasswordCredential credential = mock(PasswordCredential.class);
            given(credential.getPasswordHash()).willReturn("hashedPw");

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("currentPw1!", "hashedPw")).willReturn(true);
            given(userRepository.existsByPhoneNumber(newPhoneNumber)).willReturn(false);

            // when
            changePhoneNumberService.changePhoneNumber(userId, request);

            // then
            assertThat(memberUser.getPhoneNumber()).isEqualTo(newPhoneNumber);
        }
    }

    @Nested
    @DisplayName("전화번호 변경 실패")
    class FailureTest {

        @DisplayName("존재하지 않는 사용자이면 UserNotFoundException 발생")
        @Test
        void changePhoneNumber_WhenUserNotFound_ThrowsUserNotFoundException() {
            // given
            Long nonExistentUserId = 999L;
            ChangePhoneNumberRequest request = new ChangePhoneNumberRequest("currentPw1!", "010-9999-8888");

            given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> changePhoneNumberService.changePhoneNumber(nonExistentUserId, request))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @DisplayName("PasswordCredential이 없으면 UserNotFoundException 발생")
        @Test
        void changePhoneNumber_WhenCredentialNotFound_ThrowsUserNotFoundException() {
            // given
            Long userId = memberUser.getId();
            ChangePhoneNumberRequest request = new ChangePhoneNumberRequest("currentPw1!", "010-9999-8888");

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> changePhoneNumberService.changePhoneNumber(userId, request))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @DisplayName("비밀번호 불일치 시 InvalidCredentialsException 발생")
        @Test
        void changePhoneNumber_WithWrongPassword_ThrowsInvalidCredentialsException() {
            // given
            Long userId = memberUser.getId();
            ChangePhoneNumberRequest request = new ChangePhoneNumberRequest("wrongPw!", "010-9999-8888");

            PasswordCredential credential = mock(PasswordCredential.class);
            given(credential.getPasswordHash()).willReturn("hashedPw");

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("wrongPw!", "hashedPw")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> changePhoneNumberService.changePhoneNumber(userId, request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @DisplayName("현재 전화번호와 동일하면 DuplicatePhoneNumberException 발생")
        @Test
        void changePhoneNumber_WithSamePhoneNumber_ThrowsDuplicatePhoneNumberException() {
            // given
            Long userId = memberUser.getId();
            String currentPhone = memberUser.getPhoneNumber();
            ChangePhoneNumberRequest request = new ChangePhoneNumberRequest("currentPw1!", currentPhone);

            PasswordCredential credential = mock(PasswordCredential.class);
            given(credential.getPasswordHash()).willReturn("hashedPw");

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("currentPw1!", "hashedPw")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> changePhoneNumberService.changePhoneNumber(userId, request))
                    .isInstanceOf(DuplicatePhoneNumberException.class);
        }

        @DisplayName("새 전화번호가 이미 다른 사용자에게 등록되어 있으면 DuplicatePhoneNumberException 발생")
        @Test
        void changePhoneNumber_WithDuplicatePhoneNumber_ThrowsDuplicatePhoneNumberException() {
            // given
            Long userId = memberUser.getId();
            String duplicatePhone = "010-9999-8888";
            ChangePhoneNumberRequest request = new ChangePhoneNumberRequest("currentPw1!", duplicatePhone);

            PasswordCredential credential = mock(PasswordCredential.class);
            given(credential.getPasswordHash()).willReturn("hashedPw");

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("currentPw1!", "hashedPw")).willReturn(true);
            given(userRepository.existsByPhoneNumber(duplicatePhone)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> changePhoneNumberService.changePhoneNumber(userId, request))
                    .isInstanceOf(DuplicatePhoneNumberException.class);
        }
    }
}
