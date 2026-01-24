package igrus.web.security.auth.common.domain;

import igrus.web.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 로그인 시도 정보를 저장하는 엔티티.
 *
 * <p>Brute Force 공격 방지를 위해 로그인 실패 횟수와 계정 잠금 상태를 관리합니다.
 * 일정 횟수 이상 로그인에 실패하면 계정이 임시로 잠기게 됩니다.</p>
 *
 * @see igrus.web.common.domain.BaseEntity
 */
@Entity
@Table(name = "login_attempts")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "login_attempts_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "login_attempts_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "login_attempts_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "login_attempts_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginAttempt extends BaseEntity {

    /**
     * 로그인 시도 고유 식별자.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_attempts_id")
    private Long id;

    /**
     * 로그인 시도 대상 학번.
     */
    @Column(name = "login_attempts_student_id", nullable = false, length = 8, unique = true)
    private String studentId;

    /**
     * 로그인 실패 시도 횟수. 기본값은 0.
     */
    @Column(name = "login_attempts_attempt_count", nullable = false)
    private int attemptCount = 0;

    /**
     * 마지막 로그인 시도 일시.
     */
    @Column(name = "login_attempts_last_attempt_at", nullable = false)
    private Instant lastAttemptAt;

    /**
     * 계정 잠금 해제 일시. null이면 잠금 상태가 아님.
     */
    @Column(name = "login_attempts_locked_until")
    private Instant lockedUntil;

    /**
     * LoginAttempt 엔티티를 생성합니다.
     *
     * @param studentId 학번
     * @return 생성된 LoginAttempt 엔티티
     */
    public static LoginAttempt create(String studentId) {
        LoginAttempt loginAttempt = new LoginAttempt();
        loginAttempt.studentId = studentId;
        loginAttempt.attemptCount = 0;
        loginAttempt.lastAttemptAt = Instant.now();
        loginAttempt.lockedUntil = null;
        return loginAttempt;
    }

    /**
     * 로그인 시도 횟수를 증가시킵니다.
     */
    public void incrementAttempt() {
        this.attemptCount++;
        this.lastAttemptAt = Instant.now();
    }

    /**
     * 계정을 지정된 시간 동안 잠금 처리합니다.
     *
     * @param lockoutMinutes 잠금 시간 (분)
     */
    public void lock(int lockoutMinutes) {
        this.lockedUntil = Instant.now().plusSeconds(lockoutMinutes * 60L);
    }

    /**
     * 로그인 시도 기록을 초기화합니다.
     * 로그인 성공 시 호출됩니다.
     */
    public void reset() {
        this.attemptCount = 0;
        this.lockedUntil = null;
        this.lastAttemptAt = Instant.now();
    }

    /**
     * 계정이 현재 잠금 상태인지 확인합니다.
     *
     * @return 잠금 상태이면 true
     */
    public boolean isLocked() {
        return this.lockedUntil != null && Instant.now().isBefore(this.lockedUntil);
    }
}
