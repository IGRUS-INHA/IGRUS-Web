package igrus.web.admin.user.service;

import igrus.web.admin.user.dto.AdminEditUserInfoRequest;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.domain.User;
import igrus.web.user.event.AccountStatusChangeEvent;
import igrus.web.user.exception.DuplicateEmailException;
import igrus.web.user.exception.DuplicatePhoneNumberException;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static igrus.web.common.fixture.UserTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminEditUserInfoService 단위 테스트")
class AdminEditUserInfoServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdminEditUserInfoService adminEditUserInfoService;

    @Nested
    @DisplayName("사용자 정보 수정 성공")
    class EditUserInfoSuccessTest {

        @Test
        @DisplayName("이메일을 변경하면 새 이메일로 업데이트되고 이전 이메일의 인증 기록이 삭제된다")
        void editUserInfo_ChangeEmail_Success() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createMemberWithId(targetUserId);
            String previousEmail = targetUser.getEmail();
            String newEmail = "newemail@inha.edu";

            AdminEditUserInfoRequest request = new AdminEditUserInfoRequest(
                    newEmail, null, null, null, null, null, null, null, null, null, null, null, null
            );

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(userRepository.existsByEmail(newEmail)).willReturn(false);

            // when
            adminEditUserInfoService.editUserInfo(targetUserId, request, currentUserId);

            // then
            assertThat(targetUser.getEmail()).isEqualTo(newEmail);
            verify(emailVerificationRepository).deleteByEmail(previousEmail);
            verify(eventPublisher).publishEvent(any(AccountStatusChangeEvent.class));
        }

        @Test
        @DisplayName("이름과 학과만 변경하면 해당 필드만 업데이트된다")
        void editUserInfo_ChangeNameAndDepartment_Success() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createMemberWithId(targetUserId);
            String originalEmail = targetUser.getEmail();
            String originalPhone = targetUser.getPhoneNumber();

            AdminEditUserInfoRequest request = new AdminEditUserInfoRequest(
                    null, "새이름", null, "정보통신공학과", null, null, null, null, null, null, null, null, null
            );

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

            // when
            adminEditUserInfoService.editUserInfo(targetUserId, request, currentUserId);

            // then
            assertThat(targetUser.getName()).isEqualTo("새이름");
            assertThat(targetUser.getDepartment()).isEqualTo("정보통신공학과");
            assertThat(targetUser.getEmail()).isEqualTo(originalEmail);
            assertThat(targetUser.getPhoneNumber()).isEqualTo(originalPhone);
        }

        @Test
        @DisplayName("동일한 이메일로 요청하면 중복 체크 없이 건너뛴다")
        void editUserInfo_SameEmail_SkipsDuplicateCheck() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createMemberWithId(targetUserId);
            String currentEmail = targetUser.getEmail();

            AdminEditUserInfoRequest request = new AdminEditUserInfoRequest(
                    currentEmail, null, null, null, null, null, null, null, null, null, null, null, null
            );

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

            // when
            adminEditUserInfoService.editUserInfo(targetUserId, request, currentUserId);

            // then
            verify(userRepository, never()).existsByEmail(any());
        }

        @Test
        @DisplayName("모든 필드가 null이면 아무 필드도 변경되지 않는다")
        void editUserInfo_AllNull_NoChanges() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createMemberWithId(targetUserId);
            String originalEmail = targetUser.getEmail();
            String originalName = targetUser.getName();

            AdminEditUserInfoRequest request = new AdminEditUserInfoRequest(
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

            // when
            adminEditUserInfoService.editUserInfo(targetUserId, request, currentUserId);

            // then
            assertThat(targetUser.getEmail()).isEqualTo(originalEmail);
            assertThat(targetUser.getName()).isEqualTo(originalName);
            verify(eventPublisher).publishEvent(any(AccountStatusChangeEvent.class));
        }

        @Test
        @DisplayName("ADMIN_INFO_EDIT 타입의 감사 이벤트가 발행된다")
        void editUserInfo_PublishesAdminInfoEditEvent() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createMemberWithId(targetUserId);

            AdminEditUserInfoRequest request = new AdminEditUserInfoRequest(
                    null, "변경된이름", null, null, null, null, null, null, null, null, null, null, null
            );

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

            // when
            adminEditUserInfoService.editUserInfo(targetUserId, request, currentUserId);

            // then
            ArgumentCaptor<AccountStatusChangeEvent> captor =
                    ArgumentCaptor.forClass(AccountStatusChangeEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            AccountStatusChangeEvent event = captor.getValue();
            assertThat(event.userId()).isEqualTo(targetUserId);
            assertThat(event.changedByUserId()).isEqualTo(currentUserId);
            assertThat(event.changeType()).isEqualTo(AccountChangeType.ADMIN_INFO_EDIT);
        }

        @Test
        @DisplayName("전화번호를 변경하면 중복 체크 후 업데이트된다")
        void editUserInfo_ChangePhoneNumber_Success() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createMemberWithId(targetUserId);
            String newPhone = "010-9999-8888";

            AdminEditUserInfoRequest request = new AdminEditUserInfoRequest(
                    null, null, newPhone, null, null, null, null, null, null, null, null, null, null
            );

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(userRepository.existsByPhoneNumber(newPhone)).willReturn(false);

            // when
            adminEditUserInfoService.editUserInfo(targetUserId, request, currentUserId);

            // then
            assertThat(targetUser.getPhoneNumber()).isEqualTo(newPhone);
        }
    }

    @Nested
    @DisplayName("사용자 정보 수정 실패")
    class EditUserInfoFailureTest {

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 수정하면 UserNotFoundException 발생")
        void editUserInfo_UserNotFound_ThrowsException() {
            // given
            Long targetUserId = 999L;
            Long currentUserId = 2L;
            AdminEditUserInfoRequest request = new AdminEditUserInfoRequest(
                    "new@inha.edu", null, null, null, null, null, null, null, null, null, null, null, null
            );

            given(userRepository.findById(targetUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminEditUserInfoService.editUserInfo(targetUserId, request, currentUserId))
                    .isInstanceOf(UserNotFoundException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("이미 존재하는 이메일로 변경하면 DuplicateEmailException 발생")
        void editUserInfo_DuplicateEmail_ThrowsException() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createMemberWithId(targetUserId);
            String duplicateEmail = "existing@inha.edu";

            AdminEditUserInfoRequest request = new AdminEditUserInfoRequest(
                    duplicateEmail, null, null, null, null, null, null, null, null, null, null, null, null
            );

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(userRepository.existsByEmail(duplicateEmail)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> adminEditUserInfoService.editUserInfo(targetUserId, request, currentUserId))
                    .isInstanceOf(DuplicateEmailException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("이미 존재하는 전화번호로 변경하면 DuplicatePhoneNumberException 발생")
        void editUserInfo_DuplicatePhoneNumber_ThrowsException() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createMemberWithId(targetUserId);
            String duplicatePhone = "010-0000-0000";

            AdminEditUserInfoRequest request = new AdminEditUserInfoRequest(
                    null, null, duplicatePhone, null, null, null, null, null, null, null, null, null, null
            );

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(userRepository.existsByPhoneNumber(duplicatePhone)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> adminEditUserInfoService.editUserInfo(targetUserId, request, currentUserId))
                    .isInstanceOf(DuplicatePhoneNumberException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("같은 전화번호로는 중복 체크를 하지 않는다")
        void editUserInfo_SamePhoneNumber_SkipsDuplicateCheck() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createMemberWithId(targetUserId);
            String currentPhone = targetUser.getPhoneNumber();

            AdminEditUserInfoRequest request = new AdminEditUserInfoRequest(
                    null, null, currentPhone, null, null, null, null, null, null, null, null, null, null
            );

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

            // when
            adminEditUserInfoService.editUserInfo(targetUserId, request, currentUserId);

            // then
            verify(userRepository, never()).existsByPhoneNumber(any());
        }
    }
}
