package igrus.web.community.like.comment_like.domain;

import igrus.web.common.domain.BaseEntity;
import igrus.web.community.comment.domain.Comment;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글 좋아요 엔티티.
 * 사용자가 댓글에 좋아요를 누른 기록을 관리합니다.
 */
@Entity
@Table(name = "comment_likes", uniqueConstraints = {
    @UniqueConstraint(name = "uk_comment_likes_comment_user", columnNames = {"comment_likes_comment_id", "comment_likes_user_id"})
})
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "comment_likes_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "comment_likes_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "comment_likes_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "comment_likes_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike extends BaseEntity {

    /** 좋아요 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_likes_id")
    private Long id;

    /** 좋아요한 댓글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_likes_comment_id", nullable = false)
    private Comment comment;

    /** 좋아요한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_likes_user_id", nullable = false)
    private User user;

    private CommentLike(Comment comment, User user) {
        this.comment = comment;
        this.user = user;
    }

    /**
     * 댓글 좋아요를 생성합니다.
     *
     * @param comment 좋아요할 댓글
     * @param user    좋아요하는 사용자
     * @return 생성된 댓글 좋아요
     */
    public static CommentLike create(Comment comment, User user) {
        return new CommentLike(comment, user);
    }
}
