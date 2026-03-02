package igrus.web.user.mypage.dto.response;

import igrus.web.community.post.domain.Post;

import java.time.Instant;

/**
 * 내 게시글 목록 응답 DTO.
 */
public record MyPostResponse(
        Long id,

        String boardCode,

        String boardName,

        String title,

        int viewCount,

        int likeCount,

        boolean isAnonymous,

        Instant createdAt
) {
    public static MyPostResponse from(Post post) {
        return new MyPostResponse(
                post.getId(),
                post.getBoard().getCode().name(),
                post.getBoard().getName(),
                post.getTitle(),
                post.getViewCount(),
                post.getLikeCount(),
                post.isAnonymous(),
                post.getCreatedAt()
        );
    }
}
