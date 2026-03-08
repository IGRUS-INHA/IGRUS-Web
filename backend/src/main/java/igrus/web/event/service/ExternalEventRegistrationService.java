package igrus.web.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationStatus;
import igrus.web.event.domain.EventVisibility;
import igrus.web.event.domain.ExternalSurveyResponse;
import igrus.web.event.domain.RegistrationStatus;
import igrus.web.event.dto.response.RegistrationResponse;
import igrus.web.event.exception.EventCapacityFullException;
import igrus.web.event.exception.EventNotFoundException;
import igrus.web.event.exception.EventNotInRegistrationPeriodException;
import igrus.web.event.exception.EventNotOpenException;
import igrus.web.event.exception.EventTimeOverlapException;
import igrus.web.event.exception.ExternalAlreadyRegisteredException;
import igrus.web.event.exception.ExternalRegistrationNotAllowedException;
import igrus.web.event.exception.RegisteredMemberExistsException;
import igrus.web.event.exception.SurveyResponseSerializationException;
import igrus.web.event.exception.SurveyResponseRequiredException;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.event.repository.EventRepository;
import igrus.web.event.repository.ExternalSurveyResponseRepository;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyResponseStatus;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.survey.response.dto.request.SubmitAnswerRequest;
import igrus.web.survey.response.service.SurveyAnswerValidator;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 외부인 행사 신청 서비스.
 * 외부인(비회원) 행사 신청의 전체 검증 및 처리 로직을 구현합니다.
 *
 * <p>검증 순서:</p>
 * <ol>
 *   <li>행사 조회 + PUBLISHED 확인 (EXT-INV-08)</li>
 *   <li>allowExternal 확인 (EXT-INV-01)</li>
 *   <li>동일 studentId로 가입된 회원 존재 확인 (EXT-INV-12)</li>
 *   <li>studentId 중복 검사 (EXT-INV-02)</li>
 *   <li>phone 중복 검사 (EXT-INV-03)</li>
 *   <li>OPEN 상태 + 기간 내 확인 (EXT-INV-07)</li>
 *   <li>시간 겹침 검증 -- studentId 기반 (DECISION-06)</li>
 *   <li>설문 연동 처리 (EXT-INV-11)</li>
 *   <li>정원 확인 + 원자적 UPDATE (EXT-INV-04)</li>
 *   <li>EventRegistration.createExternal() 호출 및 저장</li>
 * </ol>
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class ExternalEventRegistrationService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final UserRepository userRepository;
    private final ExternalSurveyResponseRepository externalSurveyResponseRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyAnswerValidator surveyAnswerValidator;
    private final ObjectMapper objectMapper;
    private final EventStatusHelper eventStatusHelper;

    /**
     * 외부인 행사 신청을 처리합니다.
     *
     * @param eventId       행사 ID
     * @param name          외부인 이름
     * @param studentId     외부인 학번
     * @param phone         외부인 전화번호
     * @param department    외부인 학과
     * @param surveyAnswers 설문 응답 (nullable)
     * @return 신청 결과 응답 DTO
     */
    public RegistrationResponse registerExternal(Long eventId, String name, String studentId,
                                                  String phone, String department,
                                                  List<SubmitAnswerRequest> surveyAnswers) {
        List<SubmitAnswerRequest> answers = surveyAnswers != null ? surveyAnswers : List.of();

        // 1. 행사 조회 (EXT-INV-08: UNPUBLISHED -> 404)
        Event event = eventRepository.findByIdAndNotDeleted(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (event.getVisibility() == EventVisibility.UNPUBLISHED) {
            throw new EventNotFoundException(eventId);
        }

        // 2. allowExternal 확인 (EXT-INV-01)
        if (!Boolean.TRUE.equals(event.getAllowExternal())) {
            log.info("외부인 행사 신청 거부 - eventId: {}, 사유: 외부인 신청 비허용 행사", eventId);
            throw new ExternalRegistrationNotAllowedException();
        }

        // 3. 동일 studentId로 가입된 회원 존재 확인 (EXT-INV-12)
        if (userRepository.findByStudentId(studentId).isPresent()) {
            throw new RegisteredMemberExistsException();
        }

        // 4. studentId 중복 검사 -- CANCELED 제외 (EXT-INV-02)
        if (eventRegistrationRepository.existsByEventAndExternalStudentIdAndStatusNot(
                event, studentId, EventRegistrationStatus.CANCELED)) {
            log.info("외부인 행사 신청 거부 - eventId: {}, studentId: {}, 사유: 학번 중복", eventId, studentId);
            throw new ExternalAlreadyRegisteredException();
        }

        // 5. phone 중복 검사 -- CANCELED 제외 (EXT-INV-03)
        if (eventRegistrationRepository.existsByEventAndExternalPhoneAndStatusNot(
                event, phone, EventRegistrationStatus.CANCELED)) {
            log.info("외부인 행사 신청 거부 - eventId: {}, phone: {}, 사유: 전화번호 중복", eventId, phone);
            throw new ExternalAlreadyRegisteredException();
        }

        // 6. Lazy Evaluation (registrationStatus 갱신)
        event.updateStatusIfNeeded(Instant.now());

        // 7. OPEN 상태 확인 (EXT-INV-07)
        if (event.getRegistrationStatus() != RegistrationStatus.OPEN) {
            throw new EventNotOpenException();
        }

        // 8. 신청 기간 확인 (EXT-INV-07)
        Instant now = Instant.now();
        if (now.isBefore(event.getRegistrationStartAt()) || now.isAfter(event.getRegistrationEndAt())) {
            throw new EventNotInRegistrationPeriodException();
        }

        // 9. 시간 겹침 검증 -- studentId 기반 (DECISION-06)
        boolean hasOverlap = eventRegistrationRepository.existsOverlappingExternalRegistration(
                studentId,
                event.getEventStartAt(),
                event.getEventEndAt(),
                EventRegistrationStatus.CANCELED
        );
        if (hasOverlap) {
            throw new EventTimeOverlapException();
        }

        // 10. 설문 연동 처리 (EXT-INV-11)
        if (event.hasSurvey()) {
            Survey survey = surveyRepository.findById(event.getSurveyId())
                    .orElseThrow(SurveyNotFoundException::new);
            validateExternalSurveyState(survey, eventId);

            if (answers.isEmpty()) {
                // 설문 연결 행사인데 응답이 없으면 에러
                throw new SurveyResponseRequiredException();
            }

            // 설문 응답 검증
            surveyAnswerValidator.validate(survey, answers);
        }
        // 설문 미연결 행사에서 surveyAnswers가 제공되면 무시 (TC-054)

        // 11. 선착순인 경우: 원자적 UPDATE로 신청자 수 증가 (EXT-INV-04)
        if (event.isAutoApprove()) {
            int updated = eventRepository.incrementCurrentCountIfAvailable(eventId);
            if (updated == 0) {
                throw new EventCapacityFullException();
            }
            eventStatusHelper.updateEventStatusAfterIncrement(eventId);
            // clearAutomatically=true로 인해 기존 event가 detached 상태이므로 재조회
            event = eventRepository.findByIdAndNotDeleted(eventId)
                    .orElseThrow(() -> new EventNotFoundException(eventId));
        }

        // 12. 신청 생성 및 저장
        EventRegistration registration = EventRegistration.createExternal(
                event, name, studentId, phone, department);
        EventRegistration savedRegistration = eventRegistrationRepository.save(registration);

        // 13. 설문 응답 저장 (EXT-INV-11: DECISION-04 옵션 B - ExternalSurveyResponse 테이블)
        if (event.hasSurvey() && !answers.isEmpty()) {
            String answersJson = serializeSurveyAnswers(answers);
            ExternalSurveyResponse surveyResponse = ExternalSurveyResponse.create(
                    event.getSurveyId(),
                    savedRegistration.getId(),
                    studentId,
                    answersJson
            );
            externalSurveyResponseRepository.save(surveyResponse);
        }

        log.info("외부인 행사 신청 완료 - eventId: {}, studentId: {}, registrationId: {}",
                eventId, studentId, savedRegistration.getId());

        // 14. 응답 반환
        return RegistrationResponse.from(savedRegistration);
    }

    /**
     * 외부인 설문 상태를 검증합니다.
     */
    private void validateExternalSurveyState(Survey survey, Long eventId) {
        if (survey == null || survey.isDeleted()) {
            log.warn("외부인 행사 신청 거부 - eventId: {}, 사유: 연결된 설문이 삭제됨 (surveyId: {})",
                    eventId, survey != null ? survey.getId() : "null");
            throw new SurveyNotFoundException();
        }
        if (survey.getTrashedAt() != null) {
            log.warn("외부인 행사 신청 거부 - eventId: {}, 사유: 연결된 설문이 휴지통에 있음 (surveyId: {})",
                    eventId, survey.getId());
            throw new SurveyNotFoundException();
        }
        if (survey.getResponseStatus() == SurveyResponseStatus.NOT_STARTED) {
            throw new igrus.web.event.exception.SurveyNotReadyException();
        }
    }

    /**
     * 설문 응답을 JSON 문자열로 직렬화합니다.
     */
    private String serializeSurveyAnswers(List<SubmitAnswerRequest> answers) {
        try {
            return objectMapper.writeValueAsString(answers);
        } catch (JsonProcessingException e) {
            throw new SurveyResponseSerializationException("설문 응답 직렬화 실패", e);
        }
    }

}
