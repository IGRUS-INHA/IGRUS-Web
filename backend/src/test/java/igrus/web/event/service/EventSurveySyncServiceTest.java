package igrus.web.event.service;

import igrus.web.event.audit.EventStatusChanged;
import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventChangeType;
import igrus.web.event.repository.EventRepository;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyAccessLevel;
import igrus.web.survey.domain.SurveyResponseStatus;
import igrus.web.survey.domain.SurveyVisibility;
import igrus.web.survey.repository.SurveyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * EventSurveySyncService 단위 테스트.
 * 행사 상태 변경 시 연결된 설문의 상태 동기화를 검증합니다.
 */
@DisplayName("EventSurveySyncService")
@ExtendWith(MockitoExtension.class)
class EventSurveySyncServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private EventSurveySyncService surveySyncService;

    private static final Long EVENT_ID = 1L;
    private static final Long SURVEY_ID = 10L;
    private static final Long USER_ID = 100L;

    @BeforeEach
    void setUp() {
        // TransactionTemplate이 콜백을 즉시 실행하도록 설정
        surveySyncService = new EventSurveySyncService(eventRepository, surveyRepository, transactionManager) {
            @Override
            public void handleEventStatusChange(EventStatusChanged event) {
                // TransactionTemplate을 우회하고 직접 실행
                try {
                    java.lang.reflect.Method method = EventSurveySyncService.class.getDeclaredMethod(
                            "syncSurveyStatus", EventStatusChanged.class);
                    method.setAccessible(true);
                    method.invoke(this, event);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private Event createEventWithSurvey(Long surveyId) {
        Event event = mock(Event.class);
        when(event.hasSurvey()).thenReturn(surveyId != null);
        if (surveyId != null) {
            when(event.getSurveyId()).thenReturn(surveyId);
        }
        return event;
    }

    private Survey createSurvey(SurveyVisibility visibility, SurveyResponseStatus responseStatus) {
        Survey survey = Survey.create("테스트 설문", "설명", SurveyAccessLevel.PUBLIC, null);
        if (visibility == SurveyVisibility.PUBLISHED) {
            survey.publish();
        }
        if (responseStatus == SurveyResponseStatus.OPEN) {
            if (!survey.isPublished()) {
                survey.publish();
            }
            survey.openResponse();
        } else if (responseStatus == SurveyResponseStatus.CLOSED) {
            if (!survey.isPublished()) {
                survey.publish();
            }
            survey.openResponse();
            survey.closeResponse();
        }
        return survey;
    }

    private EventStatusChanged createEvent(EventChangeType changeType) {
        return new EventStatusChanged(EVENT_ID, USER_ID, changeType, "PREV", "NEW", null);
    }

    @Nested
    @DisplayName("설문 없는 경우")
    class NoSurveyLinked {

        @Test
        @DisplayName("surveyId가 null이면 아무 동작 안 함")
        void noSurveyId_doesNothing() {
            Event event = createEventWithSurvey(null);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            surveySyncService.handleEventStatusChange(createEvent(EventChangeType.EVENT_CANCELED));

            verifyNoInteractions(surveyRepository);
        }

        @Test
        @DisplayName("행사가 존재하지 않으면 아무 동작 안 함")
        void eventNotFound_doesNothing() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            surveySyncService.handleEventStatusChange(createEvent(EventChangeType.EVENT_CANCELED));

            verifyNoInteractions(surveyRepository);
        }

        @Test
        @DisplayName("설문이 삭제되었으면 아무 동작 안 함")
        void surveyDeleted_doesNothing() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.empty());

            surveySyncService.handleEventStatusChange(createEvent(EventChangeType.EVENT_CANCELED));
        }

        @Test
        @DisplayName("설문이 휴지통 상태이면 아무 동작 안 함")
        void surveyTrashed_doesNothing() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Survey survey = createSurvey(SurveyVisibility.PUBLISHED, SurveyResponseStatus.OPEN);
            survey.trash();
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.of(survey));

            surveySyncService.handleEventStatusChange(createEvent(EventChangeType.EVENT_CANCELED));

            // 설문이 여전히 OPEN (변경 안 됨)
            assertThat(survey.isResponseOpen()).isTrue();
        }
    }

    @Nested
    @DisplayName("EVENT_CANCELED - 행사 취소 시 설문 응답 마감")
    class EventCanceled {

        @Test
        @DisplayName("설문이 OPEN이면 CLOSED로 전환")
        void openSurvey_closeResponse() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Survey survey = createSurvey(SurveyVisibility.PUBLISHED, SurveyResponseStatus.OPEN);
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.of(survey));

            surveySyncService.handleEventStatusChange(createEvent(EventChangeType.EVENT_CANCELED));

            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.CLOSED);
        }

        @Test
        @DisplayName("설문이 이미 CLOSED이면 변경 없음")
        void alreadyClosed_noChange() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Survey survey = createSurvey(SurveyVisibility.PUBLISHED, SurveyResponseStatus.CLOSED);
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.of(survey));

            surveySyncService.handleEventStatusChange(createEvent(EventChangeType.EVENT_CANCELED));

            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.CLOSED);
        }

        @Test
        @DisplayName("설문이 NOT_STARTED이면 변경 없음")
        void notStarted_noChange() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Survey survey = createSurvey(SurveyVisibility.UNPUBLISHED, SurveyResponseStatus.NOT_STARTED);
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.of(survey));

            surveySyncService.handleEventStatusChange(createEvent(EventChangeType.EVENT_CANCELED));

            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.NOT_STARTED);
        }
    }

    @Nested
    @DisplayName("EVENT_UNPUBLISHED - 행사 비공개 시 설문 비공개")
    class EventUnpublished {

        @Test
        @DisplayName("설문이 PUBLISHED + OPEN이면 UNPUBLISHED + CLOSED로 전환")
        void publishedOpen_unpublishAndClose() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Survey survey = createSurvey(SurveyVisibility.PUBLISHED, SurveyResponseStatus.OPEN);
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.of(survey));

            surveySyncService.handleEventStatusChange(createEvent(EventChangeType.EVENT_UNPUBLISHED));

            assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.UNPUBLISHED);
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.CLOSED);
        }

        @Test
        @DisplayName("설문이 이미 UNPUBLISHED이면 변경 없음")
        void alreadyUnpublished_noChange() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Survey survey = createSurvey(SurveyVisibility.UNPUBLISHED, SurveyResponseStatus.NOT_STARTED);
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.of(survey));

            surveySyncService.handleEventStatusChange(createEvent(EventChangeType.EVENT_UNPUBLISHED));

            assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.UNPUBLISHED);
        }
    }

    @Nested
    @DisplayName("EVENT_PUBLISHED - 행사 공개 시 설문 공개")
    class EventPublished {

        @Test
        @DisplayName("설문이 UNPUBLISHED이면 PUBLISHED로 전환")
        void unpublished_publish() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Survey survey = createSurvey(SurveyVisibility.UNPUBLISHED, SurveyResponseStatus.NOT_STARTED);
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.of(survey));

            surveySyncService.handleEventStatusChange(createEvent(EventChangeType.EVENT_PUBLISHED));

            assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.PUBLISHED);
        }

        @Test
        @DisplayName("설문이 이미 PUBLISHED이면 변경 없음")
        void alreadyPublished_noChange() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Survey survey = createSurvey(SurveyVisibility.PUBLISHED, SurveyResponseStatus.CLOSED);
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.of(survey));

            surveySyncService.handleEventStatusChange(createEvent(EventChangeType.EVENT_PUBLISHED));

            assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.PUBLISHED);
        }
    }

    @Nested
    @DisplayName("REGISTRATION_CLOSED_MANUAL - 모집 마감 시 설문 마감")
    class RegistrationClosedManual {

        @Test
        @DisplayName("설문이 OPEN이면 CLOSED로 전환")
        void openSurvey_closeResponse() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Survey survey = createSurvey(SurveyVisibility.PUBLISHED, SurveyResponseStatus.OPEN);
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.of(survey));

            surveySyncService.handleEventStatusChange(createEvent(EventChangeType.REGISTRATION_CLOSED_MANUAL));

            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.CLOSED);
        }
    }

    @Nested
    @DisplayName("REGISTRATION_REOPENED - 모집 재개 시 설문 재개")
    class RegistrationReopened {

        @Test
        @DisplayName("설문이 CLOSED이면 OPEN으로 전환")
        void closedSurvey_openResponse() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Survey survey = createSurvey(SurveyVisibility.PUBLISHED, SurveyResponseStatus.CLOSED);
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.of(survey));

            surveySyncService.handleEventStatusChange(createEvent(EventChangeType.REGISTRATION_REOPENED));

            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.OPEN);
        }
    }

    @Nested
    @DisplayName("EVENT_REACTIVATED - 행사 재활성화 시 설문 공개 + 응답 재개")
    class EventReactivated {

        @Test
        @DisplayName("설문이 UNPUBLISHED + CLOSED이면 PUBLISHED + OPEN으로 전환")
        void unpublishedClosed_publishAndOpen() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Survey survey = createSurvey(SurveyVisibility.UNPUBLISHED, SurveyResponseStatus.CLOSED);
            // CLOSED 상태이지만 UNPUBLISHED이므로 publish 후 open 해야 함
            // createSurvey에서 CLOSED를 만들 때 publish->open->close 순으로 진행했으므로
            // unpublish해야 UNPUBLISHED + CLOSED 상태가 됨
            survey.unpublish();
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.of(survey));

            assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.UNPUBLISHED);
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.CLOSED);

            surveySyncService.handleEventStatusChange(createEvent(EventChangeType.EVENT_REACTIVATED));

            assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.PUBLISHED);
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.OPEN);
        }

        @Test
        @DisplayName("설문이 이미 PUBLISHED + OPEN이면 변경 없음")
        void alreadyPublishedOpen_noChange() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Survey survey = createSurvey(SurveyVisibility.PUBLISHED, SurveyResponseStatus.OPEN);
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.of(survey));

            surveySyncService.handleEventStatusChange(createEvent(EventChangeType.EVENT_REACTIVATED));

            assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.PUBLISHED);
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.OPEN);
        }
    }

    @Nested
    @DisplayName("openSurveyForRegistration - 등록 시작 시 설문 공개 + 응답 수집 시작")
    class OpenSurveyForRegistration {

        @Test
        @DisplayName("설문이 UNPUBLISHED + NOT_STARTED이면 PUBLISHED + OPEN으로 전환")
        void unpublishedNotStarted_publishAndOpen() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Survey survey = createSurvey(SurveyVisibility.UNPUBLISHED, SurveyResponseStatus.NOT_STARTED);
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.of(survey));

            surveySyncService.openSurveyForRegistration(EVENT_ID);

            assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.PUBLISHED);
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.OPEN);
        }

        @Test
        @DisplayName("설문이 PUBLISHED + NOT_STARTED이면 OPEN으로 전환")
        void publishedNotStarted_open() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Survey survey = createSurvey(SurveyVisibility.PUBLISHED, SurveyResponseStatus.NOT_STARTED);
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.of(survey));

            surveySyncService.openSurveyForRegistration(EVENT_ID);

            assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.PUBLISHED);
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.OPEN);
        }

        @Test
        @DisplayName("설문이 이미 OPEN이면 변경 없음")
        void alreadyOpen_noChange() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Survey survey = createSurvey(SurveyVisibility.PUBLISHED, SurveyResponseStatus.OPEN);
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.of(survey));

            surveySyncService.openSurveyForRegistration(EVENT_ID);

            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.OPEN);
        }

        @Test
        @DisplayName("설문이 CLOSED이면 변경 없음")
        void closed_noChange() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Survey survey = createSurvey(SurveyVisibility.PUBLISHED, SurveyResponseStatus.CLOSED);
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.of(survey));

            surveySyncService.openSurveyForRegistration(EVENT_ID);

            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.CLOSED);
        }
    }

    @Nested
    @DisplayName("REGISTRATION_CANCELED_BY_ADMIN - 개별 신청 취소")
    class RegistrationCanceledByAdmin {

        @Test
        @DisplayName("설문 상태 변경 없음")
        void noSurveyChange() {
            Event event = createEventWithSurvey(SURVEY_ID);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            Survey survey = createSurvey(SurveyVisibility.PUBLISHED, SurveyResponseStatus.OPEN);
            when(surveyRepository.findByIdAndDeletedFalse(SURVEY_ID)).thenReturn(Optional.of(survey));

            surveySyncService.handleEventStatusChange(createEvent(EventChangeType.REGISTRATION_CANCELED_BY_ADMIN));

            assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.PUBLISHED);
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.OPEN);
        }
    }
}
