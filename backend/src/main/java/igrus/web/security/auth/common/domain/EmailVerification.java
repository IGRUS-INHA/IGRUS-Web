package igrus.web.security.auth.common.domain;

import igrus.web.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * 이메일 인증 정보를 저장하는 엔티티.
 *
 * <p>회원가입 시 이메일 인증 과정에서 발급된 인증 코드와 관련 정보를 관리합니다.
 * 인증 코드의 만료 시간, 인증 시도 횟수, 인증 완료 여부 등을 추적합니다.</p>
 *
 * @see igrus.web.common.domain.BaseEntity
 */
@Entity
@Table(name = "email_verifications")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "email_verifications_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "email_verifications_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "email_verifications_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "email_verifications_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseEntity {

    /**
     * 이메일 인증 고유 식별자.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_verifications_id")
    private Long id;

    /**
     * 인증 대상 이메일 주소.
     */
    @Column(name = "email_verifications_email", nullable = false)
    private String email;

    /**
     * 6자리 인증 코드.
     */
    @Column(name = "email_verifications_code", nullable = false, length = 6)
    private String code;

    /**
     * 인증 시도 횟수. 기본값은 0.
     */
    @Column(name = "email_verifications_attempts", nullable = false)
    private int attempts = 0;

    /**
     * 인증 완료 여부. 기본값은 false.
     */
    @Column(name = "email_verifications_verified", nullable = false)
    private boolean verified = false;

    /**
     * 인증 코드 만료 일시.
     */
    @Column(name = "email_verifications_expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * 인증 완료 시 발급되는 일회용 토큰. 회원가입 요청 시 소유권 검증에 사용.
     */
    @Column(name = "email_verifications_verification_token", length = 36)
    private String verificationToken;

    /**
     * 인증 요청 사용자 ID. 회원가입 시에는 null.
     */
    @Column(name = "email_verifications_user_id")
    private Long userId;

    /**
     * EmailVerification 엔티티를 생성합니다. (회원가입용)
     *
     * @param email 인증할 이메일 주소
     * @param code 6자리 인증 코드
     * @param expiryMillis 만료 시간 (밀리초)
     * @return 생성된 EmailVerification 엔티티
     */
    public static EmailVerification create(String email, String code, long expiryMillis) {
        EmailVerification emailVerification = new EmailVerification();
        emailVerification.email = email;
        emailVerification.code = code;
        emailVerification.expiresAt = Instant.now().plusMillis(expiryMillis);
        return emailVerification;
    }

    /**
     * EmailVerification 엔티티를 생성합니다. (이메일 변경용)
     *
     * @param email 인증할 이메일 주소
     * @param code 6자리 인증 코드
     * @param expiryMillis 만료 시간 (밀리초)
     * @param userId 인증 요청 사용자 ID
     * @return 생성된 EmailVerification 엔티티
     */
    public static EmailVerification create(String email, String code, long expiryMillis, Long userId) {
        EmailVerification emailVerification = create(email, code, expiryMillis);
        emailVerification.userId = userId;
        return emailVerification;
    }

    /**
     * 인증 코드가 만료되었는지 확인합니다.
     *
     * @return 만료 여부
     */
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    /**
     * 인증 시도 횟수를 증가시킵니다.
     */
    public void incrementAttempts() {
        this.attempts++;
    }

    /**
     * 추가 인증 시도가 가능한지 확인합니다.
     *
     * @param maxAttempts 최대 시도 횟수
     * @return 시도 가능 여부
     */
    public boolean canAttempt(int maxAttempts) {
        return this.attempts < maxAttempts;
    }

    /**
     * 이메일 인증을 완료 상태로 변경하고 소유권 검증용 토큰을 발급합니다.
     *
     * @return 발급된 인증 토큰 (회원가입 시 제출 필요)
     */
    public String verify() {
        this.verified = true;
        this.verificationToken = UUID.randomUUID().toString();
        return this.verificationToken;
    }
}
