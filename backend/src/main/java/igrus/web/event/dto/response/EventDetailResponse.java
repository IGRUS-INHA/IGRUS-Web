package igrus.web.event.dto.response;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventCloseReason;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.domain.EventStatus;

import java.time.Instant;

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
 * @param status              행사 상태
 * @param closeReason         마감 사유 (CLOSED 상태일 때만)
 * @param registrationType    신청 방식 (선착순/선발제)
 * @param isRegistrable       신청 가능 여부
 * @param createdAt           생성일시
 * @param updatedAt           수정일시
 * @param isAuthor            현재 사용자가 작성자인지 여부
 * @param canEdit             현재 사용자가 수정 가능한지 여부
 * @param isRegistered        현재 사용자가 신청했는지 여부
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
        EventStatus status,
        EventCloseReason closeReason,
        EventRegistrationType registrationType,
        boolean isRegistrable,
        Instant createdAt,
        Instant updatedAt,
        boolean isAuthor,
        boolean canEdit,
        boolean isRegistered
) {
    /**
     * Event 엔티티로부터 EventDetailResponse를 생성합니다.
     * 사용자 권한 정보 포함 버전.
     *
     * @param event        행사 엔티티
     * @param isAuthor     현재 사용자가 작성자인지 여부
     * @param canEdit      현재 사용자가 수정 가능한지 여부
     * @param isRegistered 현재 사용자가 신청했는지 여부
     * @return EventDetailResponse
     */
    public static EventDetailResponse from(Event event, boolean isAuthor, boolean canEdit, boolean isRegistered) {
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
                event.getStatus(),
                event.getCloseReason(),
                event.getRegistrationType(),
                event.isRegistrable(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                isAuthor,
                canEdit,
                isRegistered
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
        return from(event, false, false, false);
    }
}
