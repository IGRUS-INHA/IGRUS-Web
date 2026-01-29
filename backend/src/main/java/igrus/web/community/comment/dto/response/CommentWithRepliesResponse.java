package igrus.web.community.comment.dto.response;

import igrus.web.community.comment.domain.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 대댓글을 포함한 댓글 응답 DTO.
 */
@Getter
@Builder
public class CommentWithRepliesResponse {

    private Long id;
    private Long postId;
    private String content;
    private Long authorId;
    private String authorName;
    private boolean isAnonymous;
    private boolean isDeleted;
    private long likeCount;
    private boolean isLikedByMe;
    private Instant createdAt;
    @Builder.Default
    private List<CommentResponse> replies = new ArrayList<>();

    private static final String DELETED_CONTENT = "삭제된 댓글입니다";
    private static final String ANONYMOUS_NAME = "익명";

    /**
     * Comment 엔티티를 CommentWithRepliesResponse로 변환합니다.
     *
     * @param comment     댓글 엔티티
     * @param likeCount   좋아요 수
     * @param isLikedByMe 현재 사용자의 좋아요 여부
     * @return CommentWithRepliesResponse
     */
    public static CommentWithRepliesResponse from(Comment comment, long likeCount, boolean isLikedByMe) {
        if (comment.isDeleted()) {
            return CommentWithRepliesResponse.builder()
                    .id(comment.getId())
                    .postId(comment.getPost().getId())
                    .content(DELETED_CONTENT)
                    .authorId(null)
                    .authorName(null)
                    .isAnonymous(comment.isAnonymous())
                    .isDeleted(true)
                    .likeCount(likeCount)
                    .isLikedByMe(isLikedByMe)
                    .createdAt(comment.getCreatedAt())
                    .replies(new ArrayList<>())
                    .build();
        }

        return CommentWithRepliesResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .content(comment.getContent())
                .authorId(comment.isAnonymous() ? null : comment.getAuthor().getId())
                .authorName(comment.isAnonymous() ? ANONYMOUS_NAME : comment.getAuthor().getName())
                .isAnonymous(comment.isAnonymous())
                .isDeleted(false)
                .likeCount(likeCount)
                .isLikedByMe(isLikedByMe)
                .createdAt(comment.getCreatedAt())
                .replies(new ArrayList<>())
                .build();
    }

    /**
     * 대댓글을 추가합니다.
     *
     * @param reply 대댓글 응답
     */
    public void addReply(CommentResponse reply) {
        this.replies.add(reply);
    }
}
