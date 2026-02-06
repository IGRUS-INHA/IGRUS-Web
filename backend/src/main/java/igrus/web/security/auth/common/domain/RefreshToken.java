package igrus.web.security.auth.common.domain;

import igrus.web.common.domain.BaseEntity;
import igrus.web.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 리프레시 토큰 정보를 저장하는 엔티티.
 *
 * <p>JWT 기반 인증에서 액세스 토큰 갱신을 위한 리프레시 토큰을 관리합니다.
 * 토큰의 만료 시간과 폐기 여부를 추적하여 토큰의 유효성을 검증합니다.</p>
 *
 * <p>토큰 로테이션을 지원하며, 같은 로그인 세션에서 파생된 토큰들은
 * 동일한 {@code tokenFamily}로 그룹화되어 탈취 감지에 활용됩니다.</p>
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
     * 토큰 패밀리 식별자. 같은 로그인 세션에서 파생된 토큰 체인을 그룹화합니다.
     */
    @Column(name = "refresh_tokens_token_family", nullable = false, length = 36)
    private String tokenFamily;

    /**
     * 로테이션 시 이 토큰을 대체한 새 토큰 값.
     */
    @Column(name = "refresh_tokens_replaced_by_token", length = 2048)
    private String replacedByToken;

    /**
     * 토큰이 폐기된 시점. Grace Period 판단에 사용됩니다.
     */
    @Column(name = "refresh_tokens_revoked_at")
    private Instant revokedAt;

    /**
     * 로그인 시 새로운 토큰 패밀리와 함께 RefreshToken을 생성합니다.
     *
     * @param user 토큰을 발급받는 사용자
     * @param token 리프레시 토큰 문자열
     * @param expiryMillis 만료 시간 (밀리초)
     * @return 새 토큰 패밀리가 할당된 RefreshToken 엔티티
     */
    public static RefreshToken createInitial(User user, String token, long expiryMillis) {
        return create(user, token, expiryMillis, UUID.randomUUID().toString());
    }

    /**
     * 기존 토큰 패밀리를 이어받아 RefreshToken을 생성합니다. (로테이션용)
     *
     * @param user 토큰을 발급받는 사용자
     * @param token 리프레시 토큰 문자열
     * @param expiryMillis 만료 시간 (밀리초)
     * @param tokenFamily 토큰 패밀리 식별자
     * @return 생성된 RefreshToken 엔티티
     */
    public static RefreshToken create(User user, String token, long expiryMillis, String tokenFamily) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.user = user;
        refreshToken.token = token;
        refreshToken.expiresAt = Instant.now().plusMillis(expiryMillis);
        refreshToken.tokenFamily = tokenFamily;
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
        this.revokedAt = Instant.now();
    }

    /**
     * 토큰을 로테이션합니다. 현재 토큰을 폐기하고 대체 토큰 정보를 기록합니다.
     *
     * @param newToken 이 토큰을 대체하는 새 토큰 값
     */
    public void rotateWith(String newToken) {
        this.revoked = true;
        this.revokedAt = Instant.now();
        this.replacedByToken = newToken;
    }

    /**
     * 토큰이 폐기된 후 유예 기간 내에 있는지 확인합니다.
     * 동시 탭 등에서의 경쟁 조건을 처리하기 위해 사용됩니다.
     *
     * @param gracePeriod 유예 기간
     * @return 유예 기간 내 여부
     */
    public boolean isWithinGracePeriod(Duration gracePeriod) {
        if (this.revokedAt == null) {
            return false;
        }
        return Instant.now().isBefore(this.revokedAt.plus(gracePeriod));
    }
}
