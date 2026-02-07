package igrus.web.admin.user.service;

import igrus.web.admin.user.exception.SelfRoleChangeException;
import igrus.web.security.auth.approval.exception.LastAdminCannotChangeException;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeUserRoleService 단위 테스트")
class ChangeUserRoleServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChangeUserRoleService changeUserRoleService;

    @Nested
    @DisplayName("권한 변경 성공")
    class SuccessTest {

        @Test
        @DisplayName("MEMBER를 OPERATOR로 변경")
        void changeRole_MemberToOperator_Success() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createTestUser();
            targetUser.verifyEmail();
            targetUser.changeRole(UserRole.MEMBER);
            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

            // when
            changeUserRoleService.changeUserRole(targetUserId, UserRole.OPERATOR, currentUserId);

            // then
            assertThat(targetUser.getRole()).isEqualTo(UserRole.OPERATOR);
        }

        @Test
        @DisplayName("ADMIN이 2명 이상일 때 ADMIN을 MEMBER로 강등")
        void changeRole_AdminToMember_WhenMultipleAdmins_Success() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createTestUser();
            targetUser.changeRole(UserRole.ADMIN);
            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(userRepository.countByRole(UserRole.ADMIN)).willReturn(2L);

            // when
            changeUserRoleService.changeUserRole(targetUserId, UserRole.MEMBER, currentUserId);

            // then
            assertThat(targetUser.getRole()).isEqualTo(UserRole.MEMBER);
        }
    }

    @Nested
    @DisplayName("권한 변경 실패")
    class FailureTest {

        @Test
        @DisplayName("자기 자신의 권한 변경 시도 시 SelfRoleChangeException 발생")
        void changeRole_Self_ThrowsException() {
            // given
            Long userId = 1L;

            // when & then
            assertThatThrownBy(() -> changeUserRoleService.changeUserRole(userId, UserRole.MEMBER, userId))
                    .isInstanceOf(SelfRoleChangeException.class);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 변경 시도 시 UserNotFoundException 발생")
        void changeRole_UserNotFound_ThrowsException() {
            // given
            Long targetUserId = 999L;
            Long currentUserId = 1L;
            given(userRepository.findById(targetUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> changeUserRoleService.changeUserRole(targetUserId, UserRole.MEMBER, currentUserId))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("마지막 ADMIN의 권한 변경 시도 시 LastAdminCannotChangeException 발생")
        void changeRole_LastAdmin_ThrowsException() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createTestUser();
            targetUser.changeRole(UserRole.ADMIN);
            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(userRepository.countByRole(UserRole.ADMIN)).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> changeUserRoleService.changeUserRole(targetUserId, UserRole.MEMBER, currentUserId))
                    .isInstanceOf(LastAdminCannotChangeException.class);
        }
    }

    private User createTestUser() {
        return User.create(
                "20231234",
                "홍길동",
                "hong@inha.edu",
                "010-1234-5678",
                "컴퓨터공학과",
                "테스트 동기",
                Gender.MALE,
                1
        );
    }
}
