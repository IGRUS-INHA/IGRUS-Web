package igrus.web.inquiry.domain;

import igrus.web.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * 문의 상태 변경 감사 이력.
 * 관리자가 수행한 문의 상태 변경(수동 변경, 답변 완료)을 기록합니다.
 *
 * <p>FK 없음 — soft-delete 및 AFTER_COMMIT 리스너와의 호환성을 위해
 * inquiryId, changedByUserId를 plain BIGINT로 저장합니다.</p>
 */
@Entity
@Table(name = "inquiry_status_change_histories", indexes = {
        @Index(name = "idx_isch_inquiry_id", columnList = "inquiry_status_change_histories_inquiry_id"),
        @Index(name = "idx_isch_changed_by_id", columnList = "inquiry_status_change_histories_changed_by_id"),
        @Index(name = "idx_isch_change_type", columnList = "inquiry_status_change_histories_change_type"),
        @Index(name = "idx_isch_created_at", columnList = "inquiry_status_change_histories_created_at")
})
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "inquiry_status_change_histories_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "inquiry_status_change_histories_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "inquiry_status_change_histories_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "inquiry_status_change_histories_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryStatusChangeHistory extends BaseEntity {

    /** 기본 키 (AUTO_INCREMENT). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_status_change_histories_id")
    private Long id;

    /** 상태가 변경된 문의의 ID. FK 제약 없이 plain BIGINT로 저장. */
    @Column(name = "inquiry_status_change_histories_inquiry_id")
    private Long inquiryId;

    /** 상태 변경을 수행한 관리자의 ID. FK 제약 없이 plain BIGINT로 저장. */
    @Column(name = "inquiry_status_change_histories_changed_by_id")
    private Long changedByUserId;

    /** 상태 변경을 수행한 관리자의 학번. 감사 추적용 비정규화 필드. */
    @Column(name = "inquiry_status_change_histories_changed_by_student_id", length = 20)
    private String changedByStudentId;

    /** 상태 변경 유형 (수동 변경, 답변 완료). */
    @Enumerated(EnumType.STRING)
    @Column(name = "inquiry_status_change_histories_change_type", nullable = false, length = 50)
    private InquiryChangeType changeType;

    /** 변경 전 상태 값. */
    @Column(name = "inquiry_status_change_histories_previous_value", nullable = false)
    private String previousValue;

    /** 변경 후 상태 값. */
    @Column(name = "inquiry_status_change_histories_new_value", nullable = false)
    private String newValue;

    private InquiryStatusChangeHistory(Long inquiryId, Long changedByUserId, String changedByStudentId,
                                       InquiryChangeType changeType,
                                       String previousValue, String newValue) {
        this.inquiryId = inquiryId;
        this.changedByUserId = changedByUserId;
        this.changedByStudentId = changedByStudentId;
        this.changeType = changeType;
        this.previousValue = previousValue;
        this.newValue = newValue;
    }

    public static InquiryStatusChangeHistory create(Long inquiryId, Long changedByUserId, String changedByStudentId,
                                                    InquiryChangeType changeType,
                                                    String previousValue, String newValue) {
        Objects.requireNonNull(changeType, "changeType must not be null");
        Objects.requireNonNull(previousValue, "previousValue must not be null");
        Objects.requireNonNull(newValue, "newValue must not be null");
        return new InquiryStatusChangeHistory(inquiryId, changedByUserId, changedByStudentId,
                changeType, previousValue, newValue);
    }
}
