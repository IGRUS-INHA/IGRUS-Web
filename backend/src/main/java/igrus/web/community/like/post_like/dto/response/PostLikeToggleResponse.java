package igrus.web.community.like.post_like.dto.response;

/**
 * 게시글 좋아요 토글 응답 DTO.
 */
public record PostLikeToggleResponse(
    boolean liked,

    int likeCount
) {
    public static PostLikeToggleResponse of(boolean liked, int likeCount) {
        return new PostLikeToggleResponse(liked, likeCount);
    }
}
