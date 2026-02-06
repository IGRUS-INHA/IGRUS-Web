package igrus.web.user.mypage.service.write;

import igrus.web.user.domain.User;
import igrus.web.user.exception.DuplicateEmailException;
import igrus.web.user.exception.DuplicatePhoneNumberException;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.mypage.dto.request.UpdateProfileRequest;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * UpdateMyProfileService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>MP-014: 이메일 수정 성공</li>
 *     <li>MP-015: 전화번호 수정 성공</li>
 *     <li>MP-016: 이메일 중복 시 예외</li>
 *     <li>MP-017: 전화번호 중복 시 예외</li>
 *     <li>MP-018: 존재하지 않는 사용자 프로필 수정</li>
 *     <li>MP-019: 기존 이메일과 동일하면 수정 안 함</li>
 *     <li>MP-020: null 값이면 해당 필드 수정 안 함</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateMyProfileService 단위 테스트")
class UpdateMyProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UpdateMyProfileService updateMyProfileService;

    private User memberUser;

    @BeforeEach
    void setUp() {
        memberUser = createMemberWithId();
    }

    @Nested
    @DisplayName("프로필 수정 성공 테스트")
    class UpdateProfileSuccessTest {

        @DisplayName("MP-014: 이메일 수정 성공")
        @Test
        void updateProfile_WithNewEmail_UpdatesEmail() {
            // given
            Long userId = memberUser.getId();
            UpdateProfileRequest request = new UpdateProfileRequest("newemail@inha.edu", null);

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(userRepository.existsByEmail("newemail@inha.edu")).willReturn(false);

            // when
            updateMyProfileService.updateProfile(userId, request);

            // then
            assertThat(memberUser.getEmail()).isEqualTo("newemail@inha.edu");
        }

        @DisplayName("MP-015: 전화번호 수정 성공")
        @Test
        void updateProfile_WithNewPhoneNumber_UpdatesPhoneNumber() {
            // given
            Long userId = memberUser.getId();
            UpdateProfileRequest request = new UpdateProfileRequest(null, "010-9999-8888");

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(userRepository.existsByPhoneNumber("010-9999-8888")).willReturn(false);

            // when
            updateMyProfileService.updateProfile(userId, request);

            // then
            assertThat(memberUser.getPhoneNumber()).isEqualTo("010-9999-8888");
        }
    }

    @Nested
    @DisplayName("프로필 수정 실패 테스트")
    class UpdateProfileFailTest {

        @DisplayName("MP-016: 이메일 중복 시 DuplicateEmailException 발생")
        @Test
        void updateProfile_WithDuplicateEmail_ThrowsDuplicateEmailException() {
            // given
            Long userId = memberUser.getId();
            UpdateProfileRequest request = new UpdateProfileRequest("duplicate@inha.edu", null);

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(userRepository.existsByEmail("duplicate@inha.edu")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> updateMyProfileService.updateProfile(userId, request))
                    .isInstanceOf(DuplicateEmailException.class);
        }

        @DisplayName("MP-017: 전화번호 중복 시 DuplicatePhoneNumberException 발생")
        @Test
        void updateProfile_WithDuplicatePhoneNumber_ThrowsDuplicatePhoneNumberException() {
            // given
            Long userId = memberUser.getId();
            UpdateProfileRequest request = new UpdateProfileRequest(null, "010-1111-2222");

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(userRepository.existsByPhoneNumber("010-1111-2222")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> updateMyProfileService.updateProfile(userId, request))
                    .isInstanceOf(DuplicatePhoneNumberException.class);
        }

        @DisplayName("MP-018: 존재하지 않는 사용자 프로필 수정 시 UserNotFoundException 발생")
        @Test
        void updateProfile_WhenUserNotFound_ThrowsUserNotFoundException() {
            // given
            Long nonExistentUserId = 999L;
            UpdateProfileRequest request = new UpdateProfileRequest("new@inha.edu", null);

            given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> updateMyProfileService.updateProfile(nonExistentUserId, request))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("프로필 수정 스킵 테스트")
    class UpdateProfileSkipTest {

        @DisplayName("MP-019: 기존 이메일과 동일하면 중복 체크 없이 정상 완료")
        @Test
        void updateProfile_WithSameEmail_SkipsDuplicateCheck() {
            // given
            Long userId = memberUser.getId();
            String currentEmail = memberUser.getEmail();
            UpdateProfileRequest request = new UpdateProfileRequest(currentEmail, null);

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));

            // when
            updateMyProfileService.updateProfile(userId, request);

            // then
            verify(userRepository, never()).existsByEmail(currentEmail);
        }

        @DisplayName("MP-020: null 값이면 해당 필드 수정 안 함")
        @Test
        void updateProfile_WithNullValues_SkipsUpdate() {
            // given
            Long userId = memberUser.getId();
            String originalEmail = memberUser.getEmail();
            String originalPhone = memberUser.getPhoneNumber();
            UpdateProfileRequest request = new UpdateProfileRequest(null, null);

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));

            // when
            updateMyProfileService.updateProfile(userId, request);

            // then
            assertThat(memberUser.getEmail()).isEqualTo(originalEmail);
            assertThat(memberUser.getPhoneNumber()).isEqualTo(originalPhone);
        }
    }
}
