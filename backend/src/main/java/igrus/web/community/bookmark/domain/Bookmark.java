package igrus.web.community.bookmark.domain;

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
 * 북마크 엔티티.
 * 사용자가 게시글에 남긴 북마크 정보를 관리합니다.
 * 게시글당 사용자 1인 1회 북마크만 가능합니다.
 */
@Entity
@Table(
    name = "bookmarks",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_bookmarks_post_user",
        columnNames = {"bookmarks_post_id", "bookmarks_user_id"}
    )
)
@AttributeOverrides({
    @AttributeOverride(name = "createdAt", column = @Column(name = "bookmarks_created_at", nullable = false, updatable = false)),
    @AttributeOverride(name = "updatedAt", column = @Column(name = "bookmarks_updated_at", nullable = false)),
    @AttributeOverride(name = "createdBy", column = @Column(name = "bookmarks_created_by", updatable = false)),
    @AttributeOverride(name = "updatedBy", column = @Column(name = "bookmarks_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmarks_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookmarks_post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookmarks_user_id", nullable = false)
    private User user;

    private Bookmark(Post post, User user) {
        this.post = post;
        this.user = user;
    }

    /**
     * 북마크를 생성합니다.
     *
     * @param post 북마크할 게시글
     * @param user 북마크하는 사용자
     * @return 생성된 북마크 엔티티
     */
    public static Bookmark create(Post post, User user) {
        return new Bookmark(post, user);
    }
}
