package igrus.web.community.comment.domain;

import igrus.web.common.domain.SoftDeletableEntity;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 댓글 엔티티.
 * 게시글에 대한 댓글 정보를 관리합니다.
 * 대댓글은 1단계까지만 허용됩니다.
 */
@Entity
@Table(name = "comments")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "comments_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "comments_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "comments_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "comments_updated_by")),
        @AttributeOverride(name = "deleted", column = @Column(name = "comments_deleted", nullable = false)),
        @AttributeOverride(name = "deletedAt", column = @Column(name = "comments_deleted_at")),
        @AttributeOverride(name = "deletedBy", column = @Column(name = "comments_deleted_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends SoftDeletableEntity {

    /** 댓글 내용 최대 길이 */
    public static final int MAX_CONTENT_LENGTH = 500;

    /** 댓글 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comments_id")
    private Long id;

    /** 댓글이 속한 게시글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comments_post_id", nullable = false)
    private Post post;

    /** 부모 댓글 (대댓글인 경우). 일반 댓글이면 null */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comments_parent_comment_id")
    private Comment parentComment;

    /** 댓글 작성자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comments_author_id", nullable = false)
    private User author;

    /** 댓글 내용 (최대 500자) */
    @Column(name = "comments_content", nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    /** 익명 여부 */
    @Column(name = "comments_is_anonymous", nullable = false)
    private boolean isAnonymous = false;

    /** 대댓글 목록 */
    @OneToMany(mappedBy = "parentComment")
    private List<Comment> replies = new ArrayList<>();

    private Comment(Post post, Comment parentComment, User author, String content, boolean isAnonymous) {
        validateContent(content);
        this.post = post;
        this.parentComment = parentComment;
        this.author = author;
        this.content = content;
        this.isAnonymous = isAnonymous;
    }

    // === 정적 팩토리 메서드 ===

    /**
     * 일반 댓글을 생성합니다.
     *
     * @param post    게시글
     * @param author  작성자
     * @param content 내용 (최대 500자)
     * @param isAnonymous 익명 여부
     * @return 생성된 댓글
     * @throws IllegalArgumentException 내용이 비어있거나 500자를 초과하는 경우
     */
    public static Comment createComment(Post post, User author, String content, boolean isAnonymous) {
        return new Comment(post, null, author, content, isAnonymous);
    }

    /**
     * 대댓글을 생성합니다.
     *
     * @param post          게시글
     * @param parentComment 부모 댓글
     * @param author        작성자
     * @param content       내용 (최대 500자)
     * @param isAnonymous   익명 여부
     * @return 생성된 대댓글
     * @throws IllegalArgumentException 부모 댓글이 이미 대댓글이거나, 내용이 비어있거나 500자를 초과하는 경우
     */
    public static Comment createReply(Post post, Comment parentComment, User author, String content, boolean isAnonymous) {
        if (parentComment.isReply()) {
            throw new IllegalArgumentException("대댓글에는 답글을 달 수 없습니다");
        }
        return new Comment(post, parentComment, author, content, isAnonymous);
    }

    // === 비즈니스 메서드 ===

    /**
     * 이 댓글이 대댓글인지 확인합니다.
     *
     * @return 대댓글이면 true
     */
    public boolean isReply() {
        return this.parentComment != null;
    }

    /**
     * 이 댓글에 대댓글을 달 수 있는지 확인합니다.
     * 일반 댓글이고 삭제되지 않은 경우에만 가능합니다.
     *
     * @return 대댓글 가능 여부
     */
    public boolean canReplyTo() {
        return !isReply() && !isDeleted();
    }

    /**
     * 대댓글이 있는지 확인합니다.
     *
     * @return 대댓글이 있으면 true
     */
    public boolean hasReplies() {
        return !this.replies.isEmpty();
    }

    /**
     * 해당 사용자가 댓글을 삭제할 수 있는지 확인합니다.
     * 작성자 본인 또는 OPERATOR 이상 권한을 가진 사용자만 삭제 가능합니다.
     *
     * @param user 확인할 사용자
     * @return 삭제 가능 여부
     */
    public boolean canDelete(User user) {
        if (user == null) {
            return false;
        }
        return isAuthor(user) || user.isOperatorOrAbove();
    }

    /**
     * 해당 사용자가 작성자인지 확인합니다.
     *
     * @param user 확인할 사용자
     * @return 작성자이면 true
     */
    public boolean isAuthor(User user) {
        return this.author != null && this.author.getId().equals(user.getId());
    }

    // === Private 검증 메서드 ===

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("내용을 입력해 주세요");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("댓글은 " + MAX_CONTENT_LENGTH + "자 이내여야 합니다");
        }
    }
}
