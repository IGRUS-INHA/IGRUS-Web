package igrus.web.user.withdrawal.domain;

import igrus.web.common.domain.BaseEntity;
import igrus.web.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "withdrawal_logs")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "withdrawal_logs_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "withdrawal_logs_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "withdrawal_logs_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "withdrawal_logs_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawalLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "withdrawal_logs_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "withdrawal_logs_user_id", nullable = false)
    private User user;

    @Column(name = "withdrawal_logs_reason", nullable = false, length = 500)
    private String reason;

    // === 정적 팩토리 메서드 ===

    public static WithdrawalLog create(User user, String reason) {
        WithdrawalLog log = new WithdrawalLog();
        log.user = user;
        log.reason = reason;
        return log;
    }
}
