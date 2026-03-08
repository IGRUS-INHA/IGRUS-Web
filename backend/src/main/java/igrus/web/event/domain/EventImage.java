package igrus.web.event.domain;

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
 * 행사 이미지 엔티티.
 * 행사에 첨부된 이미지 정보를 관리합니다.
 */
@Entity
@Table(name = "event_images")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "event_images_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "event_images_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "event_images_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "event_images_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventImage extends BaseEntity {

    /** 행사 이미지 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_images_id")
    private Long id;

    /** 이미지가 속한 행사 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_images_event_id", nullable = false)
    private Event event;

    /** 이미지 URL (최대 500자) */
    @Column(name = "event_images_image_url", nullable = false, length = 500)
    private String imageUrl;

    /** 이미지 표시 순서 (0부터 시작) */
    @Column(name = "event_images_display_order", nullable = false)
    private int displayOrder = 0;

    private EventImage(Event event, String imageUrl, int displayOrder) {
        this.event = event;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }

    /**
     * EventImage 인스턴스를 생성합니다.
     *
     * @param event        행사
     * @param imageUrl     이미지 URL
     * @param displayOrder 표시 순서
     * @return 생성된 EventImage 인스턴스
     */
    public static EventImage create(Event event, String imageUrl, int displayOrder) {
        return new EventImage(event, imageUrl, displayOrder);
    }
}
