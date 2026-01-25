package igrus.web.security.auth.common.repository;

import igrus.web.security.auth.common.domain.PrivacyConsent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 개인정보 동의 Repository.
 *
 * <p>N:1 관계로 한 사용자가 여러 개의 동의 기록을 가질 수 있습니다.
 * 약관 버전 업데이트 시 새로운 동의 기록이 생성됩니다.</p>
 */
@Repository
public interface PrivacyConsentRepository extends JpaRepository<PrivacyConsent, Long> {

    /**
     * 사용자의 동의 기록을 조회합니다.
     * 여러 개가 있을 경우 첫 번째 기록을 반환합니다.
     *
     * @param userId 사용자 ID
     * @return 동의 기록 (Optional)
     * @deprecated N:1 관계 변경으로 {@link #findFirstByUserIdOrderByConsentDateDesc(Long)} 사용 권장
     */
    @Deprecated
    Optional<PrivacyConsent> findByUserId(Long userId);

    /**
     * 사용자가 동의한 기록이 있는지 확인합니다.
     *
     * @param userId 사용자 ID
     * @return 동의 여부
     */
    boolean existsByUserIdAndConsentGivenTrue(Long userId);

    /**
     * 사용자의 모든 동의 이력을 최신순으로 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 동의 이력 목록 (최신순)
     */
    @EntityGraph(attributePaths = {"user"})
    List<PrivacyConsent> findByUserIdOrderByConsentDateDesc(Long userId);

    /**
     * 사용자의 가장 최근 동의 기록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 최신 동의 기록 (Optional)
     */
    @EntityGraph(attributePaths = {"user"})
    Optional<PrivacyConsent> findFirstByUserIdOrderByConsentDateDesc(Long userId);

    /**
     * 사용자가 특정 버전의 약관에 동의했는지 확인합니다.
     *
     * @param userId 사용자 ID
     * @param policyVersion 정책 버전
     * @return 해당 버전 동의 여부
     */
    boolean existsByUserIdAndPolicyVersion(Long userId, String policyVersion);

    /**
     * 사용자가 특정 버전의 약관에 동의한 기록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param policyVersion 정책 버전
     * @return 동의 기록 (Optional)
     */
    Optional<PrivacyConsent> findByUserIdAndPolicyVersion(Long userId, String policyVersion);

    /**
     * 사용자의 모든 동의 기록을 삭제합니다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}
