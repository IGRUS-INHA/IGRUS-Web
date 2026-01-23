package igrus.web.user.domain;

import igrus.web.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 사용자 정지 이력
 * <p>
 * PRD의 UserSuspension 테이블에 대응합니다.
 * 정지 시작일, 종료일, 사유, 해제 정보를 관리합니다.
 */
@Entity
@Table(name = "user_suspensions", indexes = {
        @Index(name = "idx_user_suspensions_user_id", columnList = "user_suspensions_user_id"),
        @Index(name = "idx_user_suspensions_suspended_at", columnList = "user_suspensions_suspended_at"),
        @Index(name = "idx_user_suspensions_suspended_until", columnList = "user_suspensions_suspended_until"),
        @Index(name = "idx_user_suspensions_lifted_at", columnList = "user_suspensions_lifted_at")
})
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "user_suspensions_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "user_suspensions_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "user_suspensions_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "user_suspensions_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSuspension extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_suspensions_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_suspensions_user_id", nullable = false)
    private User user;

    // 정지 사유
    @Column(name = "user_suspensions_reason", nullable = false)
    private String reason;

    // 정지 시작일
    @Column(name = "user_suspensions_suspended_at", nullable = false)
    private Instant suspendedAt;

    // 정지 종료일
    @Column(name = "user_suspensions_suspended_until", nullable = false)
    private Instant suspendedUntil;

    // 정지 처리자 ID
    @Column(name = "user_suspensions_suspended_by", nullable = false)
    private Long suspendedBy;

    // 해제일 (nullable)
    @Column(name = "user_suspensions_lifted_at")
    private Instant liftedAt;

    // 해제 처리자 ID (nullable)
    @Column(name = "user_suspensions_lifted_by")
    private Long liftedBy;

    private UserSuspension(User user, String reason, Instant suspendedAt,
                           Instant suspendedUntil, Long suspendedBy) {
        this.user = user;
        this.reason = reason;
        this.suspendedAt = suspendedAt;
        this.suspendedUntil = suspendedUntil;
        this.suspendedBy = suspendedBy;
    }

    // === 정적 팩토리 메서드 ===

    /**
     * 사용자 정지 이력 생성
     *
     * @param user           정지 대상 사용자
     * @param reason         정지 사유
     * @param suspendedUntil 정지 종료일
     * @param suspendedBy    정지 처리자 ID
     * @return UserSuspension
     */
    public static UserSuspension create(User user, String reason,
                                        Instant suspendedUntil, Long suspendedBy) {
        return new UserSuspension(user, reason, Instant.now(), suspendedUntil, suspendedBy);
    }

    /**
     * 사용자 정지 이력 생성 (정지 시작일 지정)
     *
     * @param user           정지 대상 사용자
     * @param reason         정지 사유
     * @param suspendedAt    정지 시작일
     * @param suspendedUntil 정지 종료일
     * @param suspendedBy    정지 처리자 ID
     * @return UserSuspension
     */
    public static UserSuspension create(User user, String reason,
                                        Instant suspendedAt, Instant suspendedUntil,
                                        Long suspendedBy) {
        validateSuspensionPeriod(suspendedAt, suspendedUntil);
        return new UserSuspension(user, reason, suspendedAt, suspendedUntil, suspendedBy);
    }

    private static void validateSuspensionPeriod(Instant suspendedAt, Instant suspendedUntil) {
        if (suspendedUntil.isBefore(suspendedAt)) {
            throw new IllegalArgumentException("정지 종료일은 정지 시작일 이후여야 합니다");
        }
    }

    // === 해제 관련 ===

    /**
     * 정지 해제
     *
     * @param liftedBy 해제 처리자 ID
     */
    public void lift(Long liftedBy) {
        if (isLifted()) {
            throw new IllegalStateException("이미 해제된 정지입니다");
        }
        this.liftedAt = Instant.now();
        this.liftedBy = liftedBy;
    }

    /**
     * 정지 해제 (해제일 지정)
     *
     * @param liftedAt 해제일
     * @param liftedBy 해제 처리자 ID
     */
    public void lift(Instant liftedAt, Long liftedBy) {
        if (isLifted()) {
            throw new IllegalStateException("이미 해제된 정지입니다");
        }
        this.liftedAt = liftedAt;
        this.liftedBy = liftedBy;
    }

    // === 상태 확인 ===

    /**
     * 정지 해제 여부 확인
     */
    public boolean isLifted() {
        return this.liftedAt != null;
    }

    /**
     * 현재 유효한 정지인지 확인 (해제되지 않았고, 정지 기간 내인 경우)
     */
    public boolean isActive() {
        if (isLifted()) {
            return false;
        }
        Instant now = Instant.now();
        return !now.isBefore(suspendedAt) && now.isBefore(suspendedUntil);
    }

    /**
     * 정지 기간이 만료되었는지 확인 (정지 종료일이 현재 시각 이전인 경우)
     */
    public boolean isExpired() {
        return Instant.now().isAfter(suspendedUntil);
    }

    /**
     * 정지 기간이 시작되었는지 확인
     */
    public boolean hasStarted() {
        return !Instant.now().isBefore(suspendedAt);
    }

    // === 사유 관리 ===

    /**
     * 정지 사유 업데이트
     */
    public void updateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("정지 사유는 필수입니다");
        }
        this.reason = reason;
    }

    /**
     * 정지 기간 연장
     *
     * @param newSuspendedUntil 새로운 정지 종료일
     */
    public void extendSuspension(Instant newSuspendedUntil) {
        if (isLifted()) {
            throw new IllegalStateException("해제된 정지는 연장할 수 없습니다");
        }
        if (newSuspendedUntil.isBefore(this.suspendedUntil)) {
            throw new IllegalArgumentException("새로운 종료일은 기존 종료일 이후여야 합니다");
        }
        this.suspendedUntil = newSuspendedUntil;
    }
}