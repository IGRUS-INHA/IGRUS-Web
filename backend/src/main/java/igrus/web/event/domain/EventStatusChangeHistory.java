package igrus.web.event.domain;

import igrus.web.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * 행사 상태 변경 감사 이력.
 * 사용자가 직접 수행한 행사 상태 변경(취소, 재활성화, 등록 마감, 등록 재오픈)을 기록합니다.
 *
 * <p>FK 없음 — soft-delete 및 AFTER_COMMIT 리스너와의 호환성을 위해
 * eventId, changedByUserId를 plain BIGINT로 저장합니다.</p>
 */
@Entity
@Table(name = "event_status_change_histories", indexes = {
        @Index(name = "idx_esch_event_id", columnList = "event_status_change_histories_event_id"),
        @Index(name = "idx_esch_changed_by_id", columnList = "event_status_change_histories_changed_by_id"),
        @Index(name = "idx_esch_change_type", columnList = "event_status_change_histories_change_type"),
        @Index(name = "idx_esch_created_at", columnList = "event_status_change_histories_created_at")
})
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "event_status_change_histories_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "event_status_change_histories_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "event_status_change_histories_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "event_status_change_histories_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventStatusChangeHistory extends BaseEntity {

    /** 기본 키 (AUTO_INCREMENT). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_status_change_histories_id")
    private Long id;

    /** 상태가 변경된 행사의 ID. FK 제약 없이 plain BIGINT로 저장. */
    @Column(name = "event_status_change_histories_event_id")
    private Long eventId;

    /** 상태 변경을 수행한 사용자의 ID. FK 제약 없이 plain BIGINT로 저장. */
    @Column(name = "event_status_change_histories_changed_by_id")
    private Long changedByUserId;

    /** 상태 변경을 수행한 사용자의 학번. 감사 추적용 비정규화 필드. */
    @Column(name = "event_status_change_histories_changed_by_student_id", length = 20)
    private String changedByStudentId;

    /** 상태 변경 유형 (취소, 재활성화, 등록 마감, 등록 재오픈). */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_status_change_histories_change_type", nullable = false, length = 50)
    private EventChangeType changeType;

    /** 변경 전 상태 값. */
    @Column(name = "event_status_change_histories_previous_value", nullable = false)
    private String previousValue;

    /** 변경 후 상태 값. */
    @Column(name = "event_status_change_histories_new_value", nullable = false)
    private String newValue;

    /** 상태 변경 사유. 선택 입력. */
    @Column(name = "event_status_change_histories_reason", columnDefinition = "TEXT")
    private String reason;

    private EventStatusChangeHistory(Long eventId, Long changedByUserId, String changedByStudentId,
                                     EventChangeType changeType,
                                     String previousValue, String newValue, String reason) {
        this.eventId = eventId;
        this.changedByUserId = changedByUserId;
        this.changedByStudentId = changedByStudentId;
        this.changeType = changeType;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.reason = reason;
    }

    public static EventStatusChangeHistory create(Long eventId, Long changedByUserId, String changedByStudentId,
                                                  EventChangeType changeType,
                                                  String previousValue, String newValue, String reason) {
        Objects.requireNonNull(changeType, "changeType must not be null");
        Objects.requireNonNull(previousValue, "previousValue must not be null");
        Objects.requireNonNull(newValue, "newValue must not be null");
        return new EventStatusChangeHistory(eventId, changedByUserId, changedByStudentId,
                changeType, previousValue, newValue, reason);
    }
}
