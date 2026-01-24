package igrus.web.security.auth.password.domain;

import igrus.web.common.domain.BaseEntity;
import igrus.web.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "password_reset_tokens_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "password_reset_tokens_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "password_reset_tokens_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "password_reset_tokens_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "password_reset_tokens_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "password_reset_tokens_user_id", nullable = false)
    private User user;

    @Column(name = "password_reset_tokens_token", nullable = false, unique = true)
    private String token;

    @Column(name = "password_reset_tokens_expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "password_reset_tokens_used", nullable = false)
    private boolean used = false;

    /**
     * PasswordResetToken 엔티티를 생성합니다.
     *
     * @param user 비밀번호를 재설정하려는 사용자
     * @param token 비밀번호 재설정 토큰 문자열
     * @param expiryMillis 만료 시간 (밀리초)
     * @return 생성된 PasswordResetToken 엔티티
     */
    public static PasswordResetToken create(User user, String token, long expiryMillis) {
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.user = user;
        passwordResetToken.token = token;
        passwordResetToken.expiresAt = Instant.now().plusMillis(expiryMillis);
        return passwordResetToken;
    }

    /**
     * 토큰이 만료되었는지 확인합니다.
     *
     * @return 만료 여부
     */
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    /**
     * 토큰이 유효한지 확인합니다. (만료되지 않았고 사용되지 않음)
     *
     * @return 유효 여부
     */
    public boolean isValid() {
        return !isExpired() && !this.used;
    }

    /**
     * 토큰을 사용됨 상태로 변경합니다.
     */
    public void markAsUsed() {
        this.used = true;
    }
}
