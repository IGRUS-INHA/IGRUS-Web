package igrus.web.community.like.post_like.dto.response;

/**
 * 게시글 좋아요 상태 조회 응답 DTO.
 */
public record PostLikeStatusResponse(
    boolean liked,

    int likeCount
) {
    public static PostLikeStatusResponse of(boolean liked, int likeCount) {
        return new PostLikeStatusResponse(liked, likeCount);
    }
}
