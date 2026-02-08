package igrus.web.event.service;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistrationStatus;
import igrus.web.event.dto.request.CreateEventRequest;
import igrus.web.event.dto.request.UpdateEventRequest;
import igrus.web.event.dto.response.EventCreateResponse;
import igrus.web.event.dto.response.EventDetailResponse;
import igrus.web.event.dto.response.EventListResponse;
import igrus.web.event.domain.EventStatus;
import igrus.web.event.exception.AssociateMemberNotAllowedException;
import igrus.web.event.exception.EventAccessDeniedException;
import igrus.web.event.exception.EventNotFoundException;
import igrus.web.event.exception.InvalidEventDateException;
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
 *   <li>{@link #closeEvent} - 행사 수동 마감 (운영진 이상)</li>
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
        // 수정 권한이 있는가
        boolean canEdit = user.isOperatorOrAbove(); // 운영진, 관리자
        // 신청 vs 신청 취소 버튼 분기용
        boolean isRegistered = eventRegistrationRepository.existsByEventIdAndUserIdAndStatusIn(
                eventId, userId, ACTIVE_REGISTRATION_STATUSES);

        return EventDetailResponse.from(event, canEdit, isRegistered);
    }

    /**
     * 행사 목록을 조회합니다.
     * 조회 시 현재 시간에 따라 행사 상태가 자동 갱신됩니다. (Lazy Evaluation)
     *
     * @param status 행사 상태 필터 (null이면 전체 조회)
     * @return 행사 목록 응답 DTO 리스트
     */
    public List<EventListResponse> getEventList(EventStatus status) {
        List<Event> events;

        // 상태 필터가 없으면 전체 조회, 있으면 해당 상태만 조회
        if (status == null) {
            events = eventRepository.findAllNotDeleted();
        } else {
            events = eventRepository.findByStatusAndNotDeleted(status);
        }

        // 각 행사의 상태를 시간에 따라 자동 갱신 (Lazy Evaluation)
        Instant now = Instant.now();
        events.forEach(event -> event.updateStatusIfNeeded(now));

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

        // 4. 날짜 유효성 검증
        validateEventDates(request.eventStartAt(), request.eventEndAt(),
                request.registrationStartAt(), request.registrationEndAt());

        // 5. 행사 수정 (도메인 메서드 호출)
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

        // 6. 응답 반환 (dirty checking으로 자동 저장)
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
     * 행사를 수동으로 마감합니다.
     *
     * @param eventId 행사 ID
     * @param userId  마감 요청자 ID
     * @return 마감된 행사 상세 응답 DTO
     * @throws EventNotFoundException       행사를 찾을 수 없는 경우
     * @throws EventAccessDeniedException   권한이 없는 경우
     */
    public EventDetailResponse closeEvent(Long eventId, Long userId) {
        // 1. 행사 조회
        Event event = eventRepository.findByIdAndNotDeleted(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. 권한 확인 (운영진 이상만 마감 가능)
        validateEditPermission(user);

        // 4. 행사 마감 (도메인 메서드 호출)
        event.closeManually();

        // 5. 응답 반환
        return EventDetailResponse.from(event);
    }

    /**
<<<<<<< HEAD
=======
     * 행사를 취소합니다.
     *
     * @param eventId 행사 ID
     * @param userId  취소 요청자 ID
     * @return 취소된 행사 상세 응답 DTO
     * @throws EventNotFoundException       행사를 찾을 수 없는 경우
     * @throws EventAccessDeniedException   권한이 없는 경우
     */
    public EventDetailResponse cancelEvent(Long eventId, Long userId) {
        // 1. 행사 조회
        Event event = eventRepository.findByIdAndNotDeleted(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. 권한 확인 (운영진 이상만 취소 가능)
        validateEditPermission(user);

        // 4. 행사 취소 (도메인 메서드 호출)
        event.cancel();

        // 5. 응답 반환
        return EventDetailResponse.from(event);
    }

    /**
>>>>>>> dev
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
     *
     * @param eventStart 행사 시작일
     * @param eventEnd   행사 종료일
     * @param regStart   신청 시작일
     * @param regEnd     신청 마감일
     * @throws InvalidEventDateException 날짜 조건이 맞지 않을 경우
     */
    private void validateEventDates(Instant eventStart, Instant eventEnd, Instant regStart, Instant regEnd) {
        // 신청 시작일 < 신청 마감일
        if (regStart.isAfter(regEnd)) {
            throw new InvalidEventDateException("신청 마감일은 신청 시작일 이후여야 합니다");
        }

        // 신청 마감일 < 행사 시작일 (신청이 끝난 후 행사 시작)
        if (!regEnd.isBefore(eventStart)) {
            throw new InvalidEventDateException("신청 마감일은 행사 시작일 이전이어야 합니다");
        }

        // 행사 시작일 <= 행사 종료일
        if (eventStart.isAfter(eventEnd)) {
            throw new InvalidEventDateException("행사 종료일은 시작일 이후여야 합니다");
        }


    }

}
