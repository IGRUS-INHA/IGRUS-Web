package igrus.web.event.dto.response;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventCloseReason;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.domain.EventStatus;
import igrus.web.event.domain.EventVisibility;
import igrus.web.event.domain.RegistrationStatus;

import java.time.Instant;
import java.util.List;

/**
 * 행사 상세 조회 응답 DTO.
 * 행사의 전체 정보를 담습니다.
 *
 * @param id                  행사 ID
 * @param title               행사 제목
 * @param description         행사 설명
 * @param location            행사 장소
 * @param authorName          작성자 이름
 * @param eventStartAt        행사 시작일시
 * @param eventEndAt          행사 종료일시
 * @param registrationStartAt 신청 시작일시
 * @param registrationEndAt   신청 마감일시
 * @param capacity            정원
 * @param currentCount        현재 신청자 수
 * @param visibility          공개 상태 (축 1)
 * @param registrationStatus  등록 상태 (축 2)
 * @param eventStatus         행사 진행 상태 (축 3)
 * @param closeReason         마감 사유 (CLOSED 상태일 때만)
 * @param registrationType    신청 방식 (선착순/선발제)
 * @param isRegistrable       신청 가능 여부
 * @param createdAt           생성일시
 * @param updatedAt           수정일시
 * @param canEdit             현재 사용자가 수정 가능한지 여부
 * @param isRegistered        현재 사용자가 신청했는지 여부
 * @param surveyId            연결된 설문 ID (null이면 설문 미연결)
 * @param allowExternal       외부인 신청 허용 여부
 */
public record EventDetailResponse(
        Long id,
        String title,
        String description,
        String location,
        String authorName,
        Instant eventStartAt,
        Instant eventEndAt,
        Instant registrationStartAt,
        Instant registrationEndAt,
        int capacity,
        int currentCount,
        EventVisibility visibility,
        RegistrationStatus registrationStatus,
        EventStatus eventStatus,
        EventCloseReason closeReason,
        EventRegistrationType registrationType,
        boolean isRegistrable,
        Instant createdAt,
        Instant updatedAt,
        boolean canEdit,
        boolean isRegistered,
        Long surveyId,
        List<EventAttachmentDto> attachments,
        Boolean allowExternal
) {
    /**
     * Event 엔티티로부터 EventDetailResponse를 생성합니다.
     * 사용자 권한 정보 포함 버전.
     *
     * @param event        행사 엔티티
     * @param canEdit      현재 사용자가 수정 가능한지 여부
     * @param isRegistered 현재 사용자가 신청했는지 여부
     * @return EventDetailResponse
     */
    public static EventDetailResponse from(Event event, boolean canEdit, boolean isRegistered) {
        return from(event, canEdit, isRegistered, List.of());
    }

    public static EventDetailResponse from(Event event, boolean canEdit, boolean isRegistered,
                                            List<EventAttachmentDto> attachments) {
        return new EventDetailResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getUser().getName(),
                event.getEventStartAt(),
                event.getEventEndAt(),
                event.getRegistrationStartAt(),
                event.getRegistrationEndAt(),
                event.getCapacity(),
                event.getCurrentCount(),
                event.getVisibility(),
                event.getRegistrationStatus(),
                event.getEventStatus(),
                event.getRegistrationStatus() == RegistrationStatus.CLOSED ? event.getCloseReason() : null,
                event.getRegistrationType(),
                event.isRegistrable(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                canEdit,
                isRegistered,
                event.getSurveyId(),
                attachments,
                event.getAllowExternal()
        );
    }

    /**
     * Event 엔티티로부터 EventDetailResponse를 생성합니다.
     * 사용자 권한 정보 없이 생성 (기본값: false).
     *
     * @param event 행사 엔티티
     * @return EventDetailResponse
     */
    public static EventDetailResponse from(Event event) {
        return from(event, false, false, List.of());
    }
}
