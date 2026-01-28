package igrus.web.community.like.postlike.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 게시글 좋아요 상태 조회 응답 DTO.
 */
@Schema(description = "게시글 좋아요 상태 조회 응답")
public record PostLikeStatusResponse(
    @Schema(description = "현재 좋아요 상태", example = "true")
    boolean liked,

    @Schema(description = "게시글 총 좋아요 수", example = "42")
    int likeCount
) {
    public static PostLikeStatusResponse of(boolean liked, int likeCount) {
        return new PostLikeStatusResponse(liked, likeCount);
    }
}
