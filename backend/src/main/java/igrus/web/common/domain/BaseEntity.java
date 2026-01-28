package igrus.web.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * 모든 엔티티의 기본 클래스.
 * 생성/수정 시각과 생성/수정자 정보를 자동으로 관리합니다.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    /** 엔티티 생성 시각 (자동 설정, 수정 불가) */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** 엔티티 마지막 수정 시각 (자동 갱신) */
    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    /** 엔티티 생성자 ID (자동 설정, 수정 불가) */
    @CreatedBy
    @Column(updatable = false)
    private Long createdBy;

    /** 엔티티 마지막 수정자 ID (자동 갱신) */
    @LastModifiedBy
    private Long updatedBy;
}
