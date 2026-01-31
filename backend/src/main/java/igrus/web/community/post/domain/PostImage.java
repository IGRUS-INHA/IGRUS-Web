package igrus.web.community.post.domain;

import igrus.web.common.domain.BaseEntity;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 이미지 엔티티.
 * 게시글에 첨부된 이미지 정보를 관리합니다.
 */
@Entity
@Table(name = "post_images")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "post_images_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "post_images_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "post_images_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "post_images_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage extends BaseEntity {

    /** 게시글 이미지 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_images_id")
    private Long id;

    /** 이미지가 속한 게시글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_images_post_id", nullable = false)
    private Post post;

    /** 이미지 URL (최대 500자) */
    @Column(name = "post_images_image_url", nullable = false, length = 500)
    private String imageUrl;

    /** 이미지 표시 순서 (0부터 시작) */
    @Column(name = "post_images_display_order", nullable = false)
    private int displayOrder = 0;

    private PostImage(Post post, String imageUrl, int displayOrder) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }

    /**
     * PostImage 인스턴스를 생성합니다.
     *
     * @param post         게시글
     * @param imageUrl     이미지 URL
     * @param displayOrder 표시 순서
     * @return 생성된 PostImage 인스턴스
     */
    public static PostImage create(Post post, String imageUrl, int displayOrder) {
        return new PostImage(post, imageUrl, displayOrder);
    }

    /**
     * 이미지의 표시 순서를 변경합니다.
     *
     * @param order 새로운 표시 순서
     */
    public void updateDisplayOrder(int order) {
        this.displayOrder = order;
    }
}
