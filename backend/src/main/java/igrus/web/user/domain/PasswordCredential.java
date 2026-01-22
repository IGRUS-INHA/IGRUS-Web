package igrus.web.user.domain;

import igrus.web.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 사용자 패스워드 기반 인증 정보 (비밀번호, 계정 상태, 승인 정보)
@Entity
@Table(name = "password_credentials")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "password_credentials_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "password_credentials_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "password_credentials_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "password_credentials_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordCredential extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "password_credentials_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "password_credentials_user_id", unique = true, nullable = false)
    private User user;

    // 비밀번호 해시 (BCrypt 암호화)
    @Column(name = "password_credentials_password_hash", nullable = false)
    private String passwordHash;

    // 계정 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "password_credentials_status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    // 정회원 승인일 : 준회원 -> 정회원 전환 시 기록
    @Column(name = "password_credentials_approved_at")
    private LocalDateTime approvedAt;

    // 승인 처리자의 ID
    @Column(name = "password_credentials_approved_by")
    private Long approvedBy;

    // === 정적 팩토리 메서드 ===

    public static PasswordCredential create(User user, String passwordHash) {
        PasswordCredential credential = new PasswordCredential();
        credential.user = user;
        credential.passwordHash = passwordHash;
        credential.status = UserStatus.ACTIVE;
        return credential;
    }

    // === 비밀번호 관련 ===

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    // === 계정 상태 관련 ===

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
    }

    // === 상태 확인 ===

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean isSuspended() {
        return this.status == UserStatus.SUSPENDED;
    }

    public boolean isWithdrawn() {
        return this.status == UserStatus.WITHDRAWN;
    }

    // === 정회원 승인 ===

    public void approve(Long approverId) {
        this.approvedAt = LocalDateTime.now();
        this.approvedBy = approverId;
    }

    public boolean isApproved() {
        return this.approvedAt != null;
    }

}