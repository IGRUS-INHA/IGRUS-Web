package igrus.web.event.service;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventChangeType;
import igrus.web.event.domain.EventRegistrationStatus;
import igrus.web.event.domain.EventStatus;
import igrus.web.event.domain.RegistrationStatus;
import igrus.web.event.dto.request.CreateEventRequest;
import igrus.web.event.dto.request.UpdateEventRequest;
import igrus.web.event.dto.response.EventCreateResponse;
import igrus.web.event.dto.response.EventDetailResponse;
import igrus.web.event.dto.response.EventListResponse;
import igrus.web.event.event.EventStatusChangeEvent;
import igrus.web.event.exception.AssociateMemberNotAllowedException;
import igrus.web.event.exception.EventAccessDeniedException;
import igrus.web.event.exception.EventNotFoundException;
import igrus.web.event.exception.EventRegistrationNotReopenableException;
import igrus.web.event.exception.InvalidEventDateException;
import igrus.web.event.repository.EventRepository;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * 행사 서비스.
 * 행사 관련 비즈니스 로직을 처리합니다.
 *
 * <p>제공 기능:</p>
 * <ul>
 *   <li>{@link #createEvent} - 행사 생성 (운영진 이상)</li>
 *   <li>{@link #getEvent} - 행사 단건 조회</li>
 *   <li>{@link #getEventList} - 행사 목록 조회 (상태 필터 가능)</li>
 *   <li>{@link #updateEvent} - 행사 수정 (운영진 이상)</li>
 *   <li>{@link #deleteEvent} - 행사 삭제 (운영진 이상)</li>
 *   <li>{@link #closeEvent} - 등록 수동 마감 (운영진 이상)</li>
 *   <li>{@link #cancelEvent} - 행사 취소 (운영진 이상)</li>
 *   <li>{@link #reactivateEvent} - 행사 재활성화 (운영진 이상)</li>
 *   <li>{@link #reopenRegistration} - 등록 수동 재오픈 (운영진 이상)</li>
 * </ul>
 *
 * @see EventRegistrationService 행사 신청 관련 기능
 */
@Transactional
@RequiredArgsConstructor
@Service
public class EventService {

    private static final Set<EventRegistrationStatus> ACTIVE_REGISTRATION_STATUSES = Set.of(
            EventRegistrationStatus.REGISTERED,
            EventRegistrationStatus.WAITING,
            EventRegistrationStatus.APPROVED
    );

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 행사를 생성합니다.
     *
     * @param request 행사 생성 요청 DTO
     * @param userId  생성자(운영진) ID
     * @return 생성된 행사 응답 DTO
     */
    public EventCreateResponse createEvent(CreateEventRequest request, Long userId) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 2. 권한 확인 (운영진 이상만 행사 생성 가능)
        validateOperatorPermission(user);

        // 3. 날짜 유효성 검증
        if (request.registrationStartAt().isBefore(Instant.now())) {
            throw new InvalidEventDateException("신청 시작일은 현재 시간 이후여야 합니다");
        }
        validateEventDates(request.eventStartAt(), request.eventEndAt(),
                request.registrationStartAt(), request.registrationEndAt());

        // 4. Event 도메인 객체 생성
        Event event = Event.create(
                user,
                request.title(),
                request.description(),
                request.location(),
                request.eventStartAt(),
                request.eventEndAt(),
                request.registrationStartAt(),
                request.registrationEndAt(),
                request.capacity(),
                request.registrationType()
        );

        // 5. 저장
        Event savedEvent = eventRepository.save(event);

        // 6. 응답 DTO 반환
        return EventCreateResponse.from(savedEvent);
    }

    /**
     * 행사를 단건 조회합니다.
     * 조회 시 현재 시간에 따라 행사 상태가 자동 갱신됩니다. (Lazy Evaluation)
     *
     * @param eventId 행사 ID
     * @param userId  현재 사용자 ID
     * @return 행사 상세 응답 DTO
     * @throws EventNotFoundException             행사를 찾을 수 없는 경우
     * @throws AssociateMemberNotAllowedException 준회원인 경우
     */
    public EventDetailResponse getEvent(Long eventId, Long userId) {
        Event event = eventRepository.findByIdAndNotDeleted(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 시간에 따른 상태 자동 갱신 (Lazy Evaluation)
        event.updateStatusIfNeeded(Instant.now());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 준회원 접근 제한
        if (user.isAssociate()) {
            throw new AssociateMemberNotAllowedException();
        }

        // 권한 정보 계산
        boolean canEdit = user.isOperatorOrAbove();
        boolean isRegistered = eventRegistrationRepository.existsByEventIdAndUserIdAndStatusIn(
                eventId, userId, ACTIVE_REGISTRATION_STATUSES);

        return EventDetailResponse.from(event, canEdit, isRegistered);
    }

    /**
     * 행사 목록을 조회합니다.
     * 조회 시 현재 시간에 따라 행사 상태가 자동 갱신됩니다. (Lazy Evaluation)
     *
     * @param eventStatus        행사 진행 상태 필터 (null이면 필터 안 함)
     * @param registrationStatus 등록 상태 필터 (null이면 필터 안 함)
     * @return 행사 목록 응답 DTO 리스트
     */
    public List<EventListResponse> getEventList(EventStatus eventStatus, RegistrationStatus registrationStatus) {
        List<Event> events;

        if (eventStatus != null) {
            events = eventRepository.findByEventStatus(eventStatus);
        } else if (registrationStatus != null) {
            events = eventRepository.findByRegistrationStatus(registrationStatus);
        } else {
            events = eventRepository.findAllNotDeleted();
        }

        // 각 행사의 상태를 시간에 따라 자동 갱신 (Lazy Evaluation)
        Instant now = Instant.now();
        events.forEach(event -> event.updateStatusIfNeeded(now));

        // Lazy 갱신 후 상태가 변경되었을 수 있으므로, 필터가 있으면 다시 적용
        if (eventStatus != null) {
            events = events.stream()
                    .filter(e -> e.getEventStatus() == eventStatus)
                    .toList();
        }
        if (registrationStatus != null) {
            events = events.stream()
                    .filter(e -> e.getRegistrationStatus() == registrationStatus)
                    .toList();
        }

        return events.stream()
                .map(EventListResponse::from)
                .toList();
    }

    /**
     * 행사를 수정합니다.
     *
     * @param eventId 행사 ID
     * @param request 행사 수정 요청 DTO
     * @param userId  수정자 ID
     * @return 수정된 행사 상세 응답 DTO
     * @throws EventNotFoundException       행사를 찾을 수 없는 경우
     * @throws EventAccessDeniedException   권한이 없는 경우
     */
    public EventDetailResponse updateEvent(Long eventId, UpdateEventRequest request, Long userId) {
        // 1. 행사 조회
        Event event = eventRepository.findByIdAndNotDeleted(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. 권한 확인 (운영진 이상만 수정 가능)
        validateEditPermission(user);

        // 4. Lazy Evaluation (EVT-INV-07: 상태별 수정 정책 적용 전 상태 갱신)
        event.updateStatusIfNeeded(Instant.now());

        // 5. 날짜 유효성 검증
        validateEventDates(request.eventStartAt(), request.eventEndAt(),
                request.registrationStartAt(), request.registrationEndAt());

        // 6. 행사 수정 (도메인 메서드 호출)
        event.update(
                request.title(),
                request.description(),
                request.location(),
                request.eventStartAt(),
                request.eventEndAt(),
                request.registrationStartAt(),
                request.registrationEndAt(),
                request.capacity()
        );

        // 7. 응답 반환 (dirty checking으로 자동 저장)
        return EventDetailResponse.from(event);
    }

    /**
     * 행사를 삭제합니다.
     *
     * @param eventId 행사 ID
     * @param userId  삭제자 ID
     * @throws EventNotFoundException       행사를 찾을 수 없는 경우
     * @throws EventAccessDeniedException   권한이 없는 경우
     */
    public void deleteEvent(Long eventId, Long userId) {
        // 1. 행사 조회
        Event event = eventRepository.findByIdAndNotDeleted(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. 권한 확인 (운영진 이상만 삭제 가능)
        validateEditPermission(user);

        // 4. Soft Delete 실행
        event.delete(userId);
    }

    /**
     * 등록을 수동으로 마감합니다.
     *
     * @param eventId 행사 ID
     * @param userId  마감 요청자 ID
     * @param reason  마감 사유
     * @return 마감된 행사 상세 응답 DTO
     * @throws EventNotFoundException       행사를 찾을 수 없는 경우
     * @throws EventAccessDeniedException   권한이 없는 경우
     */
    public EventDetailResponse closeEvent(Long eventId, Long userId, String reason) {
        // 1. 행사 조회
        Event event = eventRepository.findByIdAndNotDeleted(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. 권한 확인 (운영진 이상만 마감 가능)
        validateEditPermission(user);

        // 4. Lazy Evaluation (상태 갱신 후 마감 처리)
        event.updateStatusIfNeeded(Instant.now());

        // 5. 등록 마감 (도메인 메서드 호출)
        String previousRegStatus = event.getRegistrationStatus().name();
        event.closeRegistrationManually();

        // 6. 감사 이력 이벤트 발행
        eventPublisher.publishEvent(new EventStatusChangeEvent(
                eventId, userId, EventChangeType.REGISTRATION_CLOSED_MANUAL,
                previousRegStatus, event.getRegistrationStatus().name(), reason));

        // 7. 응답 반환
        return EventDetailResponse.from(event);
    }

    /**
     * 행사를 취소합니다.
     *
     * @param eventId 행사 ID
     * @param userId  취소 요청자 ID
     * @param reason  취소 사유
     * @return 취소된 행사 상세 응답 DTO
     * @throws EventNotFoundException                행사를 찾을 수 없는 경우
     * @throws InvalidEventStateTransitionException 취소 불가능한 상태인 경우
     */
    public EventDetailResponse cancelEvent(Long eventId, Long userId, String reason) {
        // 1. 행사 조회
        Event event = eventRepository.findByIdAndNotDeleted(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. 권한 확인 (운영진 이상만 취소 가능)
        validateEditPermission(user);

        // 4. Lazy Evaluation (EVT-INV-06: COMPLETED 종단 상태 체크를 위해 상태 갱신)
        event.updateStatusIfNeeded(Instant.now());

        // 5. 행사 취소 (도메인 메서드 호출)
        String previousEventStatus = event.getEventStatus().name();
        event.cancel();

        // 6. 감사 이력 이벤트 발행
        eventPublisher.publishEvent(new EventStatusChangeEvent(
                eventId, userId, EventChangeType.EVENT_CANCELED,
                previousEventStatus, event.getEventStatus().name(), reason));

        // 7. 응답 반환
        return EventDetailResponse.from(event);
    }

    /**
     * 취소된 행사를 재활성화합니다.
     *
     * @param eventId 행사 ID
     * @param userId  재활성화 요청자 ID
     * @param reason  재활성화 사유
     * @return 재활성화된 행사 상세 응답 DTO
     * @throws EventNotFoundException                행사를 찾을 수 없는 경우
     * @throws InvalidEventStateTransitionException 재활성화 불가능한 상태인 경우
     */
    public EventDetailResponse reactivateEvent(Long eventId, Long userId, String reason) {
        // 1. 행사 조회
        Event event = eventRepository.findByIdAndNotDeleted(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. 권한 확인 (운영진 이상만 재활성화 가능)
        validateEditPermission(user);

        // 4. 행사 재활성화 (도메인 메서드 호출)
        String previousEventStatus = event.getEventStatus().name();
        event.reactivate(Instant.now());

        // 5. 감사 이력 이벤트 발행
        eventPublisher.publishEvent(new EventStatusChangeEvent(
                eventId, userId, EventChangeType.EVENT_REACTIVATED,
                previousEventStatus, event.getEventStatus().name(), reason));

        // 6. 응답 반환
        return EventDetailResponse.from(event);
    }

    /**
     * 등록을 수동으로 재오픈합니다. (EVT-INV-13)
     * 5가지 조건 충족 시에만 재오픈 가능:
     * 1. registrationStatus == CLOSED
     * 2. eventStatus ∈ {UPCOMING, ONGOING}
     * 3. currentCount < capacity
     * 4. now < registrationEndAt (마감일 미경과)
     * 5. OPERATOR+ 권한 + 사유 필수
     *
     * @param eventId 행사 ID
     * @param userId  재오픈 요청자 ID
     * @param reason  재오픈 사유
     * @return 재오픈된 행사 상세 응답 DTO
     * @throws EventNotFoundException                  행사를 찾을 수 없는 경우
     * @throws EventRegistrationNotReopenableException 재오픈 불가능한 경우
     */
    public EventDetailResponse reopenRegistration(Long eventId, Long userId, String reason) {
        // 1. 행사 조회
        Event event = eventRepository.findByIdAndNotDeleted(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. 권한 확인 (운영진 이상만 재오픈 가능)
        validateEditPermission(user);

        // 4. Lazy Evaluation (EVT-INV-13 조건 판단 전 상태 갱신)
        Instant now = Instant.now();
        event.updateStatusIfNeeded(now);

        // 5. EVT-INV-13 조건 검증
        if (event.getRegistrationStatus() != RegistrationStatus.CLOSED) {
            throw new EventRegistrationNotReopenableException("등록이 마감 상태가 아닙니다.");
        }

        if (event.getEventStatus() != EventStatus.UPCOMING && event.getEventStatus() != EventStatus.ONGOING) {
            throw new EventRegistrationNotReopenableException("행사가 취소되었거나 완료된 상태에서는 재오픈할 수 없습니다.");
        }

        if (event.isFull()) {
            throw new EventRegistrationNotReopenableException("정원이 가득 찬 상태에서는 재오픈할 수 없습니다.");
        }

        if (now.isAfter(event.getRegistrationEndAt())) {
            throw new EventRegistrationNotReopenableException("등록 마감일이 경과하여 재오픈할 수 없습니다.");
        }

        // 6. 등록 재오픈 (도메인 메서드 호출)
        String previousRegStatus = event.getRegistrationStatus().name();
        event.reopenRegistration();

        // 7. 감사 이력 이벤트 발행 (EVT-INV-14)
        eventPublisher.publishEvent(new EventStatusChangeEvent(
                eventId, userId, EventChangeType.REGISTRATION_REOPENED,
                previousRegStatus, event.getRegistrationStatus().name(), reason));

        // 8. 응답 반환
        return EventDetailResponse.from(event);
    }

    /**
     * 운영진 이상 권한을 검증합니다.
     * 행사 생성 시 사용.
     *
     * @param user 사용자
     * @throws EventAccessDeniedException 권한이 없을 경우
     */
    private void validateOperatorPermission(User user) {
        if (!user.isOperatorOrAbove()) {
            throw new EventAccessDeniedException("행사 생성은 운영진 이상만 가능합니다");
        }
    }

    /**
     * 행사 수정/삭제 권한을 검증합니다.
     * 운영진(OPERATOR) 이상만 수정/삭제 가능.
     *
     * @param user  사용자
     * @throws EventAccessDeniedException 권한이 없을 경우
     */
    private void validateEditPermission(User user) {
        if (!user.isOperatorOrAbove()) {
            throw new EventAccessDeniedException("행사 수정/삭제는 운영진 이상만 가능합니다");
        }
    }

    /**
     * 행사 날짜 유효성을 검증합니다. (내부 공통 로직)
     * 2축 상태 모델 날짜 검증:
     * - regStart < regEnd
     * - regStart < eventStart (등록 시작은 행사 시작 전)
     * - regEnd <= eventEnd (등록 마감은 행사 종료 이전 또는 동일)
     * - eventStart < eventEnd
     *
     * @param eventStart 행사 시작일
     * @param eventEnd   행사 종료일
     * @param regStart   신청 시작일
     * @param regEnd     신청 마감일
     * @throws InvalidEventDateException 날짜 조건이 맞지 않을 경우
     */
    private void validateEventDates(Instant eventStart, Instant eventEnd, Instant regStart, Instant regEnd) {
        // 신청 시작일 < 신청 마감일
        if (!regStart.isBefore(regEnd)) {
            throw new InvalidEventDateException("신청 마감일은 신청 시작일 이후여야 합니다");
        }

        // 신청 시작일 < 행사 시작일 (등록 시작은 행사 시작 전이어야 함)
        if (!regStart.isBefore(eventStart)) {
            throw new InvalidEventDateException("신청 시작일은 행사 시작일 이전이어야 합니다");
        }

        // 신청 마감일 <= 행사 종료일
        if (regEnd.isAfter(eventEnd)) {
            throw new InvalidEventDateException("신청 마감일은 행사 종료일 이전이거나 같아야 합니다");
        }

        // 행사 시작일 <= 행사 종료일 (non-strict, EVT-INV-02)
        if (eventStart.isAfter(eventEnd)) {
            throw new InvalidEventDateException("행사 종료일은 시작일 이후이거나 같아야 합니다");
        }
    }

}
