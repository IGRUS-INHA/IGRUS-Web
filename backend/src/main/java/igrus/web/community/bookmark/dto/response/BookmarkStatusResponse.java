package igrus.web.community.bookmark.dto.response;

/**
 * 북마크 상태 조회 응답 DTO.
 */
public record BookmarkStatusResponse(
    boolean bookmarked
) {
    public static BookmarkStatusResponse of(boolean bookmarked) {
        return new BookmarkStatusResponse(bookmarked);
    }
}
