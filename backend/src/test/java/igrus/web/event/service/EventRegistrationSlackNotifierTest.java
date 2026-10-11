package igrus.web.event.service;

import igrus.web.event.domain.Event;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.event.repository.EventRepository;
import igrus.web.webhook.slack.SlackNotifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventRegistrationSlackNotifier 단위 테스트")
class EventRegistrationSlackNotifierTest {

    private static final Long EVENT_ID = 1L;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @Mock
    private SlackNotifier slackNotifier;

    @InjectMocks
    private EventRegistrationSlackNotifier notifier;

    @Test
    @DisplayName("신청자, 행사명, 현재 신청 인원/정원을 슬랙으로 보낸다")
    void notifyRegistered_SendsApplicantEventAndCount() {
        Event event = mock(Event.class);
        when(event.getTitle()).thenReturn("가을 MT");
        when(event.getCapacity()).thenReturn(30);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.countByEventIdAndStatusIn(eq(EVENT_ID), anyCollection())).thenReturn(12L);

        notifier.notifyRegistered(EVENT_ID, "홍길동 (20231234)");

        verify(slackNotifier).send("🙋 행사 신청\n홍길동 (20231234) → 가을 MT\n현재 신청 12 / 정원 30");
    }

    @Test
    @DisplayName("행사를 찾을 수 없으면 알림을 보내지 않는다")
    void notifyRegistered_EventMissing_SendsNothing() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        notifier.notifyRegistered(EVENT_ID, "홍길동 (20231234)");

        verifyNoInteractions(slackNotifier);
    }
}
