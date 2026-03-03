package igrus.web.community.bookmark.dto.response;

import igrus.web.community.bookmark.domain.Bookmark;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;

import java.time.Instant;

/**
 * 북마크한 게시글 목록 조회 응답 DTO.
 */
public record BookmarkedPostResponse(
    Long postId,

    String title,

    String boardCode,

    String boardName,

    String authorName,

    Instant createdAt,

    boolean isDeleted,

    String deletedMessage
) {
    public static BookmarkedPostResponse from(Bookmark bookmark) {
        Post post = bookmark.getPost();
        boolean isDeleted = post.isDeleted();

        return new BookmarkedPostResponse(
            post.getId(),
            isDeleted ? null : post.getTitle(),
            post.getBoard().getCode().name(),
            post.getBoard().getName(),
            isDeleted ? null : (post.isAnonymous() ? "익명" : (post.getAuthor() != null ? post.getAuthor().getDisplayName() : User.WITHDRAWN_DISPLAY_NAME)),
            post.getCreatedAt(),
            isDeleted,
            isDeleted ? "삭제된 게시글입니다" : null
        );
    }
}
