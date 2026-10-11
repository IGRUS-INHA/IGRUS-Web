package igrus.web.event.service;

import igrus.web.event.domain.EventRegistrationStatus;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.event.repository.EventRepository;
import igrus.web.webhook.slack.SlackNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 행사 신청이 접수되면 운영진 슬랙 채널에 알린다.
 */
@Component
@RequiredArgsConstructor
public class EventRegistrationSlackNotifier {

    private static final List<EventRegistrationStatus> ACTIVE_STATUSES = List.of(
            EventRegistrationStatus.REGISTERED,
            EventRegistrationStatus.WAITING,
            EventRegistrationStatus.APPROVED
    );

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final SlackNotifier slackNotifier;

    /**
     * @param eventId   신청한 행사 ID
     * @param applicant 신청자 표기 (예: "홍길동 (20231234)")
     */
    public void notifyRegistered(Long eventId, String applicant) {
        eventRepository.findById(eventId).ifPresent(event -> {
            // 승인 대기 포함, 취소/거절 제외한 현재 신청 인원
            long count = eventRegistrationRepository.countByEventIdAndStatusIn(eventId, ACTIVE_STATUSES);
            slackNotifier.send("🙋 행사 신청\n"
                    + applicant + " → " + event.getTitle() + "\n"
                    + "현재 신청 " + count + " / 정원 " + event.getCapacity());
        });
    }
}
