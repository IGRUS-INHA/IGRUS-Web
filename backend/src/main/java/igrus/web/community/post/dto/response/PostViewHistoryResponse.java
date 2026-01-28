package igrus.web.community.post.dto.response;

import igrus.web.community.post.domain.PostView;

import java.time.Instant;

/**
 * 게시글 조회 기록 응답 DTO.
 *
 * @param viewId     조회 기록 ID
 * @param viewerId   조회자 ID
 * @param viewerName 조회자 이름
 * @param viewedAt   조회 시각
 */
public record PostViewHistoryResponse(
    Long viewId,
    Long viewerId,
    String viewerName,
    Instant viewedAt
) {

    /**
     * PostView 엔티티로부터 응답 DTO를 생성합니다.
     *
     * @param postView 조회 기록 엔티티
     * @return 조회 기록 응답 DTO
     */
    public static PostViewHistoryResponse from(PostView postView) {
        return new PostViewHistoryResponse(
            postView.getId(),
            postView.getViewer().getId(),
            postView.getViewer().getName(),
            postView.getViewedAt()
        );
    }
}
