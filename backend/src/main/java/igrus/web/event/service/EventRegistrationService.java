package igrus.web.event.service;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationStatus;
import igrus.web.event.domain.EventStatus;
import igrus.web.event.dto.response.MyRegistrationResponse;
import igrus.web.event.dto.response.RegistrationListResponse;
import igrus.web.event.dto.response.RegistrationResponse;
import igrus.web.event.exception.AlreadyRegisteredException;
import igrus.web.event.exception.EventAccessDeniedException;
import igrus.web.event.exception.EventCapacityFullException;
import igrus.web.event.exception.EventNotFoundException;
import igrus.web.event.exception.EventRegistrationClosedException;
import igrus.web.event.exception.EventRegistrationNotFoundException;
import igrus.web.event.repository.EventRepository;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 행사 신청 서비스.
 * 행사 신청 관련 비즈니스 로직을 처리합니다.
 *
 * <p>제공 기능:</p>
 * <ul>
 *   <li>{@link #registerEvent} - 행사 신청 (정회원 이상)</li>
 *   <li>{@link #cancelRegistration} - 신청 취소 (신청자 본인)</li>
 *   <li>{@link #getMyRegistrations} - 내 신청 목록 조회</li>
 *   <li>{@link #getRegistrationList} - 신청자 목록 조회 (작성자/관리자)</li>
 *   <li>{@link #approveRegistration} - 신청 승인 - 선발제 (작성자/관리자)</li>
 *   <li>{@link #rejectRegistration} - 신청 거절 - 선발제 (작성자/관리자)</li>
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
     * @param eventId 행사 ID
     * @param userId  신청자 ID
     * @return 신청 결과 응답 DTO
     * @throws EventNotFoundException           행사를 찾을 수 없는 경우
     * @throws UserNotFoundException            사용자를 찾을 수 없는 경우
     * @throws EventAccessDeniedException       준회원인 경우
     * @throws AlreadyRegisteredException       이미 신청한 경우
     * @throws EventRegistrationClosedException 신청 기간이 아니거나 행사가 OPEN 상태가 아닌 경우
     * @throws EventCapacityFullException       정원이 초과된 경우 (선착순)
     */
    public RegistrationResponse registerEvent(Long eventId, Long userId) {
        // 1. 행사 조회 (비관적 락으로 동시성 제어)
        Event event = eventRepository.findByIdWithLock(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. 권한 확인 (정회원 이상)
        if (user.isAssociate()) {
            throw new EventAccessDeniedException("준회원은 행사에 신청할 수 없습니다");
        }

        // 4. 기존 신청 기록 확인 (재신청 여부 판단)
        var existingRegistration = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId);
        if (existingRegistration.isPresent()) {
            return handleReRegistration(existingRegistration.get(), event);
        }

        // 5. 행사 상태 확인 (OPEN 상태인지)
        validateEventIsOpen(event);

        // 6. 신청 기간 확인
        validateRegistrationPeriod(event);

        // 7. 정원 확인 (선착순인 경우에만)
        if (event.isAutoApprove() && event.isFull()) {
            throw new EventCapacityFullException();
        }

        // 8. 신청 생성 및 저장
        EventRegistration registration = EventRegistration.create(event, user);
        EventRegistration savedRegistration = eventRegistrationRepository.save(registration);

        // 9. 행사 신청자 수 증가 (선착순인 경우)
        if (event.isAutoApprove()) {
            event.incrementCurrentCount();
        }

        // 10. 응답 반환
        return RegistrationResponse.from(savedRegistration);
    }

    /**
     * 신청을 취소합니다.
     *
     * @param eventId 행사 ID
     * @param userId  신청자 ID (본인)
     * @return 취소된 신청 응답 DTO
     * @throws EventNotFoundException             행사를 찾을 수 없는 경우
     * @throws EventRegistrationNotFoundException 신청을 찾을 수 없는 경우
     */
    public RegistrationResponse cancelRegistration(Long eventId, Long userId) {
        // 1. 행사 조회 (비관적 락으로 동시성 제어)
        Event event = eventRepository.findByIdWithLock(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. 신청 조회
        EventRegistration registration = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(EventRegistrationNotFoundException::new);

        // 3. 이미 취소된 신청인지 확인
        if (registration.isCanceled()) {
            throw new EventAccessDeniedException("이미 취소된 신청입니다");
        }

        // 4. 신청자 수 감소 여부 판단 (취소 전에 확인)
        // REGISTERED(선착순) 또는 APPROVED(선발제 승인) 상태였으면 카운트 감소
        boolean shouldDecrementCount = registration.isActive(); // 선착순 신청이 확정되어있거나 선발제가 승인된 상태라면 TRUE

        // 5. 신청 취소 (상태 변경)
        registration.cancel();

        // 6. 신청자 수 감소
        if (shouldDecrementCount) {
            event.decrementCurrentCount();
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
     * @throws EventNotFoundException       행사를 찾을 수 없는 경우
     * @throws UserNotFoundException        사용자를 찾을 수 없는 경우
     * @throws EventAccessDeniedException   권한이 없는 경우
     */
    @Transactional(readOnly = true)
    public List<RegistrationListResponse> getRegistrationList(Long eventId, Long userId) {
        // 1. 행사 조회
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. 권한 확인 (작성자 또는 관리자)
        validateEventOwnerOrAdmin(event, user);

        // 4. 신청자 목록 조회
        List<EventRegistration> registrations = eventRegistrationRepository.findByEventId(eventId);

        return registrations.stream()
                .map(RegistrationListResponse::from)
                .toList();
    }

    /**
     * 신청을 승인합니다. (선발제 전용)
     *
     * @param registrationId 신청 ID
     * @param userId         요청자 ID (작성자/관리자)
     * @return 승인된 신청 응답 DTO
     * @throws EventRegistrationNotFoundException 신청을 찾을 수 없는 경우
     * @throws UserNotFoundException              사용자를 찾을 수 없는 경우
     * @throws EventAccessDeniedException         권한이 없는 경우
     */
    public RegistrationResponse approveRegistration(Long registrationId, Long userId) {
        // 1. 신청 조회
        EventRegistration registration = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(EventRegistrationNotFoundException::new);

        // 2. 행사 조회 (비관적 락으로 동시성 제어)
        Event event = eventRepository.findByIdWithLock(registration.getEvent().getId())
                .orElseThrow(() -> new EventNotFoundException(registration.getEvent().getId()));

        // 3. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 4. 권한 확인 (작성자 또는 관리자)
        validateEventOwnerOrAdmin(event, user);

        // 5. 선발제 행사인지 확인
        if (!event.isManualApprove()) {
            throw new EventAccessDeniedException("선발제 행사만 승인할 수 있습니다");
        }

        // 6. WAITING 상태인지 확인
        if (registration.getStatus() != EventRegistrationStatus.WAITING) {
            throw new EventAccessDeniedException("대기 중인 신청만 승인할 수 있습니다");
        }

        // 7. 정원 확인
        if (event.isFull()) {
            throw new EventCapacityFullException();
        }

        // 8. 승인 처리
        registration.approve();

        // 9. 신청자 수 증가
        event.incrementCurrentCount();

        // 10. 응답 반환
        return RegistrationResponse.from(registration);
    }

    /**
     * 신청을 거절합니다. (선발제 전용)
     *
     * @param registrationId 신청 ID
     * @param userId         요청자 ID (작성자/관리자)
     * @return 거절된 신청 응답 DTO
     * @throws EventRegistrationNotFoundException 신청을 찾을 수 없는 경우
     * @throws UserNotFoundException              사용자를 찾을 수 없는 경우
     * @throws EventAccessDeniedException         권한이 없는 경우
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

        // 4. 권한 확인 (작성자 또는 관리자)
        validateEventOwnerOrAdmin(event, user);

        // 5. 선발제 행사인지 확인
        if (!event.isManualApprove()) {
            throw new EventAccessDeniedException("선발제 행사만 거절할 수 있습니다");
        }

        // 6. WAITING 상태인지 확인
        if (registration.getStatus() != EventRegistrationStatus.WAITING) {
            throw new EventAccessDeniedException("대기 중인 신청만 거절할 수 있습니다");
        }

        // 7. 거절 처리
        registration.reject();

        // 8. 응답 반환
        return RegistrationResponse.from(registration);
    }

    // === Private 메서드 ===

    /**
     * 행사 작성자 또는 관리자 권한을 검증합니다.
     *
     * @param event 행사
     * @param user  사용자
     * @throws EventAccessDeniedException 권한이 없을 경우
     */
    private void validateEventOwnerOrAdmin(Event event, User user) {
        boolean isOwner = event.getUser().getId().equals(user.getId());
        boolean isAdmin = user.isAdmin();

        if (!isOwner && !isAdmin) {
            throw new EventAccessDeniedException("행사 작성자 또는 관리자만 접근할 수 있습니다");
        }
    }

    /**
     * 재신청을 처리합니다.
     * 취소된 신청만 재신청 가능합니다.
     *
     * @param registration 기존 신청 기록
     * @param event        행사
     * @return 재신청 결과 응답 DTO
     * @throws AlreadyRegisteredException       취소 상태가 아닌 경우
     * @throws EventRegistrationClosedException 신청 불가 상태인 경우
     * @throws EventCapacityFullException       정원 초과인 경우
     */
    private RegistrationResponse handleReRegistration(EventRegistration registration, Event event) {
        // 취소 상태가 아니면 이미 신청 중
        if (!registration.isCanceled()) {
            throw new AlreadyRegisteredException();
        }

        // 행사 상태 확인
        validateEventIsOpen(event);

        // 신청 기간 확인
        validateRegistrationPeriod(event);

        // 정원 확인 (선착순인 경우)
        if (event.isAutoApprove() && event.isFull()) {
            throw new EventCapacityFullException();
        }

        // 재신청 처리
        registration.reRegister();

        // 신청자 수 증가 (선착순인 경우)
        if (event.isAutoApprove()) {
            event.incrementCurrentCount();
        }

        return RegistrationResponse.from(registration);
    }

    /**
     * 행사가 신청 가능한 상태인지 검증합니다.
     *
     * @param event 행사
     * @throws EventRegistrationClosedException OPEN 상태가 아닌 경우
     */
    private void validateEventIsOpen(Event event) {
        if (event.getStatus() != EventStatus.OPEN) {
            throw new EventRegistrationClosedException("신청 가능한 상태가 아닙니다");
        }
    }

    /**
     * 신청 기간 내인지 검증합니다.
     *
     * @param event 행사
     * @throws EventRegistrationClosedException 신청 기간이 아닌 경우
     */
    private void validateRegistrationPeriod(Event event) {
        Instant now = Instant.now();
        if (now.isBefore(event.getRegistrationStartAt()) || now.isAfter(event.getRegistrationEndAt())) {
            throw new EventRegistrationClosedException("신청 기간이 아닙니다");
        }
    }

}
