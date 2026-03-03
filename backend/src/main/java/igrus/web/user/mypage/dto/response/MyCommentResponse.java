package igrus.web.user.mypage.dto.response;

import igrus.web.community.comment.domain.Comment;

import java.time.Instant;

/**
 * 내 댓글 목록 응답 DTO.
 */
public record MyCommentResponse(
        Long id,

        Long postId,

        String postTitle,

        String content,

        boolean isAnonymous,

        boolean isReply,

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
