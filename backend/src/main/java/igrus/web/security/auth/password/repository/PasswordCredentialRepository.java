package igrus.web.security.auth.password.repository;

import igrus.web.security.auth.password.domain.PasswordCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordCredentialRepository extends JpaRepository<PasswordCredential, Long> {

    // === 기본 조회 (soft delete 자동 필터링 by @SQLRestriction) ===

    Optional<PasswordCredential> findByUserId(Long userId);

    @Query("SELECT pc FROM PasswordCredential pc WHERE pc.user.email = :email")
    Optional<PasswordCredential> findByUserEmail(@Param("email") String email);

    boolean existsByUserId(Long userId);

    // === 삭제된 데이터 포함 조회 (native query로 @SQLRestriction 우회) ===

    @Query(value = "SELECT * FROM password_credentials pc WHERE pc.password_credentials_user_id = :userId", nativeQuery = true)
    Optional<PasswordCredential> findByUserIdIncludingDeleted(@Param("userId") Long userId);

    /**
     * 사용자의 비밀번호 자격 증명을 삭제합니다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}
