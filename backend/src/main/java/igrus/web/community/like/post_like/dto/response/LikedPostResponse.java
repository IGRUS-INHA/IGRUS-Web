package igrus.web.community.like.post_like.dto.response;

import igrus.web.community.like.post_like.domain.PostLike;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;

import java.time.Instant;

/**
 * 좋아요한 게시글 목록 조회 응답 DTO.
 */
public record LikedPostResponse(
    Long postId,

    String title,

    String boardCode,

    String boardName,

    String authorName,

    int likeCount,

    Instant createdAt,

    boolean isDeleted,

    String deletedMessage
) {
    public static LikedPostResponse from(PostLike postLike) {
        Post post = postLike.getPost();
        boolean isDeleted = post.isDeleted();

        return new LikedPostResponse(
            post.getId(),
            isDeleted ? null : post.getTitle(),
            post.getBoard().getCode().name(),
            post.getBoard().getName(),
            isDeleted ? null : (post.isAnonymous() ? "익명" : (post.getAuthor() != null ? post.getAuthor().getDisplayName() : User.WITHDRAWN_DISPLAY_NAME)),
            post.getLikeCount(),
            post.getCreatedAt(),
            isDeleted,
            isDeleted ? "삭제된 게시글입니다" : null
        );
    }
}
