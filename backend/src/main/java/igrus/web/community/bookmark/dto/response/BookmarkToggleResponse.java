package igrus.web.community.bookmark.dto.response;

/**
 * 북마크 토글 응답 DTO.
 */
public record BookmarkToggleResponse(
    boolean bookmarked
) {
    public static BookmarkToggleResponse of(boolean bookmarked) {
        return new BookmarkToggleResponse(bookmarked);
    }
}
