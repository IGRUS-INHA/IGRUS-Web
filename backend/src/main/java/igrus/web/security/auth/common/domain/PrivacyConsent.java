package igrus.web.security.auth.common.domain;

import igrus.web.common.domain.BaseEntity;
import igrus.web.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 개인정보 처리방침 동의 엔티티.
 *
 * <p>사용자가 서비스 이용을 위해 개인정보 처리방침에 동의한 기록을 저장합니다.
 * 각 사용자는 여러 개의 동의 기록을 가질 수 있으며(N:1 관계), 약관 버전이 변경될 때마다
 * 새로운 동의 기록이 생성됩니다.</p>
 *
 * <h3>주요 용도</h3>
 * <ul>
 *   <li>개인정보 처리방침 동의 여부 확인</li>
 *   <li>동의 시점 및 동의한 정책 버전 추적</li>
 *   <li>정책 버전 변경 시 재동의 필요 여부 판단</li>
 *   <li>동의 이력 관리 (법적 요구사항 대비)</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // 신규 사용자 동의 생성
 * PrivacyConsent consent = PrivacyConsent.create(user, "v1.0");
 *
 * // 약관 버전 업데이트 시 새로운 동의 생성
 * PrivacyConsent newConsent = PrivacyConsent.create(user, "v2.0");
 *
 * // 동의 여부 확인
 * if (consent.isConsentGiven()) {
 *     // 서비스 이용 허용
 * }
 * }</pre>
 *
 * @see User
 * @see BaseEntity
 */
@Entity
@Table(name = "privacy_consents")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "privacy_consents_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "privacy_consents_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "privacy_consents_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "privacy_consents_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrivacyConsent extends BaseEntity {

    /**
     * 개인정보 동의 고유 식별자 (PK).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "privacy_consents_id")
    private Long id;

    /**
     * 동의한 사용자.
     *
     * <p>한 사용자는 여러 개의 동의 기록을 가질 수 있습니다 (N:1 관계).
     * 약관 버전이 변경될 때마다 새로운 동의 기록이 생성됩니다.
     * 지연 로딩(LAZY)을 사용하여 불필요한 조회를 방지합니다.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "privacy_consents_user_id", nullable = false)
    private User user;

    /**
     * 개인정보 처리방침 동의 여부.
     *
     * <p>{@code true}면 사용자가 개인정보 처리방침에 동의한 상태입니다.
     * 기본값은 {@code false}이며, {@link #create} 메서드로 생성 시 자동으로 {@code true}로 설정됩니다.</p>
     */
    @Column(name = "privacy_consents_consent_given", nullable = false)
    private boolean consentGiven = false;

    /**
     * 동의 일시.
     *
     * <p>사용자가 개인정보 처리방침에 동의한 시점을 기록합니다.
     * {@link #create} 메서드로 생성 시 현재 시각으로 자동 설정됩니다.</p>
     */
    @Column(name = "privacy_consents_consent_date")
    private Instant consentDate;

    /**
     * 동의한 개인정보 처리방침 버전.
     *
     * <p>예: "v1.0", "v2.0" 등의 형식으로 저장됩니다.
     * 정책 버전이 변경되면 사용자에게 재동의를 요청할 수 있습니다.</p>
     */
    @Column(name = "privacy_consents_policy_version", nullable = false)
    private String policyVersion;

    /**
     * PrivacyConsent 엔티티를 생성합니다.
     * 생성 시 동의가 자동으로 완료된 것으로 처리됩니다.
     *
     * @param user 동의하는 사용자
     * @param policyVersion 동의한 개인정보 처리방침 버전
     * @return 생성된 PrivacyConsent 엔티티
     */
    public static PrivacyConsent create(User user, String policyVersion) {
        PrivacyConsent privacyConsent = new PrivacyConsent();
        privacyConsent.user = user;
        privacyConsent.policyVersion = policyVersion;
        privacyConsent.consentGiven = true;
        privacyConsent.consentDate = Instant.now();
        return privacyConsent;
    }
}
