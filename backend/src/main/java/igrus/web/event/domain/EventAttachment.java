package igrus.web.event.domain;

import igrus.web.storage.domain.FileMetadata;
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
 * 행사 첨부파일 엔티티.
 * 행사(Event)와 파일(FileMetadata) 간의 다대다 관계를 중간 테이블로 관리한다.
 */
@Entity
@Table(name = "event_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_attachment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_metadata_id", nullable = false)
    private FileMetadata fileMetadata;

    @Column(name = "is_thumbnail", nullable = false)
    private boolean thumbnail;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    private EventAttachment(Event event, FileMetadata fileMetadata, boolean thumbnail, int displayOrder) {
        this.event = event;
        this.fileMetadata = fileMetadata;
        this.thumbnail = thumbnail;
        this.displayOrder = displayOrder;
    }

    /**
     * EventAttachment 인스턴스를 생성한다.
     *
     * @param event        행사
     * @param fileMetadata 파일 메타데이터
     * @param thumbnail    썸네일 여부
     * @param displayOrder 표시 순서 (0부터 시작)
     * @return EventAttachment 인스턴스
     */
    public static EventAttachment create(Event event, FileMetadata fileMetadata, boolean thumbnail, int displayOrder) {
        return new EventAttachment(event, fileMetadata, thumbnail, displayOrder);
    }

    /**
     * 썸네일 여부를 변경한다.
     *
     * @param thumbnail 썸네일 여부
     */
    public void changeThumbnail(boolean thumbnail) {
        this.thumbnail = thumbnail;
    }

    /**
     * 표시 순서를 변경한다.
     *
     * @param displayOrder 새 표시 순서
     */
    public void changeDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
