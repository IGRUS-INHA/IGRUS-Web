package igrus.web.event.dto.response;

import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationStatus;

import java.time.Instant;

/**
 * 내 신청 목록 응답 DTO.
 * 사용자가 자신의 신청 내역을 조회할 때 사용합니다.
 *
 * @param registrationId 신청 ID
 * @param eventId        행사 ID
 * @param eventTitle     행사 제목
 * @param eventStartAt   행사 시작일시
 * @param status         신청 상태
 * @param registeredAt   신청일시
 */
public record MyRegistrationResponse(
        Long registrationId,
        Long eventId,
        String eventTitle,
        Instant eventStartAt,
        EventRegistrationStatus status,
        Instant registeredAt
) {
    /**
     * EventRegistration 엔티티로부터 MyRegistrationResponse를 생성합니다.
     *
     * @param registration 신청 엔티티
     * @return MyRegistrationResponse
     */
    public static MyRegistrationResponse from(EventRegistration registration) {
        return new MyRegistrationResponse(
                registration.getId(),
                registration.getEvent().getId(),
                registration.getEvent().getTitle(),
                registration.getEvent().getEventStartAt(),
                registration.getStatus(),
                registration.getRegisteredAt()
        );
    }
}
