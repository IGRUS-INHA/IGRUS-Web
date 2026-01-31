package igrus.web.community.comment.dto.response;

import igrus.web.community.comment.domain.Comment;
import igrus.web.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 댓글 응답 DTO.
 */
@Getter
@Builder
public class CommentResponse {

    private Long id;
    private Long postId;
    private Long parentCommentId;
    private String content;
    private Long authorId;
    private String authorName;
    private boolean isAnonymous;
    private boolean isDeleted;
    private long likeCount;
    private boolean isLikedByMe;
    private Instant createdAt;

    private static final String DELETED_CONTENT = "삭제된 댓글입니다";
    private static final String ANONYMOUS_NAME = "익명";

    /**
     * Comment 엔티티를 CommentResponse로 변환합니다.
     *
     * @param comment      댓글 엔티티
     * @param likeCount    좋아요 수
     * @param isLikedByMe  현재 사용자의 좋아요 여부
     * @return CommentResponse
     */
    public static CommentResponse from(Comment comment, long likeCount, boolean isLikedByMe) {
        if (comment.isDeleted()) {
            return CommentResponse.builder()
                    .id(comment.getId())
                    .postId(comment.getPost().getId())
                    .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                    .content(DELETED_CONTENT)
                    .authorId(null)
                    .authorName(null)
                    .isAnonymous(comment.isAnonymous())
                    .isDeleted(true)
                    .likeCount(likeCount)
                    .isLikedByMe(isLikedByMe)
                    .createdAt(comment.getCreatedAt())
                    .build();
        }

        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .content(comment.getContent())
                .authorId(comment.isAnonymous() ? null : (comment.getAuthor() != null ? comment.getAuthor().getDisplayId() : null))
                .authorName(comment.isAnonymous() ? ANONYMOUS_NAME : (comment.getAuthor() != null ? comment.getAuthor().getDisplayName() : User.WITHDRAWN_DISPLAY_NAME))
                .isAnonymous(comment.isAnonymous())
                .isDeleted(false)
                .likeCount(likeCount)
                .isLikedByMe(isLikedByMe)
                .createdAt(comment.getCreatedAt())
                .build();
    }

    /**
     * Comment 엔티티를 관리자용 CommentResponse로 변환합니다.
     * 익명 댓글도 실제 작성자 정보를 포함합니다.
     *
     * @param comment      댓글 엔티티
     * @param likeCount    좋아요 수
     * @param isLikedByMe  현재 사용자의 좋아요 여부
     * @return CommentResponse
     */
    public static CommentResponse forAdmin(Comment comment, long likeCount, boolean isLikedByMe) {
        if (comment.isDeleted()) {
            return CommentResponse.builder()
                    .id(comment.getId())
                    .postId(comment.getPost().getId())
                    .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                    .content(DELETED_CONTENT)
                    .authorId(comment.getAuthor() != null ? comment.getAuthor().getId() : null)
                    .authorName(comment.getAuthor() != null ? comment.getAuthor().getName() : User.WITHDRAWN_DISPLAY_NAME)
                    .isAnonymous(comment.isAnonymous())
                    .isDeleted(true)
                    .likeCount(likeCount)
                    .isLikedByMe(isLikedByMe)
                    .createdAt(comment.getCreatedAt())
                    .build();
        }

        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .content(comment.getContent())
                .authorId(comment.getAuthor() != null ? comment.getAuthor().getId() : null)
                .authorName(comment.getAuthor() != null ? comment.getAuthor().getName() : User.WITHDRAWN_DISPLAY_NAME)
                .isAnonymous(comment.isAnonymous())
                .isDeleted(false)
                .likeCount(likeCount)
                .isLikedByMe(isLikedByMe)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
