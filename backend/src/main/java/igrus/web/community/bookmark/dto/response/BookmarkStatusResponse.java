package igrus.web.community.bookmark.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 북마크 상태 조회 응답 DTO.
 */
@Schema(description = "북마크 상태 조회 응답")
public record BookmarkStatusResponse(
    @Schema(description = "현재 북마크 상태", example = "true")
    boolean bookmarked
) {
    public static BookmarkStatusResponse of(boolean bookmarked) {
        return new BookmarkStatusResponse(bookmarked);
    }
}
