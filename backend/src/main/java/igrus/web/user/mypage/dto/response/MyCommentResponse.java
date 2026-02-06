package igrus.web.user.mypage.dto.response;

import igrus.web.community.comment.domain.Comment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 내 댓글 목록 응답 DTO.
 */
@Schema(description = "내 댓글 정보")
public record MyCommentResponse(
        @Schema(description = "댓글 ID", example = "1")
        Long id,

        @Schema(description = "게시글 ID", example = "10")
        Long postId,

        @Schema(description = "게시글 제목", example = "첫 번째 게시글")
        String postTitle,

        @Schema(description = "댓글 내용", example = "좋은 글이네요!")
        String content,

        @Schema(description = "익명 여부", example = "false")
        boolean isAnonymous,

        @Schema(description = "대댓글 여부", example = "false")
        boolean isReply,

        @Schema(description = "작성일")
        Instant createdAt
) {
    public static MyCommentResponse from(Comment comment) {
        return new MyCommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getPost().getTitle(),
                comment.getContent(),
                comment.isAnonymous(),
                comment.isReply(),
                comment.getCreatedAt()
        );
    }
}
