package igrus.web.admin.user.service;

import igrus.web.admin.user.dto.ChangeUserStatusRequest;
import igrus.web.admin.user.exception.SelfStatusChangeException;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static igrus.web.common.fixture.UserTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

        @Test
        @DisplayName("정지 종료일이 과거이면 InvalidSuspensionException 발생")
        void suspend_PastEndDate_ThrowsException() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            Instant pastDate = Instant.now().minus(1, ChronoUnit.DAYS);
            ChangeUserStatusRequest request = new ChangeUserStatusRequest(
                    ChangeUserStatusRequest.Action.SUSPEND, "규정 위반", pastDate);

            // when & then
            assertThatThrownBy(() -> changeUserStatusService.changeUserStatus(targetUserId, request, currentUserId))
                    .isInstanceOf(InvalidSuspensionException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("마지막 ADMIN을 정지하려 하면 InvalidSuspensionException 발생")
        void suspend_LastAdmin_ThrowsException() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetAdmin = createAdminWithId(targetUserId);
            Instant suspendedUntil = Instant.now().plus(7, ChronoUnit.DAYS);
            ChangeUserStatusRequest request = new ChangeUserStatusRequest(
                    ChangeUserStatusRequest.Action.SUSPEND, "규정 위반", suspendedUntil);

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetAdmin));
            given(userRepository.countByRole(UserRole.ADMIN)).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> changeUserStatusService.changeUserStatus(targetUserId, request, currentUserId))
                    .isInstanceOf(InvalidSuspensionException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("ADMIN이 2명 이상이면 ADMIN 정지 가능")
        void suspend_AdminWithMultipleAdmins_Success() {
            // given
            Long targetUserId = 1L;
            Long currentUserId = 2L;
            User targetAdmin = createAdminWithId(targetUserId);
            Instant suspendedUntil = Instant.now().plus(7, ChronoUnit.DAYS);
            ChangeUserStatusRequest request = new ChangeUserStatusRequest(
                    ChangeUserStatusRequest.Action.SUSPEND, "규정 위반", suspendedUntil);

            given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetAdmin));
            given(userRepository.countByRole(UserRole.ADMIN)).willReturn(2L);

            // when
            changeUserStatusService.changeUserStatus(targetUserId, request, currentUserId);

            // then
            verify(userSuspensionRepository).save(any(UserSuspension.class));
            verify(eventPublisher).publishEvent(any(AccountStatusChangeEvent.class));
        }
    }

    @Nested
    @DisplayName("자동 정지 해제")
    class AutoLiftTest {

        @Test
        @DisplayName("만료된 정지가 있으면 해제하고 처리 건수를 반환한다")
        void liftExpiredSuspensions_WithExpired_LiftsAndReturnsCount() {
            // given
            User user = createMemberWithId(1L);
            user.suspend();
            UserSuspension suspension = UserSuspension.create(
                    user, "규정 위반",
                    Instant.now().minus(8, ChronoUnit.DAYS),
                    Instant.now().minus(1, ChronoUnit.DAYS),
                    2L
            );

            given(userSuspensionRepository.findExpiredButNotLifted(any()))
                    .willReturn(List.of(suspension));

            // when
            int count = changeUserStatusService.liftExpiredSuspensions();

            // then
            assertThat(count).isEqualTo(1);
            assertThat(suspension.isLifted()).isTrue();
            assertThat(suspension.getLiftedBy()).isNull();
            assertThat(user.isActive()).isTrue();
            verify(eventPublisher).publishEvent(any(AccountStatusChangeEvent.class));
        }

        @Test
        @DisplayName("만료된 정지가 없으면 0을 반환한다")
        void liftExpiredSuspensions_NoExpired_ReturnsZero() {
            // given
            given(userSuspensionRepository.findExpiredButNotLifted(any()))
                    .willReturn(List.of());

            // when
            int count = changeUserStatusService.liftExpiredSuspensions();

            // then
            assertThat(count).isEqualTo(0);
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("여러 만료된 정지가 있으면 모두 해제한다")
        void liftExpiredSuspensions_MultipleExpired_LiftsAll() {
            // given
            User user1 = createMemberWithId(1L);
            user1.suspend();
            UserSuspension suspension1 = UserSuspension.create(
                    user1, "규정 위반",
                    Instant.now().minus(8, ChronoUnit.DAYS),
                    Instant.now().minus(1, ChronoUnit.DAYS),
                    3L
            );

            User user2 = createMemberWithId(2L);
            user2.suspend();
            UserSuspension suspension2 = UserSuspension.create(
                    user2, "스팸",
                    Instant.now().minus(5, ChronoUnit.DAYS),
                    Instant.now().minus(2, ChronoUnit.DAYS),
                    3L
            );

            given(userSuspensionRepository.findExpiredButNotLifted(any()))
                    .willReturn(List.of(suspension1, suspension2));

            // when
            int count = changeUserStatusService.liftExpiredSuspensions();

            // then
            assertThat(count).isEqualTo(2);
            assertThat(suspension1.isLifted()).isTrue();
            assertThat(suspension2.isLifted()).isTrue();
            assertThat(user1.isActive()).isTrue();
            assertThat(user2.isActive()).isTrue();
            verify(eventPublisher, times(2)).publishEvent(any(AccountStatusChangeEvent.class));
        }

        @Test
        @DisplayName("감사 이벤트에 시스템 자동 해제 사유가 포함된다")
        void liftExpiredSuspensions_PublishesEventWithAutoLiftReason() {
            // given
            User user = createMemberWithId(1L);
            user.suspend();
            UserSuspension suspension = UserSuspension.create(
                    user, "규정 위반",
                    Instant.now().minus(8, ChronoUnit.DAYS),
                    Instant.now().minus(1, ChronoUnit.DAYS),
                    2L
            );

            given(userSuspensionRepository.findExpiredButNotLifted(any()))
                    .willReturn(List.of(suspension));

            // when
            changeUserStatusService.liftExpiredSuspensions();

            // then
            ArgumentCaptor<AccountStatusChangeEvent> captor =
                    ArgumentCaptor.forClass(AccountStatusChangeEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            AccountStatusChangeEvent event = captor.getValue();
            assertThat(event.userId()).isEqualTo(1L);
            assertThat(event.changedByUserId()).isNull();
            assertThat(event.previousValue()).isEqualTo("SUSPENDED");
            assertThat(event.newValue()).isEqualTo("ACTIVE");
            assertThat(event.reason()).isEqualTo("자동 정지 해제 (정지 기간 만료)");
        }
    }
}
