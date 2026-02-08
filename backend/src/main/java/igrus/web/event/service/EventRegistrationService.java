package igrus.web.event.service;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationStatus;
import igrus.web.event.domain.EventStatus;
import igrus.web.event.dto.response.MyRegistrationResponse;
import igrus.web.event.dto.response.RegistrationListResponse;
import igrus.web.event.dto.response.RegistrationResponse;
import igrus.web.event.exception.AlreadyCanceledException;
import igrus.web.event.exception.AlreadyRegisteredException;
import igrus.web.event.exception.AssociateMemberNotAllowedException;
import igrus.web.event.exception.EventCapacityFullException;
import igrus.web.event.exception.EventNotFoundException;
import igrus.web.event.exception.EventRegistrationClosedException;
import igrus.web.event.exception.EventNotInRegistrationPeriodException;
import igrus.web.event.exception.EventNotOpenException;
import igrus.web.event.exception.EventRegistrationNotFoundException;
import igrus.web.event.exception.InvalidRegistrationStatusException;
import igrus.web.event.exception.NotManualApproveEventException;
import igrus.web.event.exception.EventTimeOverlapException;
import igrus.web.event.exception.OperatorPermissionRequiredException;
import igrus.web.event.repository.EventRepository;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * 행사 신청 서비스.
 * 행사 신청 관련 비즈니스 로직을 처리합니다.
 *
 * <p>제공 기능:</p>
 * <ul>
 *   <li>{@link #registerEvent} - 행사 신청 (정회원 이상)</li>
 *   <li>{@link #cancelRegistration} - 신청 취소 (신청자 본인)</li>
 *   <li>{@link #getMyRegistrations} - 내 신청 목록 조회</li>
 *   <li>{@link #getRegistrationList} - 신청자 목록 조회 (운영진 이상)</li>
 *   <li>{@link #approveRegistration} - 신청 승인 - 선발제 (운영진 이상)</li>
 *   <li>{@link #rejectRegistration} - 신청 거절 - 선발제 (운영진 이상)</li>
 * </ul>
 *
 * @see EventService 행사 CRUD 관련 기능
 */
@Transactional
@RequiredArgsConstructor
@Service
public class EventRegistrationService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final UserRepository userRepository;

    /**
     * 행사에 신청합니다.
     *
     * <p>검증 항목:</p>
     * <ul>
     *   <li>정회원 이상만 신청 가능</li>
     *   <li>중복 신청 불가</li>
     *   <li>행사가 OPEN 상태여야 함</li>
     *   <li>신청 기간 내여야 함</li>
     *   <li>정원이 남아있어야 함 (선착순의 경우)</li>
     * </ul>
     *
     * <p>동시성 제어: 원자적 UPDATE 방식 사용</p>
     *
     * @param eventId 행사 ID
     * @param userId  신청자 ID
     * @return 신청 결과 응답 DTO
     * @throws EventNotFoundException               행사를 찾을 수 없는 경우
     * @throws UserNotFoundException                사용자를 찾을 수 없는 경우
     * @throws AssociateMemberNotAllowedException   준회원인 경우
     * @throws AlreadyRegisteredException           이미 신청한 경우
     * @throws EventTimeOverlapException            다른 행사와 시간이 겹치는 경우
     * @throws EventNotOpenException                행사가 OPEN 상태가 아닌 경우
     * @throws EventNotInRegistrationPeriodException 신청 기간이 아닌 경우
     * @throws EventCapacityFullException           정원이 초과된 경우 (자동 승인)
     */
    public RegistrationResponse registerEvent(Long eventId, Long userId) {
        // 1. 행사 조회 (락 없이)
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. 권한 확인 (정회원 이상)
        if (user.isAssociate()) {
            throw new AssociateMemberNotAllowedException();
        }

        // 4. 기존 신청 기록 확인 (재신청 여부 판단)
        var existingRegistration = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId);
        if (existingRegistration.isPresent()) {
            return handleReRegistration(existingRegistration.get(), event, eventId);
        }

        // 5. 행사 상태 확인 (OPEN 상태인지)
        validateEventIsOpen(event);

        // 6. 신청 기간 확인
        validateRegistrationPeriod(event);

        // 7. 다른 행사와 시간 겹침 확인
        validateNoTimeOverlap(userId, event);

        // 8. 선착순인 경우: 원자적 UPDATE로 신청자 수 증가
        if (event.isAutoApprove()) {
            int updated = eventRepository.incrementCurrentCountIfAvailable(eventId);
            if (updated == 0) {
                throw new EventCapacityFullException();
            }
            // 정원이 찼으면 상태 변경
            updateEventStatusAfterIncrement(eventId);
        }

        // 9. 신청 생성 및 저장
        EventRegistration registration = EventRegistration.create(event, user);
        EventRegistration savedRegistration = eventRegistrationRepository.save(registration);

        // 10. 응답 반환
        return RegistrationResponse.from(savedRegistration);
    }

    /**
     * 신청을 취소합니다.
     *
     * <p>동시성 제어: 원자적 UPDATE 방식 사용</p>
     *
     * @param eventId 행사 ID
     * @param userId  신청자 ID (본인)
     * @return 취소된 신청 응답 DTO
     * @throws EventNotFoundException             행사를 찾을 수 없는 경우
     * @throws EventRegistrationNotFoundException 신청을 찾을 수 없는 경우
     */
    public RegistrationResponse cancelRegistration(Long eventId, Long userId) {
        // 1. 행사 존재 확인
        if (!eventRepository.existsById(eventId)) {
            throw new EventNotFoundException(eventId);
        }

        // 2. 신청 조회
        EventRegistration registration = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(EventRegistrationNotFoundException::new);

        // 3. 이미 취소된 신청인지 확인
        if (registration.isCanceled()) {
            throw new AlreadyCanceledException();
        }

        // 4. 신청자 수 감소 여부 판단 (취소 전에 확인)
        // REGISTERED(선착순) 또는 APPROVED(선발제 승인) 상태였으면 카운트 감소
        boolean shouldDecrementCount = registration.isActive();

        // 5. 신청 취소 (상태 변경)
        registration.cancel();

        // 6. 신청자 수 감소 (원자적 UPDATE)
        if (shouldDecrementCount) {
            eventRepository.decrementCurrentCount(eventId);
            // 자리가 생겼으면 상태 변경 (정원 마감 → OPEN)
            updateEventStatusAfterDecrement(eventId);
        }

        // 7. 응답 반환
        return RegistrationResponse.from(registration);
    }

    /**
     * 내 신청 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 내 신청 목록
     */
    @Transactional(readOnly = true)
    public List<MyRegistrationResponse> getMyRegistrations(Long userId) {
        List<EventRegistration> registrations = eventRegistrationRepository.findByUserId(userId);

        return registrations.stream()
                .map(MyRegistrationResponse::from)
                .toList();
    }

    /**
     * 행사 신청자 목록을 조회합니다. (작성자/관리자용)
     *
     * @param eventId 행사 ID
     * @param userId  요청자 ID
     * @return 신청자 목록
     * @throws EventNotFoundException               행사를 찾을 수 없는 경우
     * @throws UserNotFoundException                사용자를 찾을 수 없는 경우
     * @throws OperatorPermissionRequiredException  운영진 권한이 없는 경우
     */
    @Transactional(readOnly = true)
    public Page<RegistrationListResponse> getRegistrationList(Long eventId, Long userId, Pageable pageable) {
        // 1. 행사 조회
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. 권한 확인 (운영진 이상)
        validateOperatorPermission(user);

        // 4. 신청자 목록 조회 (페이징)
        Page<EventRegistration> registrations = eventRegistrationRepository.findByEventId(eventId, pageable);

        return registrations.map(RegistrationListResponse::from);
    }

    /**
     * 신청을 승인합니다. (선발제 전용)
     *
     * <p>동시성 제어: 원자적 UPDATE 방식 사용</p>
     *
     * @param registrationId 신청 ID
     * @param userId         요청자 ID (작성자/관리자)
     * @return 승인된 신청 응답 DTO
     * @throws EventRegistrationNotFoundException   신청을 찾을 수 없는 경우
     * @throws UserNotFoundException               사용자를 찾을 수 없는 경우
     * @throws OperatorPermissionRequiredException 운영진 권한이 없는 경우
     * @throws NotManualApproveEventException      수동 승인 행사가 아닌 경우
     * @throws InvalidRegistrationStatusException  대기 상태가 아닌 경우
     * @throws EventCapacityFullException          정원이 초과된 경우
     */
    public RegistrationResponse approveRegistration(Long registrationId, Long userId) {
        // 1. 신청 조회
        EventRegistration registration = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(EventRegistrationNotFoundException::new);

        Long eventId = registration.getEvent().getId();

        // 2. 행사 조회 (락 없이)
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 3. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 4. 권한 확인 (운영진 이상)
        validateOperatorPermission(user);

        // 5. 선발제 행사인지 확인
        if (!event.isManualApprove()) {
            throw new NotManualApproveEventException();
        }

        // 6. WAITING 상태인지 확인
        if (registration.getStatus() != EventRegistrationStatus.WAITING) {
            throw new InvalidRegistrationStatusException();
        }

        // 7. 원자적 UPDATE로 신청자 수 증가 (정원 체크 포함, 상태 체크 없음)
        // 선발제 승인은 신청 기간 종료 후에도 가능해야 하므로 상태와 관계없이 정원만 체크
        int updated = eventRepository.incrementCurrentCountForApproval(eventId);
        if (updated == 0) {
            throw new EventCapacityFullException();
        }

        // 8. 승인 처리
        registration.approve();

        // 9. 정원이 찼으면 상태 변경
        updateEventStatusAfterIncrement(eventId);

        // 10. 응답 반환
        return RegistrationResponse.from(registration);
    }

    /**
     * 신청을 거절합니다. (선발제 전용)
     *
     * @param registrationId 신청 ID
     * @param userId         요청자 ID (작성자/관리자)
     * @return 거절된 신청 응답 DTO
     * @throws EventRegistrationNotFoundException   신청을 찾을 수 없는 경우
     * @throws UserNotFoundException               사용자를 찾을 수 없는 경우
     * @throws OperatorPermissionRequiredException 운영진 권한이 없는 경우
     * @throws NotManualApproveEventException      수동 승인 행사가 아닌 경우
     * @throws InvalidRegistrationStatusException  대기 상태가 아닌 경우
     */
    public RegistrationResponse rejectRegistration(Long registrationId, Long userId) {
        // 1. 신청 조회
        EventRegistration registration = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(EventRegistrationNotFoundException::new);

        // 2. 행사 조회
        Event event = eventRepository.findById(registration.getEvent().getId())
                .orElseThrow(() -> new EventNotFoundException(registration.getEvent().getId()));

        // 3. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 4. 권한 확인 (운영진 이상)
        validateOperatorPermission(user);

        // 5. 선발제 행사인지 확인
        if (!event.isManualApprove()) {
            throw new NotManualApproveEventException();
        }

        // 6. WAITING 상태인지 확인
        if (registration.getStatus() != EventRegistrationStatus.WAITING) {
            throw new InvalidRegistrationStatusException();
        }

        // 7. 거절 처리
        registration.reject();

        // 8. 응답 반환
        return RegistrationResponse.from(registration);
    }

    /**
     * 승인 또는 거절을 되돌려 대기 상태로 복원합니다. (선발제 전용)
     *
     * <p>승인 상태에서 되돌릴 경우 신청자 수를 감소시킵니다.</p>
     *
     * @param registrationId 신청 ID
     * @param userId         요청자 ID (운영진)
     * @return 되돌린 신청 응답 DTO
     * @throws EventRegistrationNotFoundException   신청을 찾을 수 없는 경우
     * @throws UserNotFoundException               사용자를 찾을 수 없는 경우
     * @throws OperatorPermissionRequiredException 운영진 권한이 없는 경우
     * @throws NotManualApproveEventException      수동 승인 행사가 아닌 경우
     * @throws InvalidRegistrationStatusException  승인/거절 상태가 아닌 경우
     */
    public RegistrationResponse revertRegistration(Long registrationId, Long userId) {
        // 1. 신청 조회
        EventRegistration registration = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(EventRegistrationNotFoundException::new);

        Long eventId = registration.getEvent().getId();

        // 2. 행사 조회
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 3. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 4. 권한 확인 (운영진 이상)
        validateOperatorPermission(user);

        // 5. 선발제 행사인지 확인
        if (!event.isManualApprove()) {
            throw new NotManualApproveEventException();
        }

        // 6. APPROVED 또는 REJECTED 상태인지 확인
        if (!registration.isApproved() && !registration.isRejected()) {
            throw new InvalidRegistrationStatusException();
        }

        // 7. 승인 상태였으면 카운트 감소
        if (registration.isApproved()) {
            eventRepository.decrementCurrentCount(eventId);
            updateEventStatusAfterDecrement(eventId);
        }

        // 8. WAITING 상태로 되돌리기
        registration.revertToWaiting();

        // 9. 응답 반환
        return RegistrationResponse.from(registration);
    }

    // === Private 메서드 ===

    /**
     * 운영진 이상 권한을 검증합니다.
     * 신청자 목록 조회, 승인/거절 시 사용.
     *
     * @param user  사용자
     * @throws OperatorPermissionRequiredException 권한이 없을 경우
     */
    private void validateOperatorPermission(User user) {
        if (!user.isOperatorOrAbove()) {
            throw new OperatorPermissionRequiredException();
        }
    }

    /**
     * 재신청을 처리합니다.
     * 취소된 신청만 재신청 가능합니다.
     *
     * @param registration 기존 신청 기록
     * @param event        행사
     * @param eventId      행사 ID (원자적 UPDATE용)
     * @return 재신청 결과 응답 DTO
     * @throws AlreadyRegisteredException       취소 상태가 아닌 경우
     * @throws EventRegistrationClosedException 신청 불가 상태인 경우
     * @throws EventCapacityFullException       정원 초과인 경우
     */
    private RegistrationResponse handleReRegistration(EventRegistration registration, Event event, Long eventId) {
        // 취소 상태가 아니면 이미 신청 중
        if (!registration.isCanceled()) {
            throw new AlreadyRegisteredException();
        }

        // 행사 상태 확인
        validateEventIsOpen(event);

        // 신청 기간 확인
        validateRegistrationPeriod(event);

        // 다른 행사와 시간 겹침 확인
        validateNoTimeOverlap(registration.getUser().getId(), event);

        // 선착순인 경우: 원자적 UPDATE로 신청자 수 증가
        if (event.isAutoApprove()) {
            int updated = eventRepository.incrementCurrentCountIfAvailable(eventId);
            if (updated == 0) {
                throw new EventCapacityFullException();
            }
            // 정원이 찼으면 상태 변경
            updateEventStatusAfterIncrement(eventId);
        }

        // 재신청 처리
        registration.reRegister();

        return RegistrationResponse.from(registration);
    }

    /**
     * 행사가 신청 가능한 상태인지 검증합니다.
     *
     * @param event 행사
     * @throws EventNotOpenException OPEN 상태가 아닌 경우
     */
    private void validateEventIsOpen(Event event) {
        if (event.getStatus() != EventStatus.OPEN) {
            throw new EventNotOpenException();
        }
    }

    /**
     * 신청 기간 내인지 검증합니다.
     *
     * @param event 행사
     * @throws EventNotInRegistrationPeriodException 신청 기간이 아닌 경우
     */
    private void validateRegistrationPeriod(Event event) {
        Instant now = Instant.now();
        if (now.isBefore(event.getRegistrationStartAt()) || now.isAfter(event.getRegistrationEndAt())) {
            throw new EventNotInRegistrationPeriodException();
        }
    }

    /**
     * 사용자의 확정된 신청(REGISTERED, APPROVED) 중
     * 신청하려는 행사의 진행 시간과 겹치는 신청이 없는지 검증합니다.
     *
     * @param userId 사용자 ID
     * @param event  신청하려는 행사
     * @throws EventTimeOverlapException 시간이 겹치는 신청이 있는 경우
     */
    private void validateNoTimeOverlap(Long userId, Event event) {
        boolean hasOverlap = eventRegistrationRepository.existsOverlappingRegistration(
                userId,
                event.getEventStartAt(),
                event.getEventEndAt(),
                Set.of(EventRegistrationStatus.REGISTERED, EventRegistrationStatus.APPROVED)
        );
        if (hasOverlap) {
            throw new EventTimeOverlapException();
        }
    }

    /**
     * 신청자 수 증가 후 행사 상태를 업데이트합니다.
     * 정원이 찼으면 CLOSED 상태로 변경합니다.
     *
     * @param eventId 행사 ID
     */
    private void updateEventStatusAfterIncrement(Long eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event != null && event.isFull()) {
            event.closeByCapacity();
        }
    }

    /**
     * 신청자 수 감소 후 행사 상태를 업데이트합니다.
     * 정원 마감 상태에서 자리가 생기면 OPEN 상태로 변경합니다.
     *
     * @param eventId 행사 ID
     */
    private void updateEventStatusAfterDecrement(Long eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event != null) {
            event.reopenIfNeeded();
        }
    }

}
