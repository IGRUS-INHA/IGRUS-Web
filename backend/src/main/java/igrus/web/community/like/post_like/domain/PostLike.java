package igrus.web.community.like.post_like.domain;

import igrus.web.common.domain.BaseEntity;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 좋아요 엔티티.
 * 사용자가 게시글에 남긴 좋아요 정보를 관리합니다.
 * 게시글당 사용자 1인 1회 좋아요만 가능합니다.
 */
@Entity
@Table(
    name = "likes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_likes_post_user",
        columnNames = {"likes_post_id", "likes_user_id"}
    )
)
@AttributeOverrides({
    @AttributeOverride(name = "createdAt", column = @Column(name = "likes_created_at", nullable = false, updatable = false)),
    @AttributeOverride(name = "updatedAt", column = @Column(name = "likes_updated_at", nullable = false)),
    @AttributeOverride(name = "createdBy", column = @Column(name = "likes_created_by", updatable = false)),
    @AttributeOverride(name = "updatedBy", column = @Column(name = "likes_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "likes_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "likes_post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "likes_user_id", nullable = false)
    private User user;

    private PostLike(Post post, User user) {
        this.post = post;
        this.user = user;
    }

    /**
     * 게시글 좋아요를 생성합니다.
     *
     * @param post 좋아요할 게시글
     * @param user 좋아요하는 사용자
     * @return 생성된 게시글 좋아요 엔티티
     */
    public static PostLike create(Post post, User user) {
        return new PostLike(post, user);
    }
}
