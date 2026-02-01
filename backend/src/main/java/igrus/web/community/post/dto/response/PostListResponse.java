package igrus.web.community.post.dto.response;

import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;
import java.time.Instant;

/**
 * 게시글 목록 조회 응답 DTO.
 * 목록 화면에서 보여줄 게시글 정보를 담습니다.
 */
public record PostListResponse(
    Long postId,
    String title,
    String authorName,
    boolean isAnonymous,
    boolean isQuestion,
    int viewCount,
    int likeCount,
    int commentCount,
    Instant createdAt
) {
    /**
     * Post 엔티티로부터 PostListResponse를 생성합니다.
     * 익명 게시글의 경우 작성자 이름을 "익명"으로 표시합니다.
     *
     * @param post 게시글 엔티티
     * @return PostListResponse
     */
    public static PostListResponse from(Post post) {
        return new PostListResponse(
            post.getId(),
            post.getTitle(),
            post.isAnonymous() ? "익명" : (post.getAuthor() != null ? post.getAuthor().getDisplayName() : User.WITHDRAWN_DISPLAY_NAME),
            post.isAnonymous(),
            post.isQuestion(),
            post.getViewCount(),
            0,  // likeCount - 추후 구현
            0,  // commentCount - 추후 구현
            post.getCreatedAt()
        );
    }
}
