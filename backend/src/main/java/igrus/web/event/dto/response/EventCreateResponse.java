package igrus.web.event.dto.response;

import igrus.web.event.domain.Event;

import java.time.Instant;

/**
 * 행사 생성 응답 DTO.
 * 행사 생성 완료 후 반환되는 정보를 담습니다.
 *
 * @param id        생성된 행사 ID
 * @param title     행사 제목
 * @param createdAt 생성일시
 */
public record EventCreateResponse(
        Long id,
        String title,
        Instant createdAt
) {
    /**
     * Event 엔티티로부터 EventCreateResponse를 생성합니다.
     *
     * @param event 행사 엔티티
     * @return EventCreateResponse
     */
    public static EventCreateResponse from(Event event) {
        return new EventCreateResponse(
                event.getId(),
                event.getTitle(),
                event.getCreatedAt()
        );
    }
}
