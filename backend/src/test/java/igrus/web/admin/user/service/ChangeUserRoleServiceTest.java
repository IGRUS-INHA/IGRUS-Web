package igrus.web.admin.user.service;

import igrus.web.admin.user.exception.SelfRoleChangeException;
import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.security.auth.approval.domain.AssociateDecisionType;
import igrus.web.security.auth.approval.exception.LastAdminCannotChangeException;
import igrus.web.security.auth.approval.repository.AssociateDecisionRepository;
import igrus.web.security.auth.approval.service.manage.ValidateNotLastAdminService;
import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserRoleHistory;
import igrus.web.user.event.AccountStatusChangeEvent;
import igrus.web.user.exception.SameRoleChangeException;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.repository.UserRoleHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeUserRoleService 단위 테스트")
class ChangeUserRoleServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleHistoryRepository userRoleHistoryRepository;

    @Mock
    private AssociateDecisionRepository associateDecisionRepository;

    @Mock
    private ValidateNotLastAdminService validateNotLastAdminService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChangeUserRoleService changeUserRoleService;

    @Nested
    @DisplayName("권한 변경 성공")
    class SuccessTest {

        @Test
        @DisplayName("MEMBER를 OPERATOR로 변경하면 역할이 변경되고 이력이 저장됨")
        void changeRole_MemberToOperator_Success() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createTestUser();
            targetUser.changeRole(UserRole.MEMBER);
            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

            // when
            changeUserRoleService.changeUserRole(targetUserId, UserRole.OPERATOR, currentUserId);

            // then
            assertThat(targetUser.getRole()).isEqualTo(UserRole.OPERATOR);
            verify(userRoleHistoryRepository).save(any(UserRoleHistory.class));
            verify(validateNotLastAdminService).validateNotLastAdmin(targetUserId);
            verify(refreshTokenRepository).revokeAllByUserId(targetUserId);
            verify(eventPublisher).publishEvent(any(AccountStatusChangeEvent.class));
        }

        @Test
        @DisplayName("MEMBER를 ASSOCIATE로 강등하면 DEMOTED 기록이 생성됨")
        void changeRole_MemberToAssociate_CreatesDemotedRecord() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createTestUser();
            targetUser.changeRole(UserRole.MEMBER);
            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(associateDecisionRepository.findByUserIdAndActiveTrue(targetUserId)).willReturn(Optional.empty());

            // when
            changeUserRoleService.changeUserRole(targetUserId, UserRole.ASSOCIATE, currentUserId);

            // then
            assertThat(targetUser.getRole()).isEqualTo(UserRole.ASSOCIATE);
            verify(associateDecisionRepository).save(any(AssociateDecision.class));
        }

        @Test
        @DisplayName("MEMBER를 ASSOCIATE로 강등하면 기존 APPROVED 결정이 비활성화됨")
        void changeRole_MemberToAssociate_DeactivatesExistingDecision() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createTestUser();
            targetUser.changeRole(UserRole.MEMBER);
            AssociateDecision existingDecision = AssociateDecision.approve(targetUser, currentUserId);
            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(associateDecisionRepository.findByUserIdAndActiveTrue(targetUserId)).willReturn(Optional.of(existingDecision));

            // when
            changeUserRoleService.changeUserRole(targetUserId, UserRole.ASSOCIATE, currentUserId);

            // then
            assertThat(existingDecision.isActive()).isFalse();
            verify(associateDecisionRepository).save(any(AssociateDecision.class));
        }

        @Test
        @DisplayName("OPERATOR를 ASSOCIATE로 강등해도 DEMOTED 기록이 생성됨")
        void changeRole_OperatorToAssociate_CreatesDemotedRecord() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createTestUser();
            targetUser.changeRole(UserRole.OPERATOR);
            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(associateDecisionRepository.findByUserIdAndActiveTrue(targetUserId)).willReturn(Optional.empty());

            // when
            changeUserRoleService.changeUserRole(targetUserId, UserRole.ASSOCIATE, currentUserId);

            // then
            assertThat(targetUser.getRole()).isEqualTo(UserRole.ASSOCIATE);
            verify(associateDecisionRepository).save(any(AssociateDecision.class));
        }

        @Test
        @DisplayName("ADMIN이 2명 이상일 때 ADMIN을 MEMBER로 강등하면 성공")
        void changeRole_AdminToMember_WhenMultipleAdmins_Success() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createTestUser();
            targetUser.changeRole(UserRole.ADMIN);
            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

            // when
            changeUserRoleService.changeUserRole(targetUserId, UserRole.MEMBER, currentUserId);

            // then
            assertThat(targetUser.getRole()).isEqualTo(UserRole.MEMBER);
            verify(userRoleHistoryRepository).save(any(UserRoleHistory.class));
            verify(refreshTokenRepository).revokeAllByUserId(targetUserId);
            verify(eventPublisher).publishEvent(any(AccountStatusChangeEvent.class));
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
            doThrow(new LastAdminCannotChangeException())
                    .when(validateNotLastAdminService).validateNotLastAdmin(targetUserId);

            // when & then
            assertThatThrownBy(() -> changeUserRoleService.changeUserRole(targetUserId, UserRole.MEMBER, currentUserId))
                    .isInstanceOf(LastAdminCannotChangeException.class);
        }

        @Test
        @DisplayName("동일 역할로 변경 시도 시 SameRoleChangeException 발생")
        void changeRole_SameRole_ThrowsException() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createTestUser();
            targetUser.changeRole(UserRole.MEMBER);
            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

            // when & then
            assertThatThrownBy(() -> changeUserRoleService.changeUserRole(targetUserId, UserRole.MEMBER, currentUserId))
                    .isInstanceOf(SameRoleChangeException.class);
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
                List.of(),
                Gender.MALE,
                1,
                EnrollmentStatus.ENROLLED,
                List.of(), null, null, null
        );
    }
}
