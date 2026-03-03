package igrus.web.event.service;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationStatus;
import igrus.web.event.domain.EventStatus;
import igrus.web.event.domain.EventVisibility;
import igrus.web.event.domain.RegistrationStatus;
import igrus.web.event.dto.response.MyRegistrationResponse;
import igrus.web.event.dto.response.RegistrationListResponse;
import igrus.web.event.dto.response.RegistrationResponse;
import igrus.web.event.exception.AlreadyCanceledException;
import igrus.web.event.exception.AlreadyRegisteredException;
import igrus.web.event.exception.AssociateMemberNotAllowedException;
import igrus.web.event.exception.EventCapacityFullException;
import igrus.web.event.exception.EventNotEditableException;
import igrus.web.event.exception.EventNotFoundException;
import igrus.web.event.exception.EventNotInRegistrationPeriodException;
import igrus.web.event.exception.EventNotOpenException;
import igrus.web.event.exception.EventRegistrationNotFoundException;
import igrus.web.event.exception.InvalidRegistrationStatusException;
import igrus.web.event.exception.NotManualApproveEventException;
import igrus.web.event.exception.EventTimeOverlapException;
import igrus.web.event.exception.OperatorPermissionRequiredException;
import igrus.web.event.exception.SurveyNotReadyException;
import igrus.web.event.exception.SurveyResponseRequiredException;
import igrus.web.event.repository.EventRepository;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyResponseStatus;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.survey.response.domain.SurveyResponse;
import igrus.web.survey.response.dto.request.SubmitAnswerRequest;
import igrus.web.survey.response.exception.SurveyResponseDuplicateException;
import igrus.web.survey.response.repository.SurveyResponseRepository;
import igrus.web.survey.response.service.SurveyAnswerFactory;
import igrus.web.survey.response.service.SurveyAnswerValidator;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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
 *   <li>{@link #revertRegistration} - 승인/거절 되돌리기 - 선발제 (운영진 이상)</li>
 * </ul>
 *
 * @see EventService 행사 CRUD 관련 기능
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class EventRegistrationService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final SurveyAnswerValidator surveyAnswerValidator;
    private final SurveyAnswerFactory surveyAnswerFactory;

    /**
     * 행사에 신청합니다.
     *
     * <p>설문 연결 행사(event.surveyId != null)인 경우 {@link #registerEventWithSurvey}로 위임하여
     * 설문 응답 저장과 행사 신청을 단일 트랜잭션으로 원자적으로 처리합니다.
     * 설문 미연결 행사(event.surveyId == null)인 경우 기존 로직 그대로 실행합니다.</p>
     *
     * <p>검증 항목:</p>
     * <ul>
     *   <li>정회원 이상만 신청 가능</li>
     *   <li>중복 신청 불가</li>
     *   <li>등록 상태가 OPEN이어야 함</li>
     *   <li>신청 기간 내여야 함</li>
     *   <li>정원이 남아있어야 함 (선착순의 경우)</li>
     *   <li>설문 연결 행사: 설문 상태 검증 + 설문 응답 필수 (SEVT-INV-06, 10, 11)</li>
     * </ul>
     *
     * <p>동시성 제어: 원자적 UPDATE 방식 사용</p>
     *
     * @param eventId       행사 ID
     * @param userId        신청자 ID
     * @param surveyAnswers 설문 응답 데이터 (설문 미연결 행사에서는 무시됨, null 허용)
     * @return 신청 결과 응답 DTO
     * @throws EventNotFoundException               행사를 찾을 수 없는 경우
     * @throws UserNotFoundException                사용자를 찾을 수 없는 경우
     * @throws AssociateMemberNotAllowedException   준회원인 경우
     * @throws AlreadyRegisteredException           이미 신청한 경우
     * @throws EventTimeOverlapException            다른 행사와 시간이 겹치는 경우
     * @throws EventNotOpenException                등록 상태가 OPEN이 아닌 경우
     * @throws EventNotInRegistrationPeriodException 신청 기간이 아닌 경우
     * @throws EventCapacityFullException           정원이 초과된 경우 (선착순)
     * @throws SurveyResponseRequiredException      설문 응답이 필요한데 존재하지 않는 경우
     * @throws SurveyNotReadyException              설문이 NOT_STARTED 상태인 경우
     * @throws SurveyNotFoundException              설문이 삭제되었거나 휴지통에 있는 경우
     */
    public RegistrationResponse registerEvent(Long eventId, Long userId,
                                               List<SubmitAnswerRequest> surveyAnswers) {
        List<SubmitAnswerRequest> answers = surveyAnswers != null ? surveyAnswers : List.of();

        // 1. 행사 조회 (락 없이)
        Event event = eventRepository.findByIdAndNotDeleted(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 1-1. UNPUBLISHED 행사 신청 차단 (정보 은폐: 존재하지 않는 것처럼 처리)
        if (event.getVisibility() == EventVisibility.UNPUBLISHED) {
            throw new EventNotFoundException(eventId);
        }

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. 권한 확인 (정회원 이상)
        if (user.isAssociate()) {
            throw new AssociateMemberNotAllowedException();
        }

        // 4. Lazy Evaluation (registrationStatus 갱신 후 신청 가능 여부 판단)
        event.updateStatusIfNeeded(Instant.now());

        // 5. 기존 신청 기록 확인 (재신청 여부 판단)
        var existingRegistration = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId);
        if (existingRegistration.isPresent()) {
            return handleReRegistration(existingRegistration.get(), event, eventId, answers);
        }

        // 6. 등록 상태 확인 (OPEN 상태인지)
        validateEventIsOpen(event);

        // 7. 신청 기간 확인
        validateRegistrationPeriod(event);

        // SEVT-INV-07: 설문 연결 행사 분기
        if (event.hasSurvey()) {
            return registerEventWithSurvey(event, user, answers);
        }

        // === 설문 미연결 행사: 기존 로직 그대로 ===

        // 8. 다른 행사와 시간 겹침 확인
        validateNoTimeOverlap(userId, event);

        // 9. 선착순인 경우: 원자적 UPDATE로 신청자 수 증가
        if (event.isAutoApprove()) {
            int updated = eventRepository.incrementCurrentCountIfAvailable(eventId);
            if (updated == 0) {
                throw new EventCapacityFullException();
            }
            // 정원이 찼으면 상태 변경
            updateEventStatusAfterIncrement(eventId);
        }

        // 10. 신청 생성 및 저장
        EventRegistration registration = EventRegistration.create(event, user);
        EventRegistration savedRegistration = eventRegistrationRepository.save(registration);

        // 11. 응답 반환
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
        if (eventRepository.findByIdAndNotDeleted(eventId).isEmpty()) {
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
            int decremented = eventRepository.decrementCurrentCount(eventId);
            if (decremented == 0) {
                log.error("신청자 수 감소 실패 (이미 0): eventId={}", eventId);
            }
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
        Event event = eventRepository.findByIdAndNotDeleted(eventId)
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
     * REG-INV-14: COMPLETED 또는 CANCELED 상태에서는 승인/거절 불가.
     *
     * <p>동시성 제어: 원자적 UPDATE 방식 사용</p>
     *
     * @param registrationId 신청 ID
     * @param userId         요청자 ID (작성자/관리자)
     * @return 승인된 신청 응답 DTO
     * @throws EventRegistrationNotFoundException   신청을 찾을 수 없는 경우
     * @throws UserNotFoundException               사용자를 찾을 수 없는 경우
     * @throws OperatorPermissionRequiredException 운영진 권한이 없는 경우
     * @throws EventNotEditableException           COMPLETED/CANCELED 상태인 경우
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
        Event event = eventRepository.findByIdAndNotDeleted(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 3. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 4. 권한 확인 (운영진 이상)
        validateOperatorPermission(user);

        // 5. Lazy Evaluation (REG-INV-14: eventStatus 갱신 후 승인 가능 여부 판단)
        event.updateStatusIfNeeded(Instant.now());

        // 6. REG-INV-14: COMPLETED 또는 CANCELED 상태에서는 승인 불가
        if (event.getEventStatus() == EventStatus.COMPLETED || event.getEventStatus() == EventStatus.CANCELED) {
            throw new EventNotEditableException(event.getEventStatus());
        }

        // 7. 선발제 행사인지 확인
        if (!event.isManualApprove()) {
            throw new NotManualApproveEventException();
        }

        // 8. WAITING 상태인지 확인
        if (registration.getStatus() != EventRegistrationStatus.WAITING) {
            throw new InvalidRegistrationStatusException();
        }

        // 9. 시간 겹침 검증 (승인 대상 사용자의 기존 확정 신청과 겹치는지 확인)
        validateNoTimeOverlap(registration.getUser().getId(), event);

        // 10. 원자적 UPDATE로 신청자 수 증가 (정원 체크 포함, 상태 체크 없음)
        // 선발제 승인은 신청 기간 종료 후에도 가능해야 하므로 상태와 관계없이 정원만 체크
        int updated = eventRepository.incrementCurrentCountForApproval(eventId);
        if (updated == 0) {
            throw new EventCapacityFullException();
        }

        // 11. 승인 처리 (clearAutomatically로 인한 엔티티 분리 대비 — 명시적 save 필요)
        registration.approve();
        eventRegistrationRepository.save(registration);

        // 12. 정원이 찼으면 상태 변경
        updateEventStatusAfterIncrement(eventId);

        // 13. 응답 반환 (영속성 컨텍스트 초기화 후이므로 다시 조회)
        EventRegistration updatedRegistration = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(EventRegistrationNotFoundException::new);
        return RegistrationResponse.from(updatedRegistration);
    }

    /**
     * 신청을 거절합니다. (선발제 전용)
     * REG-INV-14: COMPLETED 또는 CANCELED 상태에서는 승인/거절 불가.
     *
     * @param registrationId 신청 ID
     * @param userId         요청자 ID (작성자/관리자)
     * @return 거절된 신청 응답 DTO
     * @throws EventRegistrationNotFoundException   신청을 찾을 수 없는 경우
     * @throws UserNotFoundException               사용자를 찾을 수 없는 경우
     * @throws OperatorPermissionRequiredException 운영진 권한이 없는 경우
     * @throws EventNotEditableException           COMPLETED/CANCELED 상태인 경우
     * @throws NotManualApproveEventException      수동 승인 행사가 아닌 경우
     * @throws InvalidRegistrationStatusException  대기 상태가 아닌 경우
     */
    public RegistrationResponse rejectRegistration(Long registrationId, Long userId) {
        // 1. 신청 조회
        EventRegistration registration = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(EventRegistrationNotFoundException::new);

        // 2. 행사 조회
        Event event = eventRepository.findByIdAndNotDeleted(registration.getEvent().getId())
                .orElseThrow(() -> new EventNotFoundException(registration.getEvent().getId()));

        // 3. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 4. 권한 확인 (운영진 이상)
        validateOperatorPermission(user);

        // 5. Lazy Evaluation (REG-INV-14: eventStatus 갱신 후 거절 가능 여부 판단)
        event.updateStatusIfNeeded(Instant.now());

        // 6. REG-INV-14: COMPLETED 또는 CANCELED 상태에서는 거절 불가
        if (event.getEventStatus() == EventStatus.COMPLETED || event.getEventStatus() == EventStatus.CANCELED) {
            throw new EventNotEditableException(event.getEventStatus());
        }

        // 7. 선발제 행사인지 확인
        if (!event.isManualApprove()) {
            throw new NotManualApproveEventException();
        }

        // 8. WAITING 상태인지 확인
        if (registration.getStatus() != EventRegistrationStatus.WAITING) {
            throw new InvalidRegistrationStatusException();
        }

        // 9. 거절 처리
        registration.reject();

        // 10. 응답 반환
        return RegistrationResponse.from(registration);
    }

    /**
     * 승인 또는 거절을 되돌려 대기 상태로 복원합니다. (선발제 전용)
     * REG-INV-10: 행사가 UPCOMING 상태일 때만 되돌리기 가능.
     *
     * <p>승인 상태에서 되돌릴 경우 신청자 수를 감소시킵니다.</p>
     *
     * @param registrationId 신청 ID
     * @param userId         요청자 ID (운영진)
     * @return 되돌린 신청 응답 DTO
     * @throws EventRegistrationNotFoundException   신청을 찾을 수 없는 경우
     * @throws UserNotFoundException               사용자를 찾을 수 없는 경우
     * @throws OperatorPermissionRequiredException 운영진 권한이 없는 경우
     * @throws EventNotEditableException           UPCOMING 상태가 아닌 경우
     * @throws NotManualApproveEventException      수동 승인 행사가 아닌 경우
     * @throws InvalidRegistrationStatusException  승인/거절 상태가 아닌 경우
     */
    public RegistrationResponse revertRegistration(Long registrationId, Long userId) {
        // 1. 신청 조회
        EventRegistration registration = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(EventRegistrationNotFoundException::new);

        Long eventId = registration.getEvent().getId();

        // 2. 행사 조회
        Event event = eventRepository.findByIdAndNotDeleted(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 3. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 4. 권한 확인 (운영진 이상)
        validateOperatorPermission(user);

        // 5. Lazy Evaluation (REG-INV-10: eventStatus 갱신 후 되돌리기 가능 여부 판단)
        event.updateStatusIfNeeded(Instant.now());

        // 6. REG-INV-10: 행사가 UPCOMING 상태일 때만 되돌리기 가능
        if (event.getEventStatus() != EventStatus.UPCOMING) {
            throw new EventNotEditableException(event.getEventStatus());
        }

        // 7. 선발제 행사인지 확인
        if (!event.isManualApprove()) {
            throw new NotManualApproveEventException();
        }

        // 8. APPROVED 또는 REJECTED 상태인지 확인
        if (!registration.isApproved() && !registration.isRejected()) {
            throw new InvalidRegistrationStatusException();
        }

        // 9. 승인 상태였는지 미리 기록 (clearAutomatically로 인한 엔티티 분리 대비)
        boolean wasApproved = registration.isApproved();

        // 10. WAITING 상태로 되돌리기 (카운트 감소 전에 상태 변경)
        registration.revertToWaiting();
        eventRegistrationRepository.saveAndFlush(registration);

        // 11. 승인 상태였으면 카운트 감소 (clearAutomatically=true로 영속성 컨텍스트 초기화됨)
        if (wasApproved) {
            int decremented = eventRepository.decrementCurrentCount(eventId);
            if (decremented == 0) {
                log.error("신청자 수 감소 실패 (이미 0): eventId={}, registrationId={}", eventId, registrationId);
            }
            updateEventStatusAfterDecrement(eventId);
        }

        // 12. 응답 반환 (영속성 컨텍스트 초기화 후이므로 다시 조회)
        EventRegistration updatedRegistration = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(EventRegistrationNotFoundException::new);
        return RegistrationResponse.from(updatedRegistration);
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
     * 설문 연결 행사에 대한 신청을 처리합니다.
     * 설문 응답 저장과 행사 신청을 단일 트랜잭션으로 원자적으로 처리합니다.
     *
     * <p>설문 상태별 surveyAnswers 처리 분기 매트릭스:</p>
     * <ul>
     *   <li>#1: OPEN + surveyAnswers 포함 -> 새 응답 저장 + 신청 진행</li>
     *   <li>#2: OPEN + surveyAnswers 미포함 + 기존 응답 있음 -> 기존 응답으로 신청 진행</li>
     *   <li>#3: OPEN + surveyAnswers 미포함 + 기존 응답 없음 -> 실패</li>
     *   <li>#4: CLOSED + surveyAnswers 포함 + 기존 응답 없음 -> 실패</li>
     *   <li>#5: CLOSED + surveyAnswers 포함 + 기존 응답 있음 -> surveyAnswers 무시, 기존 응답으로 진행</li>
     *   <li>#6: CLOSED + surveyAnswers 미포함 + 기존 응답 있음 -> 기존 응답으로 신청 진행</li>
     *   <li>#7: CLOSED + surveyAnswers 미포함 + 기존 응답 없음 -> 실패</li>
     *   <li>#8: NOT_STARTED -> validateSurveyState()에서 이미 차단</li>
     * </ul>
     *
     * @param event         행사 (surveyId != null)
     * @param user          신청자
     * @param surveyAnswers 설문 응답 데이터 (null 허용)
     * @return 신청 결과 응답 DTO
     * @throws SurveyNotFoundException         설문이 삭제되었거나 휴지통에 있는 경우
     * @throws SurveyNotReadyException         설문이 NOT_STARTED 상태인 경우
     * @throws SurveyResponseRequiredException 설문 응답이 필요한데 존재하지 않는 경우
     * @throws EventCapacityFullException      정원이 초과된 경우 (선착순)
     */
    private RegistrationResponse registerEventWithSurvey(Event event, User user,
                                                          List<SubmitAnswerRequest> surveyAnswers) {
        Long eventId = event.getId();

        // 8. 설문 상태 검증 (SEVT-INV-10, 11)
        Survey survey = surveyRepository.findById(event.getSurveyId())
                .orElseThrow(SurveyNotFoundException::new);
        validateSurveyState(survey, eventId, user.getId());

        // 9. 설문 응답 처리 (SEVT-INV-06) -- 분기 매트릭스 적용
        boolean hasExistingResponse = surveyResponseRepository
                .existsBySurveyIdAndUserId(event.getSurveyId(), user.getId());

        if (!surveyAnswers.isEmpty()) {
            // surveyAnswers 포함된 요청
            if (survey.getResponseStatus() == SurveyResponseStatus.OPEN) {
                // #1: OPEN + surveyAnswers 있음 -> 새 응답 저장
                surveyAnswerValidator.validate(survey, surveyAnswers);
                SurveyResponse response = SurveyResponse.create(survey, user);
                surveyAnswerFactory.createAnswers(response, survey, surveyAnswers);
                try {
                    surveyResponseRepository.save(response);
                } catch (DataIntegrityViolationException e) {
                    if (isDuplicateSurveyResponse(e)) {
                        throw new SurveyResponseDuplicateException();
                    }
                    throw e;
                }
            } else if (survey.getResponseStatus() == SurveyResponseStatus.CLOSED) {
                if (hasExistingResponse) {
                    // #5: CLOSED + surveyAnswers 있음 + 기존 응답 있음 -> surveyAnswers 무시, 기존 응답으로 진행
                    log.debug("설문 CLOSED 상태 - 제공된 surveyAnswers 무시, 기존 응답으로 진행 - surveyId: {}, userId: {}",
                            survey.getId(), user.getId());
                } else {
                    // #4: CLOSED + surveyAnswers 있음 + 기존 응답 없음 -> 실패
                    log.info("행사 신청 거부 - eventId: {}, userId: {}, 사유: 설문 응답 미존재 (surveyId: {})",
                            eventId, user.getId(), event.getSurveyId());
                    throw new SurveyResponseRequiredException();
                }
            }
        } else {
            // surveyAnswers 미포함된 요청
            if (!hasExistingResponse) {
                // #3, #7: 기존 응답 없음 -> 실패
                log.info("행사 신청 거부 - eventId: {}, userId: {}, 사유: 설문 응답 미존재 (surveyId: {})",
                        eventId, user.getId(), event.getSurveyId());
                throw new SurveyResponseRequiredException();
            }
            // #2, #6: 기존 응답 있음 -> 진행
        }

        // 설문 응답 확인 완료 로그 (TASK-015)
        log.debug("설문 응답 확인 완료 - eventId: {}, userId: {}, surveyId: {}",
                eventId, user.getId(), event.getSurveyId());

        // 10. 다른 행사와 시간 겹침 확인
        validateNoTimeOverlap(user.getId(), event);

        // 11. 선착순인 경우: 원자적 UPDATE로 신청자 수 증가
        if (event.isAutoApprove()) {
            int updated = eventRepository.incrementCurrentCountIfAvailable(eventId);
            if (updated == 0) {
                throw new EventCapacityFullException();
            }
            updateEventStatusAfterIncrement(eventId);
        }

        // 12. 신청 생성 및 저장
        EventRegistration registration = EventRegistration.create(event, user);
        EventRegistration savedRegistration = eventRegistrationRepository.save(registration);

        return RegistrationResponse.from(savedRegistration);
    }

    /**
     * 재신청을 처리합니다.
     * 취소된 신청만 재신청 가능합니다.
     * 설문 연결 행사인 경우 현재 event.surveyId 기준으로 설문 응답 존재 여부를 검증합니다 (SEVT-INV-06).
     *
     * @param registration 기존 신청 기록
     * @param event        행사
     * @param eventId      행사 ID (원자적 UPDATE용)
     * @return 재신청 결과 응답 DTO
     * @throws AlreadyRegisteredException       취소 상태가 아닌 경우
     * @throws EventNotOpenException            등록 상태가 OPEN이 아닌 경우
     * @throws EventCapacityFullException       정원 초과인 경우
     * @throws SurveyResponseRequiredException  설문 응답이 필요한데 존재하지 않는 경우
     * @throws SurveyNotFoundException          설문이 삭제되었거나 휴지통에 있는 경우
     * @throws SurveyNotReadyException          설문이 NOT_STARTED 상태인 경우
     */
    private RegistrationResponse handleReRegistration(EventRegistration registration, Event event, Long eventId,
                                                       List<SubmitAnswerRequest> surveyAnswers) {
        // 취소 상태가 아니면 이미 신청 중
        if (!registration.isCanceled()) {
            throw new AlreadyRegisteredException();
        }

        // 등록 상태 확인
        validateEventIsOpen(event);

        // 신청 기간 확인
        validateRegistrationPeriod(event);

        // 설문 검증 (SEVT-INV-06: 재신청 시에도 현재 event.surveyId 기준으로 검증)
        if (event.hasSurvey()) {
            Long userId = registration.getUser().getId();

            // 설문 상태 검증
            Survey survey = surveyRepository.findById(event.getSurveyId())
                    .orElseThrow(SurveyNotFoundException::new);
            validateSurveyState(survey, eventId, userId);

            // 설문 응답 존재 확인
            boolean hasExistingResponse = surveyResponseRepository
                    .existsBySurveyIdAndUserId(event.getSurveyId(), userId);

            if (!hasExistingResponse) {
                // 기존 응답 없음: surveyAnswers로 새 응답 저장 시도
                if (!surveyAnswers.isEmpty()
                        && survey.getResponseStatus() == SurveyResponseStatus.OPEN) {
                    surveyAnswerValidator.validate(survey, surveyAnswers);
                    User user = registration.getUser();
                    SurveyResponse response = SurveyResponse.create(survey, user);
                    surveyAnswerFactory.createAnswers(response, survey, surveyAnswers);
                    try {
                        surveyResponseRepository.save(response);
                    } catch (DataIntegrityViolationException e) {
                        if (isDuplicateSurveyResponse(e)) {
                            throw new SurveyResponseDuplicateException();
                        }
                        throw e;
                    }
                } else {
                    log.info("행사 재신청 거부 - eventId: {}, userId: {}, 사유: 설문 응답 미존재 (surveyId: {})",
                            eventId, userId, event.getSurveyId());
                    throw new SurveyResponseRequiredException();
                }
            }
            // 기존 응답 있음: 그대로 진행

            log.debug("설문 응답 확인 완료 - eventId: {}, userId: {}, surveyId: {}",
                    eventId, userId, event.getSurveyId());
        }

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

        // 재신청 처리 (clear 이후 detached 상태이므로 명시적 save 필요)
        registration.reRegister();
        eventRegistrationRepository.save(registration);

        return RegistrationResponse.from(registration);
    }

    /**
     * 등록 상태가 OPEN인지 검증합니다.
     *
     * @param event 행사
     * @throws EventNotOpenException OPEN 상태가 아닌 경우
     */
    private void validateEventIsOpen(Event event) {
        if (event.getRegistrationStatus() != RegistrationStatus.OPEN) {
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
     * 신청자 수 증가 후 등록 상태를 업데이트합니다.
     * 정원이 찼으면 CLOSED 상태로 변경합니다.
     *
     * @param eventId 행사 ID
     */
    private void updateEventStatusAfterIncrement(Long eventId) {
        Event event = eventRepository.findByIdAndNotDeleted(eventId).orElse(null);
        if (event == null) {
            log.warn("행사 상태 갱신 실패: 원자적 UPDATE 이후 행사를 찾을 수 없음. eventId={}", eventId);
            return;
        }
        if (event.isFull()) {
            event.closeRegistrationByCapacity();
        }
    }

    /**
     * 신청자 수 감소 후 등록 상태를 업데이트합니다.
     * 정원 마감 상태에서 자리가 생기면 OPEN 상태로 변경합니다.
     *
     * @param eventId 행사 ID
     */
    private void updateEventStatusAfterDecrement(Long eventId) {
        Event event = eventRepository.findByIdAndNotDeleted(eventId).orElse(null);
        if (event == null) {
            log.warn("행사 상태 갱신 실패: 원자적 UPDATE 이후 행사를 찾을 수 없음. eventId={}", eventId);
            return;
        }
        event.reopenIfNeeded(Instant.now());
    }

    /**
     * 설문 상태를 검증합니다.
     * 설문 연동 행사 신청 시, 설문이 유효하고 응답 수집이 가능한 상태인지 확인합니다.
     *
     * <p>검증 항목:</p>
     * <ul>
     *   <li>설문이 존재하고 삭제되지 않았는지 (deleted == false)</li>
     *   <li>설문이 휴지통에 있지 않은지 (trashedAt == null)</li>
     *   <li>설문 응답 상태가 NOT_STARTED가 아닌지 (OPEN 또는 CLOSED)</li>
     * </ul>
     *
     * <p>DECISION-03(A): responseStatus != NOT_STARTED만 검증. visibility는 검증 대상 아님</p>
     *
     * @param survey  검증할 설문 (null 가능)
     * @param eventId 행사 ID (로깅용)
     * @param userId  사용자 ID (로깅용)
     * @throws SurveyNotFoundException  설문이 null이거나, 삭제되었거나, 휴지통에 있는 경우
     * @throws SurveyNotReadyException  설문 응답 상태가 NOT_STARTED인 경우
     * @see igrus.web.survey.domain.SurveyResponseStatus
     */
    private void validateSurveyState(Survey survey, Long eventId, Long userId) {
        if (survey == null || survey.isDeleted()) {
            log.warn("행사 신청 거부 - eventId: {}, userId: {}, 사유: 연결된 설문이 삭제됨 (surveyId: {})",
                    eventId, userId, survey != null ? survey.getId() : "null");
            throw new SurveyNotFoundException();
        }
        if (survey.getTrashedAt() != null) {
            log.warn("행사 신청 거부 - eventId: {}, userId: {}, 사유: 연결된 설문이 삭제됨 (surveyId: {})",
                    eventId, userId, survey.getId());
            throw new SurveyNotFoundException();
        }
        if (survey.getResponseStatus() == SurveyResponseStatus.NOT_STARTED) {
            log.info("행사 신청 거부 - eventId: {}, userId: {}, 사유: 설문 미시작 (surveyId: {}, responseStatus: NOT_STARTED)",
                    eventId, userId, survey.getId());
            throw new SurveyNotReadyException();
        }
    }

    private static final String SURVEY_RESPONSE_UNIQUE_CONSTRAINT = "uk_survey_responses_survey_user";

    private boolean isDuplicateSurveyResponse(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException cve) {
            return SURVEY_RESPONSE_UNIQUE_CONSTRAINT.equals(cve.getConstraintName());
        }
        return false;
    }

}
