package igrus.web.admin.user.service;

import igrus.web.admin.user.exception.ForceWithdrawException;
import igrus.web.admin.user.exception.SelfStatusChangeException;
import igrus.web.security.auth.common.exception.account.AccountWithdrawnException;
import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.audit.AccountStatusChanged;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.withdrawal.domain.WithdrawalLog;
import igrus.web.user.withdrawal.repository.WithdrawalLogRepository;
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
@DisplayName("ForceWithdrawService 단위 테스트")
class ForceWithdrawServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordCredentialRepository passwordCredentialRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private WithdrawalLogRepository withdrawalLogRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ForceWithdrawService forceWithdrawService;

    @Nested
    @DisplayName("강제 탈퇴 성공")
    class ForceWithdrawSuccessTest {

        @Test
        @DisplayName("활성 회원을 강제 탈퇴하면 상태 변경, soft delete, 토큰 무효화, 로그 저장, 이벤트 발행이 수행된다")
        void forceWithdraw_ActiveMember_Success() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            String reason = "동아리 규정 위반";
            User targetUser = createMemberWithId(targetUserId);
            PasswordCredential credential = mock(PasswordCredential.class);

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(passwordCredentialRepository.findByUserId(targetUserId)).willReturn(Optional.of(credential));

            // when
            forceWithdrawService.forceWithdraw(targetUserId, reason, currentUserId);

            // then
            assertThat(targetUser.isWithdrawn()).isTrue();
            verify(withdrawalLogRepository).save(any(WithdrawalLog.class));
            verify(credential).withdraw();
            verify(credential).delete(currentUserId);
            verify(refreshTokenRepository).revokeAllByUserId(targetUserId);
            verify(eventPublisher).publishEvent(any(AccountStatusChanged.class));
        }

        @Test
        @DisplayName("정지된 회원도 강제 탈퇴할 수 있다")
        void forceWithdraw_SuspendedMember_Success() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            String reason = "동아리 규정 위반";
            User targetUser = createMemberWithId(targetUserId);
            targetUser.suspend();

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(passwordCredentialRepository.findByUserId(targetUserId)).willReturn(Optional.empty());

            // when
            forceWithdrawService.forceWithdraw(targetUserId, reason, currentUserId);

            // then
            assertThat(targetUser.isWithdrawn()).isTrue();
            verify(withdrawalLogRepository).save(any(WithdrawalLog.class));
            verify(refreshTokenRepository).revokeAllByUserId(targetUserId);
            verify(eventPublisher).publishEvent(any(AccountStatusChanged.class));
        }

        @Test
        @DisplayName("ADMIN이 여러 명일 때 ADMIN을 강제 탈퇴할 수 있다")
        void forceWithdraw_AdminWithMultipleAdmins_Success() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            String reason = "직무 태만";
            User targetAdmin = createAdminWithId(targetUserId);

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetAdmin));
            given(userRepository.countByRole(UserRole.ADMIN)).willReturn(2L);
            given(passwordCredentialRepository.findByUserId(targetUserId)).willReturn(Optional.empty());

            // when
            forceWithdrawService.forceWithdraw(targetUserId, reason, currentUserId);

            // then
            assertThat(targetAdmin.isWithdrawn()).isTrue();
            verify(withdrawalLogRepository).save(any(WithdrawalLog.class));
            verify(eventPublisher).publishEvent(any(AccountStatusChanged.class));
        }

        @Test
        @DisplayName("WithdrawalLog에 사유가 정확히 저장된다")
        void forceWithdraw_SavesReasonCorrectly() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            String reason = "동아리 규정 위반";
            User targetUser = createMemberWithId(targetUserId);

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(passwordCredentialRepository.findByUserId(targetUserId)).willReturn(Optional.empty());

            // when
            forceWithdrawService.forceWithdraw(targetUserId, reason, currentUserId);

            // then
            ArgumentCaptor<WithdrawalLog> captor = ArgumentCaptor.forClass(WithdrawalLog.class);
            verify(withdrawalLogRepository).save(captor.capture());

            WithdrawalLog savedLog = captor.getValue();
            assertThat(savedLog.getReason()).isEqualTo(reason);
            assertThat(savedLog.getUser()).isEqualTo(targetUser);
        }

        @Test
        @DisplayName("FORCE_WITHDRAWAL 타입의 감사 이벤트가 발행된다")
        void forceWithdraw_PublishesForceWithdrawalEvent() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            String reason = "동아리 규정 위반";
            User targetUser = createMemberWithId(targetUserId);

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(passwordCredentialRepository.findByUserId(targetUserId)).willReturn(Optional.empty());

            // when
            forceWithdrawService.forceWithdraw(targetUserId, reason, currentUserId);

            // then
            ArgumentCaptor<AccountStatusChanged> captor =
                    ArgumentCaptor.forClass(AccountStatusChanged.class);
            verify(eventPublisher).publishEvent(captor.capture());

            AccountStatusChanged event = captor.getValue();
            assertThat(event.userId()).isEqualTo(targetUserId);
            assertThat(event.changedByUserId()).isEqualTo(currentUserId);
            assertThat(event.changeType()).isEqualTo(AccountChangeType.FORCE_WITHDRAWAL);
            assertThat(event.previousValue()).isEqualTo("ACTIVE");
            assertThat(event.newValue()).isEqualTo("WITHDRAWN");
            assertThat(event.reason()).isEqualTo(reason);
        }

        @Test
        @DisplayName("PasswordCredential이 없어도 강제 탈퇴가 성공한다")
        void forceWithdraw_NoCredential_StillSucceeds() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            String reason = "동아리 규정 위반";
            User targetUser = createMemberWithId(targetUserId);

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(passwordCredentialRepository.findByUserId(targetUserId)).willReturn(Optional.empty());

            // when
            forceWithdrawService.forceWithdraw(targetUserId, reason, currentUserId);

            // then
            assertThat(targetUser.isWithdrawn()).isTrue();
            verify(withdrawalLogRepository).save(any(WithdrawalLog.class));
            verify(refreshTokenRepository).revokeAllByUserId(targetUserId);
            verify(eventPublisher).publishEvent(any(AccountStatusChanged.class));
        }
    }

    @Nested
    @DisplayName("강제 탈퇴 실패")
    class ForceWithdrawFailureTest {

        @Test
        @DisplayName("자기 자신을 강제 탈퇴하면 SelfStatusChangeException 발생")
        void forceWithdraw_Self_ThrowsException() {
            // given
            Long userId = 1L;
            String reason = "테스트";

            // when & then
            assertThatThrownBy(() -> forceWithdrawService.forceWithdraw(userId, reason, userId))
                    .isInstanceOf(SelfStatusChangeException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("존재하지 않는 사용자를 강제 탈퇴하면 UserNotFoundException 발생")
        void forceWithdraw_UserNotFound_ThrowsException() {
            // given
            Long targetUserId = 999L;
            Long currentUserId = 2L;
            String reason = "테스트";

            given(userRepository.findById(targetUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> forceWithdrawService.forceWithdraw(targetUserId, reason, currentUserId))
                    .isInstanceOf(UserNotFoundException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("이미 탈퇴한 사용자를 강제 탈퇴하면 AccountWithdrawnException 발생")
        void forceWithdraw_AlreadyWithdrawn_ThrowsException() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            String reason = "테스트";
            User targetUser = createWithdrawnMemberWithId();

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

            // when & then
            assertThatThrownBy(() -> forceWithdrawService.forceWithdraw(targetUserId, reason, currentUserId))
                    .isInstanceOf(AccountWithdrawnException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("마지막 ADMIN을 강제 탈퇴하면 ForceWithdrawException 발생")
        void forceWithdraw_LastAdmin_ThrowsException() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            String reason = "테스트";
            User targetAdmin = createAdminWithId(targetUserId);

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetAdmin));
            given(userRepository.countByRole(UserRole.ADMIN)).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> forceWithdrawService.forceWithdraw(targetUserId, reason, currentUserId))
                    .isInstanceOf(ForceWithdrawException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }
    }
}
