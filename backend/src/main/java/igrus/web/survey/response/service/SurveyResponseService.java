package igrus.web.survey.response.service;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationStatus;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.event.repository.EventRepository;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyAccessLevel;
import igrus.web.survey.domain.SurveyResponseStatus;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.survey.response.domain.SurveyResponse;
import igrus.web.survey.response.dto.request.SubmitSurveyResponseRequest;
import igrus.web.survey.response.dto.response.AdminSurveyResponseListItem;
import igrus.web.survey.response.dto.response.SurveyResponseDetailResponse;
import igrus.web.survey.response.exception.*;
import igrus.web.survey.response.repository.SurveyResponseRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 설문 응답 서비스.
 * 설문 응답 제출, 수정, 조회 비즈니스 로직을 처리합니다.
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class SurveyResponseService {

    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final UserRepository userRepository;
    private final SurveyAnswerValidator answerValidator;
    private final SurveyAnswerFactory surveyAnswerFactory;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;

    /**
     * 회원 응답을 제출합니다.
     *
     * @param surveyId 설문 ID
     * @param request  응답 제출 요청
     * @param auth     인증된 사용자 정보
     * @return 제출된 응답 상세
     */
    public SurveyResponseDetailResponse submitResponse(Long surveyId,
                                                        SubmitSurveyResponseRequest request,
                                                        AuthenticatedUser auth) {
        User user = userRepository.findById(auth.userId())
                .orElseThrow(() -> new UserNotFoundException(auth.userId()));

        Survey survey = surveyRepository.findByIdAndDeletedFalse(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        survey.updateStatusIfNeeded(Instant.now());

        if (!survey.isAcceptingResponses()) {
            throw new SurveyNotAcceptingResponsesException();
        }

        validateAccessLevel(survey.getAccessLevel(), user);

        if (surveyResponseRepository.existsBySurveyIdAndUserId(surveyId, user.getId())) {
            throw new SurveyResponseDuplicateException();
        }

        answerValidator.validate(survey, request.answers());

        SurveyResponse response = SurveyResponse.create(survey, user);
        surveyAnswerFactory.createAnswers(response, survey, request.answers());

        SurveyResponse savedResponse;
        try {
            savedResponse = surveyResponseRepository.save(response);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateSurveyResponse(e)) {
                throw new SurveyResponseDuplicateException();
            }
            throw e;
        }

        log.info("설문 응답 제출 - surveyId: {}, userId: {}", surveyId, user.getId());
        return SurveyResponseDetailResponse.from(savedResponse);
    }

    /**
     * 비회원(익명) 응답을 제출합니다. PUBLIC 설문에서만 가능합니다.
     *
     * @param surveyId 설문 ID
     * @param request  응답 제출 요청
     * @return 제출된 응답 상세
     */
    public SurveyResponseDetailResponse submitAnonymousResponse(Long surveyId,
                                                                 SubmitSurveyResponseRequest request) {
        Survey survey = surveyRepository.findByIdAndDeletedFalse(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        survey.updateStatusIfNeeded(Instant.now());

        if (survey.getAccessLevel() != SurveyAccessLevel.PUBLIC) {
            throw new SurveyAnonymousNotAllowedException();
        }

        if (!survey.isAcceptingResponses()) {
            throw new SurveyNotAcceptingResponsesException();
        }

        answerValidator.validate(survey, request.answers());

        SurveyResponse response = SurveyResponse.createAnonymous(survey);
        surveyAnswerFactory.createAnswers(response, survey, request.answers());
        SurveyResponse savedResponse = surveyResponseRepository.save(response);

        log.info("비회원 설문 응답 제출 - surveyId: {}", surveyId);
        return SurveyResponseDetailResponse.from(savedResponse);
    }

    /**
     * 본인 응답을 수정합니다. OPEN 상태 + 현재 accessLevel 충족 시에만 가능합니다.
     *
     * @param surveyId 설문 ID
     * @param request  응답 수정 요청
     * @param auth     인증된 사용자 정보
     * @return 수정된 응답 상세
     */
    public SurveyResponseDetailResponse updateMyResponse(Long surveyId,
                                                          SubmitSurveyResponseRequest request,
                                                          AuthenticatedUser auth) {
        User user = userRepository.findById(auth.userId())
                .orElseThrow(() -> new UserNotFoundException(auth.userId()));

        Survey survey = surveyRepository.findByIdAndDeletedFalse(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        survey.updateStatusIfNeeded(Instant.now());

        if (!survey.isAcceptingResponses()) {
            throw new SurveyNotAcceptingResponsesException();
        }

        validateAccessLevel(survey.getAccessLevel(), user);

        SurveyResponse response = surveyResponseRepository.findBySurveyIdAndUserIdWithAnswers(surveyId, user.getId())
                .orElseThrow(SurveyResponseNotFoundException::new);

        answerValidator.validate(survey, request.answers());

        response.getAnswers().clear();
        surveyAnswerFactory.createAnswers(response, survey, request.answers());
        SurveyResponse savedResponse = surveyResponseRepository.save(response);

        log.info("설문 응답 수정 - surveyId: {}, userId: {}", surveyId, user.getId());
        return SurveyResponseDetailResponse.from(savedResponse);
    }

    /**
     * 본인 응답을 조회합니다.
     *
     * @param surveyId 설문 ID
     * @param auth     인증된 사용자 정보
     * @return 본인 응답 상세
     */
    @Transactional(readOnly = true)
    public SurveyResponseDetailResponse getMyResponse(Long surveyId, AuthenticatedUser auth) {
        User user = userRepository.findById(auth.userId())
                .orElseThrow(() -> new UserNotFoundException(auth.userId()));

        SurveyResponse response = surveyResponseRepository.findBySurveyIdAndUserIdWithAnswers(surveyId, user.getId())
                .orElseThrow(SurveyResponseNotFoundException::new);

        log.info("본인 응답 조회 - surveyId: {}, userId: {}", surveyId, user.getId());
        return SurveyResponseDetailResponse.from(response);
    }

    /**
     * 관리자가 특정 설문의 응답 목록을 조회합니다.
     * 설문이 존재하지 않거나 삭제된 경우 404를 반환합니다.
     * 삭제된 응답은 결과에 포함되지 않습니다.
     *
     * @param surveyId 설문 ID
     * @return 응답 목록 (빈 목록 가능)
     */
    @Transactional(readOnly = true)
    public List<AdminSurveyResponseListItem> getResponsesBySurveyId(Long surveyId) {
        surveyRepository.findByIdAndDeletedFalse(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));

        List<SurveyResponse> responses = surveyResponseRepository
                .findValidResponsesWithUserAndAnswersBySurveyId(surveyId);

        log.info("관리자 응답 목록 조회 - surveyId: {}, count: {}", surveyId, responses.size());
        return responses.stream()
                .map(AdminSurveyResponseListItem::from)
                .toList();
    }

    /**
     * 본인 응답을 삭제합니다.
     * OPEN 상태의 설문에서만 삭제 가능합니다.
     * 행사 연결 설문의 경우 행사 신청도 함께 취소됩니다.
     *
     * @param surveyId 설문 ID
     * @param auth     인증된 사용자 정보
     */
    public void deleteMyResponse(Long surveyId, AuthenticatedUser auth) {
        User user = userRepository.findById(auth.userId())
                .orElseThrow(() -> new UserNotFoundException(auth.userId()));

        Survey survey = surveyRepository.findByIdAndDeletedFalse(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        survey.updateStatusIfNeeded(Instant.now());

        if (survey.getResponseStatus() != SurveyResponseStatus.OPEN) {
            throw new SurveyClosedException();
        }

        SurveyResponse response = surveyResponseRepository.findBySurveyIdAndUserIdAndDeletedFalse(surveyId, user.getId())
                .orElseThrow(SurveyResponseNotFoundException::new);

        response.delete(user.getId());

        cancelLinkedEventRegistration(surveyId, user.getId());

        log.info("설문 응답 삭제 - surveyId: {}, userId: {}", surveyId, user.getId());
    }

    /**
     * 설문과 연결된 행사의 신청을 취소합니다.
     * 연결된 행사가 없거나 신청이 없으면 아무 동작하지 않습니다.
     */
    private void cancelLinkedEventRegistration(Long surveyId, Long userId) {
        Optional<Event> eventOptional = eventRepository.findBySurveyId(surveyId);
        if (eventOptional.isEmpty()) {
            return;
        }

        Event event = eventOptional.get();
        Optional<EventRegistration> registrationOptional =
                eventRegistrationRepository.findByEventIdAndUserId(event.getId(), userId);
        if (registrationOptional.isEmpty()) {
            return;
        }

        EventRegistration registration = registrationOptional.get();
        if (registration.isCanceled()) {
            return;
        }

        // WAITING 상태는 정원에 포함되지 않으므로 카운트를 감소하지 않음.
        // APPROVED, REGISTERED 상태만 정원에 포함되므로 해당 상태일 때만 카운트 감소.
        boolean wasApproved = registration.getStatus() == EventRegistrationStatus.APPROVED
                || registration.getStatus() == EventRegistrationStatus.REGISTERED;

        // cancel()을 먼저 호출하여 상태를 변경한 후 카운트를 감소시킴.
        // cancel()은 엔티티 상태만 변경하고, decrementCurrentCount()는 별도 @Modifying 쿼리로 실행됨.
        registration.cancel();

        if (wasApproved) {
            eventRepository.decrementCurrentCount(event.getId());
        }

        log.info("설문 응답 삭제로 행사 신청 취소 - eventId: {}, userId: {}, wasApproved: {}",
                event.getId(), userId, wasApproved);
    }

    // ==================== Private helpers ====================

    /**
     * 설문의 accessLevel과 사용자 역할을 비교하여 응답 권한을 검증합니다.
     */
    private void validateAccessLevel(SurveyAccessLevel accessLevel, User user) {
        boolean allowed = switch (accessLevel) {
            case PUBLIC, ASSOCIATE -> true;
            case MEMBER -> !user.isAssociate();
            case OPERATOR -> user.isOperatorOrAbove();
        };
        if (!allowed) {
            throw new SurveyResponseAccessDeniedException();
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
