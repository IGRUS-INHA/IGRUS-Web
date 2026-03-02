package igrus.web.community.bookmark.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 북마크한 게시글 목록 페이징 응답 DTO.
 */
public record BookmarkedPostPageResponse(
    List<BookmarkedPostResponse> posts,

    long totalElements,

    int totalPages,

    int currentPage,

    boolean hasNext
) {
    public static BookmarkedPostPageResponse from(Page<BookmarkedPostResponse> page) {
        return new BookmarkedPostPageResponse(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.hasNext()
        );
    }
}
