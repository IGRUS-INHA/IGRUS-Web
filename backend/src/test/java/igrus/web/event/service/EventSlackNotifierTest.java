package igrus.web.event.service;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.user.domain.User;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.event.repository.EventRepository;
import igrus.web.webhook.slack.SlackNotifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventSlackNotifier 단위 테스트")
class EventSlackNotifierTest {

    private static final Long EVENT_ID = 1L;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @Mock
    private SlackNotifier slackNotifier;

    @InjectMocks
    private EventSlackNotifier notifier;

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

    @Test
    @DisplayName("취소 요청: 신청자, 행사명, 사유(200자 초과분은 생략), 처리 링크를 보낸다")
    void notifyCancelRequested_SendsReasonAndLink() {
        ReflectionTestUtils.setField(notifier, "frontendUrl", "https://www.igrus.co.kr");
        Event event = mock(Event.class);
        when(event.getTitle()).thenReturn("가을 MT");
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        notifier.notifyCancelRequested(EVENT_ID, "홍길동 (20231234)", "가".repeat(201));

        verify(slackNotifier).send("🙏 행사 취소 요청\n홍길동 (20231234) → 가을 MT\n사유: "
                + "가".repeat(200) + "…\n처리: https://www.igrus.co.kr/notify");
    }

    @Test
    @DisplayName("취소 수락: 처리자와 취소 후 현재 신청 인원/정원을 보낸다")
    void notifyCancelAccepted_SendsOperatorAndCount() {
        Event event = mock(Event.class);
        when(event.getTitle()).thenReturn("가을 MT");
        when(event.getCapacity()).thenReturn(30);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.countByEventIdAndStatusIn(eq(EVENT_ID), anyCollection())).thenReturn(11L);

        notifier.notifyCancelAccepted(EVENT_ID, "홍길동 (20231234)", "김운영");

        verify(slackNotifier).send("✅ 신청 취소 처리\n홍길동 (20231234) → 가을 MT\n처리자: 김운영\n현재 신청 11 / 정원 30");
    }

    @Test
    @DisplayName("취소 요청 거절: 신청자, 행사명, 처리자를 보낸다")
    void notifyCancelRequestRejected_SendsOperator() {
        Event event = mock(Event.class);
        when(event.getTitle()).thenReturn("가을 MT");
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        notifier.notifyCancelRequestRejected(EVENT_ID, "홍길동 (20231234)", "김운영");

        verify(slackNotifier).send("↩️ 취소 요청 거절\n홍길동 (20231234) → 가을 MT\n처리자: 김운영");
    }

    @Test
    @DisplayName("행사 등록: 일시와 모집 정보를 한국 시간으로 보낸다")
    void notifyEventCreated_SendsScheduleInKst() {
        Event event = mock(Event.class);
        when(event.getTitle()).thenReturn("가을 MT");
        when(event.getRegistrationType()).thenReturn(EventRegistrationType.AUTO_APPROVE);
        when(event.getCapacity()).thenReturn(40);
        when(event.getEventStartAt()).thenReturn(Instant.parse("2026-10-24T05:00:00Z"));
        when(event.getEventEndAt()).thenReturn(Instant.parse("2026-10-25T03:00:00Z"));
        when(event.getRegistrationEndAt()).thenReturn(Instant.parse("2026-10-20T14:59:00Z"));

        notifier.notifyEventCreated(event, "김운영");

        verify(slackNotifier).send("📅 새 행사 등록\n가을 MT\n"
                + "일시: 10/24(토) 14:00 ~ 10/25(일) 12:00\n"
                + "모집: 선착순 · 정원 40명 · ~10/20(화) 23:59 마감\n"
                + "작성자: 김운영");
    }

    @Test
    @DisplayName("신청자 표기: 회원, 외부인, 탈퇴 회원을 구분한다")
    void applicantLabel_MemberExternalWithdrawn() {
        User user = mock(User.class);
        when(user.getName()).thenReturn("홍길동");
        when(user.getStudentId()).thenReturn("20231234");
        EventRegistration member = mock(EventRegistration.class);
        when(member.getUser()).thenReturn(user);

        EventRegistration external = mock(EventRegistration.class);
        when(external.getIsExternal()).thenReturn(true);
        when(external.getExternalName()).thenReturn("김외부");
        when(external.getExternalStudentId()).thenReturn("20249999");

        User deleted = mock(User.class);
        when(deleted.getName()).thenThrow(new EntityNotFoundException("Unable to find User"));
        EventRegistration withdrawn = mock(EventRegistration.class);
        when(withdrawn.getUser()).thenReturn(deleted);

        assertThat(EventSlackNotifier.applicantLabel(member)).isEqualTo("홍길동 (20231234)");
        assertThat(EventSlackNotifier.applicantLabel(external)).isEqualTo("김외부 (20249999, 외부인)");
        assertThat(EventSlackNotifier.applicantLabel(withdrawn)).isEqualTo("탈퇴한 사용자");
    }
}
