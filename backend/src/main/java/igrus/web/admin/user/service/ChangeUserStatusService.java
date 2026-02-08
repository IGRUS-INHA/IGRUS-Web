package igrus.web.admin.user.service;

import igrus.web.admin.user.dto.ChangeUserStatusRequest;
import igrus.web.admin.user.exception.SelfStatusChangeException;
import igrus.web.common.exception.ErrorCode;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserSuspension;
import igrus.web.user.exception.InvalidSuspensionException;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.repository.UserSuspensionRepository;
import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.event.AccountStatusChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChangeUserStatusService {

    private final UserRepository userRepository;
    private final UserSuspensionRepository userSuspensionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void changeUserStatus(Long targetUserId, ChangeUserStatusRequest request, Long currentUserId) {
        // 1. 자기 자신 상태 변경 방지
        if (targetUserId.equals(currentUserId)) {
            throw new SelfStatusChangeException();
        }

        // 2. action에 따라 정지 또는 해제 분기
        switch (request.action()) {
            case SUSPEND -> suspend(targetUserId, request, currentUserId); // 정지
            case LIFT -> lift(targetUserId, currentUserId); // 해제
        }
    }

    private void suspend(Long targetUserId, ChangeUserStatusRequest request, Long currentUserId) {
        // 1. 입력값 검증 (사유 필수, 정지 종료일 필수)
        if (request.reason() == null || request.reason().isBlank()) {
            throw InvalidSuspensionException.reasonRequired();
        }
        if (request.suspendedUntil() == null) {
            throw new InvalidSuspensionException(ErrorCode.SUSPENSION_INVALID_PERIOD);
        }

        // 2. 정지 종료일이 현재 시간 이후인지 검증
        if (!request.suspendedUntil().isAfter(Instant.now())) {
            throw InvalidSuspensionException.endDateMustBeFuture();
        }

        // 3. 대상 사용자 조회
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        // 4. 마지막 ADMIN인 경우 정지 불가
        if (targetUser.isAdmin()) {
            long adminCount = userRepository.countByRole(UserRole.ADMIN);
            if (adminCount <= 1) {
                throw InvalidSuspensionException.lastAdminCannotSuspend();
            }
        }

        // 5. 이미 정지된 사용자인지 확인
        if (targetUser.isSuspended()) {
            throw new InvalidSuspensionException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        // 6. 사용자 상태를 SUSPENDED로 변경
        String previousStatus = targetUser.getStatus().name();
        targetUser.suspend();

        // 7. 정지 기록 생성 및 저장
        UserSuspension suspension = UserSuspension.create(
                targetUser, request.reason(), request.suspendedUntil(), currentUserId);
        userSuspensionRepository.save(suspension);

        // 8. 감사 이벤트 발행
        eventPublisher.publishEvent(new AccountStatusChangeEvent(
                targetUserId, currentUserId, AccountChangeType.SUSPENSION,
                previousStatus, UserStatus.SUSPENDED.name(),
                request.reason()
        ));
    }

    /**
     * 만료된 정지를 자동으로 해제합니다.
     * 스케줄러에서 호출되며, 관리자가 설정한 정지 종료 시각이 지난 정지를 해제합니다.
     *
     * @return 처리된 정지 건수
     */
    public int liftExpiredSuspensions() {
        // 1. 정지 종료 시각(suspendedUntil)이 지났지만 아직 해제(liftedAt)되지 않은 정지 조회
        List<UserSuspension> expiredSuspensions =
                userSuspensionRepository.findExpiredButNotLifted(Instant.now());

        int count = 0;
        for (UserSuspension suspension : expiredSuspensions) {
            User user = suspension.getUser();
            String previousStatus = user.getStatus().name();

            // 2. 정지 해제 처리 (시스템 자동 해제이므로 liftedBy = null)
            suspension.lift(null);

            // 3. 사용자 상태를 ACTIVE로 복원
            user.activate();

            // 4. 감사 이벤트 발행 (수동 해제와 구분하기 위해 changedByUserId = null, 사유 명시)
            eventPublisher.publishEvent(new AccountStatusChangeEvent(
                    user.getId(), null, AccountChangeType.SUSPENSION_LIFT,
                    previousStatus, UserStatus.ACTIVE.name(),
                    "자동 정지 해제 (정지 기간 만료)"
            ));

            log.info("자동 정지 해제 완료: userId={}", user.getId());
            count++;
        }

        return count;
    }

    private void lift(Long targetUserId, Long currentUserId) {
        // 1. 대상 사용자 조회
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        // 2. 활성 정지 기록 조회 (없으면 이미 해제된 상태)
        UserSuspension suspension = userSuspensionRepository
                .findActiveByUserId(targetUserId, Instant.now())
                .orElseThrow(InvalidSuspensionException::alreadyLifted);

        // 3. 정지 해제 처리 + 사용자 상태를 ACTIVE로 복원
        String previousStatus = targetUser.getStatus().name();
        suspension.lift(currentUserId);
        targetUser.activate();

        // 4. 감사 이벤트 발행
        eventPublisher.publishEvent(new AccountStatusChangeEvent(
                targetUserId, currentUserId, AccountChangeType.SUSPENSION_LIFT,
                previousStatus, UserStatus.ACTIVE.name(),
                null
        ));
    }
}
