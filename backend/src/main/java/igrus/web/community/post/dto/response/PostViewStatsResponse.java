package igrus.web.community.post.dto.response;

/**
 * 게시글 조회 통계 응답 DTO.
 *
 * @param postId        게시글 ID
 * @param totalViews    총 조회 수
 * @param uniqueViewers 고유 조회자 수
 */
public record PostViewStatsResponse(
    Long postId,
    long totalViews,
    long uniqueViewers
) {

    /**
     * 조회 통계 응답을 생성합니다.
     *
     * @param postId        게시글 ID
     * @param totalViews    총 조회 수
     * @param uniqueViewers 고유 조회자 수
     * @return 조회 통계 응답
     */
    public static PostViewStatsResponse of(Long postId, long totalViews, long uniqueViewers) {
        return new PostViewStatsResponse(postId, totalViews, uniqueViewers);
    }
}
