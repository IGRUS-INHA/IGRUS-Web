package igrus.web.admin.user.service;

import igrus.web.admin.user.dto.ChangeUserStatusRequest;
import igrus.web.admin.user.exception.SelfStatusChangeException;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserSuspension;
import igrus.web.user.event.AccountStatusChangeEvent;
import igrus.web.user.exception.InvalidSuspensionException;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.repository.UserSuspensionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static igrus.web.common.fixture.UserTestFixture.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeUserStatusService 단위 테스트")
class ChangeUserStatusServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSuspensionRepository userSuspensionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChangeUserStatusService changeUserStatusService;

    @Nested
    @DisplayName("계정 정지 성공")
    class SuspendSuccessTest {

        @Test
        @DisplayName("정상 사용자를 정지하면 상태 변경 및 이벤트가 발행된다")
        void suspend_ActiveUser_Success() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createMemberWithId(targetUserId);
            Instant suspendedUntil = Instant.now().plus(7, ChronoUnit.DAYS);
            ChangeUserStatusRequest request = new ChangeUserStatusRequest(
                    ChangeUserStatusRequest.Action.SUSPEND, "규정 위반", suspendedUntil);

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

            // when
            changeUserStatusService.changeUserStatus(targetUserId, request, currentUserId);

            // then
            verify(userSuspensionRepository).save(any(UserSuspension.class));
            verify(eventPublisher).publishEvent(any(AccountStatusChangeEvent.class));
        }
    }

    @Nested
    @DisplayName("정지 해제 성공")
    class LiftSuccessTest {

        @Test
        @DisplayName("정지된 사용자를 해제하면 상태 변경 및 이벤트가 발행된다")
        void lift_SuspendedUser_Success() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createMemberWithId(targetUserId);
            targetUser.suspend();

            UserSuspension suspension = UserSuspension.create(
                    targetUser, "규정 위반",
                    Instant.now().plus(7, ChronoUnit.DAYS), currentUserId);

            ChangeUserStatusRequest request = new ChangeUserStatusRequest(
                    ChangeUserStatusRequest.Action.LIFT, null, null);

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(userSuspensionRepository.findActiveByUserId(any(), any()))
                    .willReturn(Optional.of(suspension));

            // when
            changeUserStatusService.changeUserStatus(targetUserId, request, currentUserId);

            // then
            verify(eventPublisher).publishEvent(any(AccountStatusChangeEvent.class));
        }
    }

    @Nested
    @DisplayName("상태 변경 실패")
    class FailureTest {

        @Test
        @DisplayName("자기 자신의 상태 변경 시도 시 SelfStatusChangeException 발생")
        void changeStatus_Self_ThrowsException() {
            // given
            Long userId = 1L;
            ChangeUserStatusRequest request = new ChangeUserStatusRequest(
                    ChangeUserStatusRequest.Action.SUSPEND, "사유", Instant.now().plus(1, ChronoUnit.DAYS));

            // when & then
            assertThatThrownBy(() -> changeUserStatusService.changeUserStatus(userId, request, userId))
                    .isInstanceOf(SelfStatusChangeException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("이미 정지된 사용자 정지 시도 시 InvalidSuspensionException 발생")
        void suspend_AlreadySuspended_ThrowsException() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createMemberWithId(targetUserId);
            targetUser.suspend();

            Instant suspendedUntil = Instant.now().plus(7, ChronoUnit.DAYS);
            ChangeUserStatusRequest request = new ChangeUserStatusRequest(
                    ChangeUserStatusRequest.Action.SUSPEND, "규정 위반", suspendedUntil);

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

            // when & then
            assertThatThrownBy(() -> changeUserStatusService.changeUserStatus(targetUserId, request, currentUserId))
                    .isInstanceOf(InvalidSuspensionException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("사유 없이 정지 시도 시 InvalidSuspensionException 발생")
        void suspend_WithoutReason_ThrowsException() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            Instant suspendedUntil = Instant.now().plus(7, ChronoUnit.DAYS);
            ChangeUserStatusRequest request = new ChangeUserStatusRequest(
                    ChangeUserStatusRequest.Action.SUSPEND, null, suspendedUntil);

            // when & then
            assertThatThrownBy(() -> changeUserStatusService.changeUserStatus(targetUserId, request, currentUserId))
                    .isInstanceOf(InvalidSuspensionException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("정지 기한 없이 정지 시도 시 InvalidSuspensionException 발생")
        void suspend_WithoutDate_ThrowsException() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            ChangeUserStatusRequest request = new ChangeUserStatusRequest(
                    ChangeUserStatusRequest.Action.SUSPEND, "사유", null);

            // when & then
            assertThatThrownBy(() -> changeUserStatusService.changeUserStatus(targetUserId, request, currentUserId))
                    .isInstanceOf(InvalidSuspensionException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("존재하지 않는 사용자 정지 시도 시 UserNotFoundException 발생")
        void suspend_UserNotFound_ThrowsException() {
            // given
            Long targetUserId = 999L;
            Long currentUserId = 2L;
            Instant suspendedUntil = Instant.now().plus(7, ChronoUnit.DAYS);
            ChangeUserStatusRequest request = new ChangeUserStatusRequest(
                    ChangeUserStatusRequest.Action.SUSPEND, "규정 위반", suspendedUntil);

            given(userRepository.findById(targetUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> changeUserStatusService.changeUserStatus(targetUserId, request, currentUserId))
                    .isInstanceOf(UserNotFoundException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("활성 정지가 없는 사용자 해제 시도 시 InvalidSuspensionException 발생")
        void lift_NoActiveSuspension_ThrowsException() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetUser = createMemberWithId(targetUserId);
            ChangeUserStatusRequest request = new ChangeUserStatusRequest(
                    ChangeUserStatusRequest.Action.LIFT, null, null);

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));
            given(userSuspensionRepository.findActiveByUserId(any(), any()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> changeUserStatusService.changeUserStatus(targetUserId, request, currentUserId))
                    .isInstanceOf(InvalidSuspensionException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }
    }
}
