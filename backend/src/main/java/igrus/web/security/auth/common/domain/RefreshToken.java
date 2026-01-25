package igrus.web.security.auth.common.domain;

import igrus.web.common.domain.BaseEntity;
import igrus.web.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 리프레시 토큰 정보를 저장하는 엔티티.
 *
 * <p>JWT 기반 인증에서 액세스 토큰 갱신을 위한 리프레시 토큰을 관리합니다.
 * 토큰의 만료 시간과 폐기 여부를 추적하여 토큰의 유효성을 검증합니다.</p>
 *
 * @see igrus.web.common.domain.BaseEntity
 * @see igrus.web.user.domain.User
 */
@Entity
@Table(name = "refresh_tokens")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "refresh_tokens_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "refresh_tokens_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "refresh_tokens_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "refresh_tokens_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    /**
     * 리프레시 토큰 고유 식별자.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_tokens_id")
    private Long id;

    /**
     * 토큰을 발급받은 사용자.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refresh_tokens_user_id", nullable = false)
    private User user;

    /**
     * 리프레시 토큰 문자열.
     */
    @Column(name = "refresh_tokens_token", nullable = false, unique = true, length = 2048)
    private String token;

    /**
     * 토큰 만료 일시.
     */
    @Column(name = "refresh_tokens_expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * 토큰 폐기 여부. 기본값은 false.
     */
    @Column(name = "refresh_tokens_revoked", nullable = false)
    private boolean revoked = false;

    /**
     * RefreshToken 엔티티를 생성합니다.
     *
     * @param user 토큰을 발급받는 사용자
     * @param token 리프레시 토큰 문자열
     * @param expiryMillis 만료 시간 (밀리초)
     * @return 생성된 RefreshToken 엔티티
     */
    public static RefreshToken create(User user, String token, long expiryMillis) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.user = user;
        refreshToken.token = token;
        refreshToken.expiresAt = Instant.now().plusMillis(expiryMillis);
        return refreshToken;
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
     * 토큰이 유효한지 확인합니다. (만료되지 않았고 폐기되지 않음)
     *
     * @return 유효 여부
     */
    public boolean isValid() {
        return !isExpired() && !this.revoked;
    }

    /**
     * 토큰을 폐기 상태로 변경합니다.
     */
    public void revoke() {
        this.revoked = true;
    }
}
