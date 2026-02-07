package igrus.web.user.domain;

import igrus.web.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


// 계정 상태 변경 감사 이력
@Entity
@Table(name = "account_status_change_histories", indexes = {
        @Index(name = "idx_asch_user_id", columnList = "account_status_change_histories_user_id"),
        @Index(name = "idx_asch_changed_by_id", columnList = "account_status_change_histories_changed_by_id"),
        @Index(name = "idx_asch_change_type", columnList = "account_status_change_histories_change_type"),
        @Index(name = "idx_asch_created_at", columnList = "account_status_change_histories_created_at")
})
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "account_status_change_histories_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "account_status_change_histories_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "account_status_change_histories_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "account_status_change_histories_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountStatusChangeHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_status_change_histories_id")
    private Long id;

    @Column(name = "account_status_change_histories_user_id")
    private Long userId;

    @Column(name = "account_status_change_histories_user_student_id", length = 20)
    private String userStudentId;

    @Column(name = "account_status_change_histories_changed_by_id")
    private Long changedByUserId;

    @Column(name = "account_status_change_histories_changed_by_student_id", length = 20)
    private String changedByStudentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status_change_histories_change_type", nullable = false)
    private AccountChangeType changeType;

    @Column(name = "account_status_change_histories_previous_value", nullable = false)
    private String previousValue;

    @Column(name = "account_status_change_histories_new_value", nullable = false)
    private String newValue;

    @Column(name = "account_status_change_histories_reason")
    private String reason;

    private AccountStatusChangeHistory(Long userId, String userStudentId,
                                       Long changedByUserId, String changedByStudentId,
                                       AccountChangeType changeType,
                                       String previousValue, String newValue, String reason) {
        this.userId = userId;
        this.userStudentId = userStudentId;
        this.changedByUserId = changedByUserId;
        this.changedByStudentId = changedByStudentId;
        this.changeType = changeType;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.reason = reason;
    }

    public static AccountStatusChangeHistory create(Long userId, String userStudentId,
                                                    Long changedByUserId, String changedByStudentId,
                                                    AccountChangeType changeType,
                                                    String previousValue, String newValue, String reason) {
        return new AccountStatusChangeHistory(userId, userStudentId, changedByUserId, changedByStudentId,
                changeType, previousValue, newValue, reason);
    }
}
