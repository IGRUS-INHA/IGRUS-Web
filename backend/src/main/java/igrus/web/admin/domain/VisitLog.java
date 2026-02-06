package igrus.web.admin.domain;

import igrus.web.common.domain.BaseEntity;
import igrus.web.user.domain.User;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 방문 기록 엔티티.
 *
 * <p>로그인 성공 시 방문 기록을 저장합니다.
 * KST 기준 하루 1회만 기록하며, 대시보드 방문자 수 집계에 사용됩니다.</p>
 *
 * <p>user가 null인 경우 비회원 방문을 의미합니다. (향후 확장용)</p>
 */
@Entity
@Table(name = "visit_logs", indexes = {
        @Index(name = "idx_visit_logs_user_id", columnList = "visit_logs_user_id"),
        @Index(name = "idx_visit_logs_visited_at", columnList = "visit_logs_visited_at")
})
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "visit_logs_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "visit_logs_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "visit_logs_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "visit_logs_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visit_logs_id")
    private Long id;

    /** 방문한 사용자 (null이면 비회원) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_logs_user_id")
    private User user;

    /** 방문 시각 */
    @Column(name = "visit_logs_visited_at", nullable = false)
    private Instant visitedAt;

    private VisitLog(User user, Instant visitedAt) {
        this.user = user;
        this.visitedAt = visitedAt;
    }

    /**
     * 회원 방문 기록을 생성합니다.
     *
     * @param user 방문한 사용자
     * @return 방문 기록
     */
    public static VisitLog ofMember(User user) {
        return new VisitLog(user, Instant.now());
    }

    /**
     * 비회원 방문 기록을 생성합니다. (향후 확장용)
     *
     * @return 방문 기록
     */
    public static VisitLog ofGuest() {
        return new VisitLog(null, Instant.now());
    }
}
