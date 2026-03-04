package igrus.web.event.dto.response;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.domain.EventStatus;
import igrus.web.event.domain.EventVisibility;
import igrus.web.event.domain.RegistrationStatus;

import java.time.Instant;

/**
 * 행사 목록 조회 응답 DTO.
 * 목록 화면에서 보여줄 행사 요약 정보를 담습니다.
 *
 * @param id                  행사 ID
 * @param title               행사 제목
 * @param location            행사 장소
 * @param eventStartAt        행사 시작일시
 * @param eventEndAt          행사 종료일시
 * @param registrationEndAt   신청 마감일시
 * @param capacity            정원
 * @param currentCount        현재 신청자 수
 * @param visibility          공개 상태 (축 1)
 * @param registrationStatus  등록 상태 (축 2)
 * @param eventStatus         행사 진행 상태 (축 3)
 * @param registrationType    신청 방식 (선착순/선발제)
 * @param isRegistrable       신청 가능 여부
 * @param surveyId            연결된 설문 ID (null이면 설문 미연결)
 */
public record EventListResponse(
        Long id,
        String title,
        String location,
        Instant eventStartAt,
        Instant eventEndAt,
        Instant registrationEndAt,
        int capacity,
        int currentCount,
        EventVisibility visibility,
        RegistrationStatus registrationStatus,
        EventStatus eventStatus,
        EventRegistrationType registrationType,
        boolean isRegistrable,
        Long surveyId
) {
    /**
     * Event 엔티티로부터 EventListResponse를 생성합니다.
     *
     * @param event 행사 엔티티
     * @return EventListResponse
     */
    public static EventListResponse from(Event event) {
        return new EventListResponse(
                event.getId(),
                event.getTitle(),
                event.getLocation(),
                event.getEventStartAt(),
                event.getEventEndAt(),
                event.getRegistrationEndAt(),
                event.getCapacity(),
                event.getCurrentCount(),
                event.getVisibility(),
                event.getRegistrationStatus(),
                event.getEventStatus(),
                event.getRegistrationType(),
                event.isRegistrable(),
                event.getSurveyId()
        );
    }
}
