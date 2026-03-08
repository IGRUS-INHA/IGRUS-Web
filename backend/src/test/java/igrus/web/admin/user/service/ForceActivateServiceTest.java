package igrus.web.admin.user.service;

import igrus.web.admin.user.exception.UserNotPendingVerificationException;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.audit.AccountStatusChanged;
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
@DisplayName("ForceActivateService 단위 테스트")
class ForceActivateServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordCredentialRepository passwordCredentialRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ForceActivateService forceActivateService;

    @Nested
    @DisplayName("강제 활성화 성공")
    class ForceActivateSuccessTest {

        @Test
        @DisplayName("PENDING_VERIFICATION 사용자를 강제 활성화하면 User와 PasswordCredential 상태가 ACTIVE로 변경된다")
        void forceActivate_PendingUser_StatusBecomesActive() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createPendingVerificationUserWithId(targetUserId);
            PasswordCredential credential = mock(PasswordCredential.class);

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(passwordCredentialRepository.findByUserId(targetUserId)).willReturn(Optional.of(credential));

            // when
            forceActivateService.forceActivate(targetUserId, currentUserId);

            // then
            assertThat(targetUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            verify(credential).verifyEmail();
            verify(emailVerificationRepository).deleteByEmail(targetUser.getEmail());
        }

        @Test
        @DisplayName("FORCE_ACTIVATION 타입의 감사 이벤트가 올바른 값으로 발행된다")
        void forceActivate_PublishesForceActivationEvent() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createPendingVerificationUserWithId(targetUserId);

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(passwordCredentialRepository.findByUserId(targetUserId)).willReturn(Optional.empty());

            // when
            forceActivateService.forceActivate(targetUserId, currentUserId);

            // then
            ArgumentCaptor<AccountStatusChanged> captor =
                    ArgumentCaptor.forClass(AccountStatusChanged.class);
            verify(eventPublisher).publishEvent(captor.capture());

            AccountStatusChanged event = captor.getValue();
            assertThat(event.userId()).isEqualTo(targetUserId);
            assertThat(event.changedByUserId()).isEqualTo(currentUserId);
            assertThat(event.changeType()).isEqualTo(AccountChangeType.FORCE_ACTIVATION);
            assertThat(event.previousValue()).isEqualTo("PENDING_VERIFICATION");
            assertThat(event.newValue()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("PasswordCredential이 없어도 강제 활성화가 성공한다")
        void forceActivate_NoCredential_StillSucceeds() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createPendingVerificationUserWithId(targetUserId);

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(passwordCredentialRepository.findByUserId(targetUserId)).willReturn(Optional.empty());

            // when
            forceActivateService.forceActivate(targetUserId, currentUserId);

            // then
            assertThat(targetUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            verify(emailVerificationRepository).deleteByEmail(targetUser.getEmail());
            verify(eventPublisher).publishEvent(any(AccountStatusChanged.class));
        }
    }

    @Nested
    @DisplayName("강제 활성화 실패")
    class ForceActivateFailureTest {

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 강제 활성화하면 UserNotFoundException 발생")
        void forceActivate_UserNotFound_ThrowsException() {
            // given
            Long targetUserId = 999L;
            Long currentUserId = 2L;

            given(userRepository.findById(targetUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> forceActivateService.forceActivate(targetUserId, currentUserId))
                    .isInstanceOf(UserNotFoundException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("ACTIVE 상태 사용자를 강제 활성화하면 UserNotPendingVerificationException 발생")
        void forceActivate_ActiveUser_ThrowsException() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createMemberWithId(targetUserId);

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

            // when & then
            assertThatThrownBy(() -> forceActivateService.forceActivate(targetUserId, currentUserId))
                    .isInstanceOf(UserNotPendingVerificationException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("SUSPENDED 상태 사용자를 강제 활성화하면 UserNotPendingVerificationException 발생")
        void forceActivate_SuspendedUser_ThrowsException() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createMemberWithId(targetUserId);
            targetUser.suspend();

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

            // when & then
            assertThatThrownBy(() -> forceActivateService.forceActivate(targetUserId, currentUserId))
                    .isInstanceOf(UserNotPendingVerificationException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("WITHDRAWN 상태 사용자를 강제 활성화하면 UserNotPendingVerificationException 발생")
        void forceActivate_WithdrawnUser_ThrowsException() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createWithdrawnMemberWithId();

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

            // when & then
            assertThatThrownBy(() -> forceActivateService.forceActivate(targetUserId, currentUserId))
                    .isInstanceOf(UserNotPendingVerificationException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }
    }
}
