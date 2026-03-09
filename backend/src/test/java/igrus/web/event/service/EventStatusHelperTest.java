package igrus.web.event.service;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.RegistrationStatus;
import igrus.web.event.repository.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.*;

/**
 * EventStatusHelper 단위 테스트.
 * 시간 기반 상태 자동 갱신 시 설문 동기화 호출을 검증합니다.
 */
@DisplayName("EventStatusHelper")
@ExtendWith(MockitoExtension.class)
class EventStatusHelperTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventSurveySyncService surveySyncService;

    @InjectMocks
    private EventStatusHelper eventStatusHelper;

    @Nested
    @DisplayName("updateStatusIfNeeded - 상태 갱신 및 설문 동기화")
    class UpdateStatusIfNeeded {

        @Test
        @DisplayName("NOT_STARTED에서 OPEN으로 전이 시 설문 동기화 호출")
        void notStartedToOpen_callsSurveySync() {
            Event event = mock(Event.class);
            when(event.getId()).thenReturn(1L);
            when(event.getRegistrationStatus())
                    .thenReturn(RegistrationStatus.NOT_STARTED)
                    .thenReturn(RegistrationStatus.OPEN);

            Instant now = Instant.now();
            eventStatusHelper.updateStatusIfNeeded(event, now);

            verify(event).updateStatusIfNeeded(now);
            verify(surveySyncService).openSurveyForRegistration(1L);
        }

        @Test
        @DisplayName("상태 변경 없으면 설문 동기화 호출하지 않음")
        void noChange_doesNotCallSurveySync() {
            Event event = mock(Event.class);
            when(event.getRegistrationStatus()).thenReturn(RegistrationStatus.NOT_STARTED);

            eventStatusHelper.updateStatusIfNeeded(event, Instant.now());

            verify(surveySyncService, never()).openSurveyForRegistration(anyLong());
        }

        @Test
        @DisplayName("이미 OPEN 상태이면 설문 동기화 호출하지 않음")
        void alreadyOpen_doesNotCallSurveySync() {
            Event event = mock(Event.class);
            when(event.getRegistrationStatus()).thenReturn(RegistrationStatus.OPEN);

            eventStatusHelper.updateStatusIfNeeded(event, Instant.now());

            verify(surveySyncService, never()).openSurveyForRegistration(anyLong());
        }

        @Test
        @DisplayName("OPEN에서 CLOSED로 전이 시 설문 동기화 호출하지 않음")
        void openToClosed_doesNotCallSurveySync() {
            Event event = mock(Event.class);
            when(event.getRegistrationStatus())
                    .thenReturn(RegistrationStatus.OPEN)
                    .thenReturn(RegistrationStatus.CLOSED);

            eventStatusHelper.updateStatusIfNeeded(event, Instant.now());

            verify(surveySyncService, never()).openSurveyForRegistration(anyLong());
        }
    }
}
