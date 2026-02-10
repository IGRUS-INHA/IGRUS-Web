package igrus.web.community.pinnedpost.domain;

import igrus.web.common.domain.SoftDeletableEntity;
import igrus.web.community.pinnedpost.exception.InvalidDisplayOrderException;
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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고정 게시글 엔티티.
 * 메인 페이지에 고정할 게시글 정보를 관리합니다.
 */
@Entity
@Table(name = "pinned_posts")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "pinned_posts_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "pinned_posts_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "pinned_posts_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "pinned_posts_updated_by")),
        @AttributeOverride(name = "deleted", column = @Column(name = "pinned_posts_deleted", nullable = false)),
        @AttributeOverride(name = "deletedAt", column = @Column(name = "pinned_posts_deleted_at")),
        @AttributeOverride(name = "deletedBy", column = @Column(name = "pinned_posts_deleted_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PinnedPost extends SoftDeletableEntity {

    /** 고정 게시글 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pinned_posts_id")
    private Long id;

    /** 고정된 게시글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinned_posts_post_id", nullable = false)
    private Post post;

    /** 표시 순서 (1부터 시작) */
    @Column(name = "pinned_posts_display_order", nullable = false)
    private Integer displayOrder;

    /** 고정한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinned_posts_pinned_by", nullable = false)
    private User pinnedBy;

    /** 낙관적 락을 위한 버전 (동시성 제어) */
    @Version
    @Column(name = "pinned_posts_version")
    private Long version;

    // === 정적 팩토리 메서드 ===

    /**
     * 고정 게시글을 생성합니다.
     *
     * @param post         고정할 게시글
     * @param pinnedBy     고정한 사용자
     * @param displayOrder 표시 순서 (1 이상)
     * @return 생성된 고정 게시글
     * @throws InvalidDisplayOrderException 표시 순서가 1 미만인 경우
     */
    public static PinnedPost create(Post post, User pinnedBy, Integer displayOrder) {
        validateDisplayOrder(displayOrder);
        PinnedPost pinnedPost = new PinnedPost();
        pinnedPost.post = post;
        pinnedPost.pinnedBy = pinnedBy;
        pinnedPost.displayOrder = displayOrder;
        return pinnedPost;
    }

    // === 비즈니스 메서드 ===

    /**
     * 표시 순서를 변경합니다.
     *
     * @param newOrder 새 표시 순서 (1 이상)
     * @throws InvalidDisplayOrderException 표시 순서가 1 미만인 경우
     */
    public void updateDisplayOrder(Integer newOrder) {
        validateDisplayOrder(newOrder);
        this.displayOrder = newOrder;
    }

    // === Private 검증 메서드 ===

    private static void validateDisplayOrder(Integer order) {
        if (order == null || order < 1) {
            throw new InvalidDisplayOrderException(order);
        }
    }
}
