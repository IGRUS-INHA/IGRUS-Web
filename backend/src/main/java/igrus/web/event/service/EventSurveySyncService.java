package igrus.web.event.service;

import igrus.web.event.audit.EventStatusChanged;
import igrus.web.event.domain.Event;
import igrus.web.event.repository.EventRepository;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyResponseStatus;
import igrus.web.survey.repository.SurveyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

/**
 * 행사 상태 변경 시 연결된 설문의 상태를 동기화하는 서비스.
 * {@link RecordEventStatusChangeService}와 동일한 패턴으로 {@link EventStatusChanged} 이벤트를 수신합니다.
 *
 * <p>동기화 규칙:</p>
 * <ul>
 *   <li>EVENT_CANCELED → 설문 응답 마감 (OPEN → CLOSED)</li>
 *   <li>EVENT_UNPUBLISHED → 설문 비공개 (PUBLISHED → UNPUBLISHED, 자동으로 OPEN → CLOSED)</li>
 *   <li>EVENT_PUBLISHED → 설문 공개 (UNPUBLISHED → PUBLISHED)</li>
 *   <li>REGISTRATION_CLOSED_MANUAL → 설문 응답 마감 (OPEN → CLOSED)</li>
 *   <li>REGISTRATION_REOPENED → 설문 응답 재개 (CLOSED → OPEN)</li>
 *   <li>EVENT_REACTIVATED → 설문 공개 + 응답 재개</li>
 *   <li>REGISTRATION_CANCELED_BY_ADMIN → 변경 없음 (개별 신청 취소)</li>
 * </ul>
 *
 * <p>best-effort 방식: 동기화 실패 시 로그만 기록하고 행사 작업에는 영향 없음.</p>
 */
@Slf4j
@Service
public class EventSurveySyncService {

    private final EventRepository eventRepository;
    private final SurveyRepository surveyRepository;
    private final TransactionTemplate transactionTemplate;

    public EventSurveySyncService(
            EventRepository eventRepository,
            SurveyRepository surveyRepository,
            PlatformTransactionManager transactionManager) {
        this.eventRepository = eventRepository;
        this.surveyRepository = surveyRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @EventListener
    public void handleEventStatusChange(EventStatusChanged event) {
        try {
            transactionTemplate.executeWithoutResult(status -> syncSurveyStatus(event));
        } catch (Exception e) {
            log.error("설문 상태 동기화 실패: eventId={}, changeType={}",
                    event.eventId(), event.changeType(), e);
        }
    }

    private void syncSurveyStatus(EventStatusChanged event) {
        Event eventEntity = eventRepository.findById(event.eventId()).orElse(null);
        if (eventEntity == null || !eventEntity.hasSurvey()) {
            return;
        }

        Survey survey = surveyRepository.findByIdAndDeletedFalse(eventEntity.getSurveyId())
                .orElse(null);
        if (survey == null) {
            log.warn("동기화 대상 설문 없음 (삭제됨): surveyId={}", eventEntity.getSurveyId());
            return;
        }
        if (survey.isTrashed()) {
            log.warn("동기화 대상 설문이 휴지통 상태: surveyId={}", eventEntity.getSurveyId());
            return;
        }

        survey.updateStatusIfNeeded(Instant.now());

        switch (event.changeType()) {
            case EVENT_CANCELED -> closeSurveyResponseIfOpen(survey);
            case EVENT_UNPUBLISHED -> unpublishSurveyIfPublished(survey);
            case EVENT_PUBLISHED -> publishSurveyIfUnpublished(survey);
            case REGISTRATION_CLOSED_MANUAL -> closeSurveyResponseIfOpen(survey);
            case REGISTRATION_REOPENED -> openSurveyResponseIfClosed(survey);
            case EVENT_REACTIVATED -> reactivateSurvey(survey);
            case REGISTRATION_CANCELED_BY_ADMIN -> { /* 개별 신청 취소 - 설문 무관 */ }
        }

        log.info("설문 상태 동기화 완료: eventId={}, surveyId={}, changeType={}",
                event.eventId(), eventEntity.getSurveyId(), event.changeType());
    }

    private void closeSurveyResponseIfOpen(Survey survey) {
        if (!survey.isResponseOpen()) {
            log.debug("설문 응답이 이미 OPEN 상태가 아님: surveyId={}", survey.getId());
            return;
        }
        try {
            survey.closeResponse();
            log.info("설문 응답 마감: surveyId={}", survey.getId());
        } catch (Exception e) {
            log.warn("설문 응답 마감 실패: surveyId={}", survey.getId(), e);
        }
    }

    private void unpublishSurveyIfPublished(Survey survey) {
        if (!survey.isPublished()) {
            log.debug("설문이 이미 비공개 상태: surveyId={}", survey.getId());
            return;
        }
        try {
            survey.unpublish();
            log.info("설문 비공개 전환: surveyId={}", survey.getId());
        } catch (Exception e) {
            log.warn("설문 비공개 전환 실패: surveyId={}", survey.getId(), e);
        }
    }

    private void publishSurveyIfUnpublished(Survey survey) {
        if (survey.isPublished()) {
            log.debug("설문이 이미 공개 상태: surveyId={}", survey.getId());
            return;
        }
        try {
            survey.publish();
            log.info("설문 공개 전환: surveyId={}", survey.getId());
        } catch (Exception e) {
            log.warn("설문 공개 전환 실패 (질문 검증 등): surveyId={}", survey.getId(), e);
        }
    }

    private void openSurveyResponseIfClosed(Survey survey) {
        if (survey.isResponseOpen()) {
            log.debug("설문 응답이 이미 OPEN 상태: surveyId={}", survey.getId());
            return;
        }
        try {
            survey.openResponse();
            log.info("설문 응답 재개: surveyId={}", survey.getId());
        } catch (Exception e) {
            log.warn("설문 응답 재개 실패: surveyId={}", survey.getId(), e);
        }
    }

    private void reactivateSurvey(Survey survey) {
        publishSurveyIfUnpublished(survey);
        openSurveyResponseIfClosed(survey);
    }

    /**
     * 등록 시작(NOT_STARTED → OPEN) 시 연결된 설문을 공개하고 응답 수집을 시작합니다.
     * {@link EventStatusHelper}에서 Lazy Evaluation 전이 감지 시 직접 호출됩니다.
     *
     * <p>설문이 NOT_STARTED 상태일 때만 동작합니다.
     * 이미 OPEN이거나 CLOSED 상태이면 변경하지 않습니다.</p>
     *
     * <p>best-effort 방식: 실패 시 로그만 기록하고 행사 작업에는 영향 없음.</p>
     *
     * @param eventId 행사 ID
     */
    public void openSurveyForRegistration(Long eventId) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Event eventEntity = eventRepository.findById(eventId).orElse(null);
                if (eventEntity == null || !eventEntity.hasSurvey()) {
                    return;
                }

                Survey survey = surveyRepository.findByIdAndDeletedFalse(eventEntity.getSurveyId())
                        .orElse(null);
                if (survey == null || survey.isTrashed()) {
                    return;
                }

                if (survey.getResponseStatus() != SurveyResponseStatus.NOT_STARTED) {
                    log.debug("설문이 NOT_STARTED 상태가 아님, 동기화 건너뜀: surveyId={}, status={}",
                            survey.getId(), survey.getResponseStatus());
                    return;
                }

                publishSurveyIfUnpublished(survey);
                survey.openResponse();
                log.info("등록 시작에 따른 설문 응답 수집 시작: eventId={}, surveyId={}", eventId, survey.getId());
            });
        } catch (Exception e) {
            log.error("등록 시작 시 설문 동기화 실패: eventId={}", eventId, e);
        }
    }
}
