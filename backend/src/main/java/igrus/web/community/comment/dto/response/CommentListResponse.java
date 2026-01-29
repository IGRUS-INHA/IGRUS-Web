package igrus.web.community.comment.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 댓글 목록 응답 DTO.
 */
@Getter
@Builder
public class CommentListResponse {

    private List<CommentWithRepliesResponse> comments;
    private long totalCount;

    /**
     * 댓글 목록 응답을 생성합니다.
     *
     * @param comments   댓글 목록 (계층 구조)
     * @param totalCount 전체 댓글 수 (삭제되지 않은 댓글만)
     * @return CommentListResponse
     */
    public static CommentListResponse of(List<CommentWithRepliesResponse> comments, long totalCount) {
        return CommentListResponse.builder()
                .comments(comments)
                .totalCount(totalCount)
                .build();
    }
}
