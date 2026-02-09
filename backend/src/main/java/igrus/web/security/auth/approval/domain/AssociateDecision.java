package igrus.web.security.auth.approval.domain;

import igrus.web.common.domain.BaseEntity;
import igrus.web.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "associate_decisions")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "associate_decisions_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "associate_decisions_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "associate_decisions_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "associate_decisions_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssociateDecision extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "associate_decisions_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "associate_decisions_user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "associate_decisions_type", nullable = false)
    private AssociateDecisionType type;

    @Column(name = "associate_decisions_reason")
    private String reason;

    @Column(name = "associate_decisions_decided_by", nullable = false)
    private Long decidedBy;

    @Column(name = "associate_decisions_decided_at", nullable = false)
    private Instant decidedAt;

    @Column(name = "associate_decisions_active", nullable = false)
    private boolean active;

    @Version
    @Column(name = "associate_decisions_version")
    private Long version;

    public static AssociateDecision approve(User user, Long decidedBy) {
        AssociateDecision decision = new AssociateDecision();
        decision.user = user;
        decision.type = AssociateDecisionType.APPROVED;
        decision.decidedBy = decidedBy;
        decision.decidedAt = Instant.now();
        decision.active = true;
        return decision;
    }

    public static AssociateDecision reject(User user, Long decidedBy, String reason) {
        AssociateDecision decision = new AssociateDecision();
        decision.user = user;
        decision.type = AssociateDecisionType.REJECTED;
        decision.reason = reason;
        decision.decidedBy = decidedBy;
        decision.decidedAt = Instant.now();
        decision.active = true;
        return decision;
    }

    public static AssociateDecision demote(User user, Long decidedBy, String reason) {
        AssociateDecision decision = new AssociateDecision();
        decision.user = user;
        decision.type = AssociateDecisionType.DEMOTED;
        decision.reason = reason;
        decision.decidedBy = decidedBy;
        decision.decidedAt = Instant.now();
        decision.active = true;
        return decision;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isApproved() {
        return this.type == AssociateDecisionType.APPROVED;
    }

    public boolean isRejected() {
        return this.type == AssociateDecisionType.REJECTED;
    }

    public boolean isDemoted() {
        return this.type == AssociateDecisionType.DEMOTED;
    }
}
