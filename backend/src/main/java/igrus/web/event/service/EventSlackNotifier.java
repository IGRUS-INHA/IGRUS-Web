package igrus.web.event.service;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationStatus;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.event.repository.EventRepository;
import igrus.web.webhook.slack.SlackNotifier;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * 행사 등록, 신청, 취소 요청과 그 처리 결과를 운영진 슬랙 채널에 알린다.
 */
@Component
@RequiredArgsConstructor
public class EventSlackNotifier {

    private static final List<EventRegistrationStatus> ACTIVE_STATUSES = List.of(
            EventRegistrationStatus.REGISTERED,
            EventRegistrationStatus.WAITING,
            EventRegistrationStatus.APPROVED
    );

    private static final int CANCEL_REASON_MAX_LENGTH = 200;

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter
            .ofPattern("M/d(E) HH:mm", Locale.KOREAN)
            .withZone(ZoneId.of("Asia/Seoul"));

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final SlackNotifier slackNotifier;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * 신청자 표기를 만든다. (예: "홍길동 (20231234)", "김외부 (20249999, 외부인)")
     * 원자적 UPDATE로 영속성 컨텍스트가 초기화되기 전에 호출해야 한다.
     */
    public static String applicantLabel(EventRegistration registration) {
        if (Boolean.TRUE.equals(registration.getIsExternal())) {
            return registration.getExternalName() + " (" + registration.getExternalStudentId() + ", 외부인)";
        }
        try {
            return registration.getUser().getName() + " (" + registration.getUser().getStudentId() + ")";
        } catch (EntityNotFoundException e) {
            // 탈퇴(soft delete)한 회원은 조회되지 않는다
            return "탈퇴한 사용자";
        }
    }

    public void notifyEventCreated(Event event, String creatorName) {
        String registrationType = event.getRegistrationType() == EventRegistrationType.AUTO_APPROVE
                ? "선착순" : "선발제";
        slackNotifier.send("📅 새 행사 등록\n"
                + event.getTitle() + "\n"
                + "일시: " + format(event.getEventStartAt()) + " ~ " + format(event.getEventEndAt()) + "\n"
                + "모집: " + registrationType + " · 정원 " + event.getCapacity() + "명 · ~"
                + format(event.getRegistrationEndAt()) + " 마감\n"
                + "작성자: " + creatorName);
    }

    /**
     * @param eventId   신청한 행사 ID
     * @param applicant 신청자 표기 (예: "홍길동 (20231234)")
     */
    public void notifyRegistered(Long eventId, String applicant) {
        eventRepository.findById(eventId).ifPresent(event ->
                slackNotifier.send("🙋 행사 신청\n"
                        + applicant + " → " + event.getTitle() + "\n"
                        + currentCountLine(eventId, event)));
    }

    public void notifyCancelRequested(Long eventId, String applicant, String reason) {
        String shortReason = reason.length() > CANCEL_REASON_MAX_LENGTH
                ? reason.substring(0, CANCEL_REASON_MAX_LENGTH) + "…"
                : reason;
        eventRepository.findById(eventId).ifPresent(event ->
                slackNotifier.send("🙏 행사 취소 요청\n"
                        + applicant + " → " + event.getTitle() + "\n"
                        + "사유: " + shortReason + "\n"
                        + "처리: " + frontendUrl + "/notify"));
    }

    public void notifyCancelAccepted(Long eventId, String applicant, String operatorName) {
        eventRepository.findById(eventId).ifPresent(event ->
                slackNotifier.send("✅ 신청 취소 처리\n"
                        + applicant + " → " + event.getTitle() + "\n"
                        + "처리자: " + operatorName + "\n"
                        + currentCountLine(eventId, event)));
    }

    public void notifyCancelRequestRejected(Long eventId, String applicant, String operatorName) {
        eventRepository.findById(eventId).ifPresent(event ->
                slackNotifier.send("↩️ 취소 요청 거절\n"
                        + applicant + " → " + event.getTitle() + "\n"
                        + "처리자: " + operatorName));
    }

    private String currentCountLine(Long eventId, Event event) {
        // 승인 대기 포함, 취소/거절 제외한 현재 신청 인원
        long count = eventRegistrationRepository.countByEventIdAndStatusIn(eventId, ACTIVE_STATUSES);
        return "현재 신청 " + count + " / 정원 " + event.getCapacity();
    }

    private static String format(Instant instant) {
        return DATE_TIME_FORMAT.format(instant);
    }
}
