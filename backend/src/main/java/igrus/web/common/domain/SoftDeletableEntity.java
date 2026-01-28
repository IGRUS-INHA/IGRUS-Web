package igrus.web.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.Instant;

/**
 * Soft Delete를 지원하는 엔티티의 기본 클래스.
 * 물리적 삭제 대신 deleted 플래그로 논리적 삭제를 수행합니다.
 */
@MappedSuperclass
@Getter
public abstract class SoftDeletableEntity extends BaseEntity {

    /** 삭제 여부 (true: 삭제됨, false: 활성) */
    @Column(nullable = false)
    private boolean deleted = false;

    /** 삭제 시각 (삭제되지 않은 경우 null) */
    private Instant deletedAt;

    /** 삭제 수행자 ID (삭제되지 않은 경우 null) */
    private Long deletedBy;

    public void delete(Long deletedBy) {
        this.deleted = true;
        this.deletedAt = Instant.now();
        this.deletedBy = deletedBy;
    }

    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
