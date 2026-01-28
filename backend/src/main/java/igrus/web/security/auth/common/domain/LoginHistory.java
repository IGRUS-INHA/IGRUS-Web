package igrus.web.security.auth.common.domain;

import igrus.web.common.domain.BaseEntity;
import igrus.web.user.domain.User;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 로그인 히스토리 엔티티.
 *
 * <p>로그인 시도(성공/실패)에 대한 이력을 저장합니다.
 * 보안 감사, 분석, 이상 탐지 목적으로 사용됩니다.</p>
 */
@Entity
@Table(name = "login_histories", indexes = {
        @Index(name = "idx_login_histories_user_id", columnList = "login_histories_user_id"),
        @Index(name = "idx_login_histories_student_id", columnList = "login_histories_student_id"),
        @Index(name = "idx_login_histories_success", columnList = "login_histories_success"),
        @Index(name = "idx_login_histories_attempted_at", columnList = "login_histories_attempted_at"),
        @Index(name = "idx_login_histories_ip_address", columnList = "login_histories_ip_address")
})
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "login_histories_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "login_histories_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "login_histories_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "login_histories_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginHistory extends BaseEntity {

    private static final int MAX_USER_AGENT_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_histories_id")
    private Long id;

    /** 로그인한 사용자 (성공 시에만 설정, 실패 시 null 가능) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "login_histories_user_id")
    private User user;

    /** 시도한 학번 */
    @Column(name = "login_histories_student_id", nullable = false, length = 8)
    private String studentId;

    /** 클라이언트 IP 주소 */
    @Column(name = "login_histories_ip_address", nullable = false, length = 45)
    private String ipAddress;

    /** 클라이언트 User-Agent */
    @Column(name = "login_histories_user_agent", length = 500)
    private String userAgent;

    /** 로그인 성공 여부 */
    @Column(name = "login_histories_success", nullable = false)
    private boolean success;

    /** 실패 사유 (성공 시 null) */
    @Enumerated(EnumType.STRING)
    @Column(name = "login_histories_failure_reason", length = 50)
    private LoginFailureReason failureReason;

    /** 로그인 시도 시각 */
    @Column(name = "login_histories_attempted_at", nullable = false)
    private Instant attemptedAt;

    private LoginHistory(User user, String studentId, String ipAddress, String userAgent,
                         boolean success, LoginFailureReason failureReason) {
        this.user = user;
        this.studentId = studentId;
        this.ipAddress = ipAddress;
        this.userAgent = truncateUserAgent(userAgent);
        this.success = success;
        this.failureReason = failureReason;
        this.attemptedAt = Instant.now();
    }

    /**
     * 로그인 성공 히스토리를 생성합니다.
     *
     * @param user 로그인한 사용자
     * @param studentId 학번
     * @param ipAddress 클라이언트 IP 주소
     * @param userAgent 클라이언트 User-Agent
     * @return 로그인 성공 히스토리
     */
    public static LoginHistory success(User user, String studentId, String ipAddress, String userAgent) {
        return new LoginHistory(user, studentId, ipAddress, userAgent, true, null);
    }

    /**
     * 로그인 실패 히스토리를 생성합니다 (사용자 정보 없이).
     *
     * @param studentId 시도한 학번
     * @param ipAddress 클라이언트 IP 주소
     * @param userAgent 클라이언트 User-Agent
     * @param failureReason 실패 사유
     * @return 로그인 실패 히스토리
     */
    public static LoginHistory failure(String studentId, String ipAddress, String userAgent,
                                       LoginFailureReason failureReason) {
        return new LoginHistory(null, studentId, ipAddress, userAgent, false, failureReason);
    }

    /**
     * 로그인 실패 히스토리를 생성합니다 (사용자 정보 포함).
     *
     * @param user 로그인 시도한 사용자
     * @param studentId 시도한 학번
     * @param ipAddress 클라이언트 IP 주소
     * @param userAgent 클라이언트 User-Agent
     * @param failureReason 실패 사유
     * @return 로그인 실패 히스토리
     */
    public static LoginHistory failure(User user, String studentId, String ipAddress, String userAgent,
                                       LoginFailureReason failureReason) {
        return new LoginHistory(user, studentId, ipAddress, userAgent, false, failureReason);
    }

    private static String truncateUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > MAX_USER_AGENT_LENGTH
                ? userAgent.substring(0, MAX_USER_AGENT_LENGTH)
                : userAgent;
    }
}
