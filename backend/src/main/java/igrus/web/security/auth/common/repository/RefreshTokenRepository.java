package igrus.web.security.auth.common.repository;

import igrus.web.security.auth.common.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId")
    void revokeAllByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") Instant now);

    /**
     * 특정 사용자의 모든 리프레시 토큰을 삭제합니다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 특정 사용자의 모든 리프레시 토큰을 물리적으로 삭제합니다 (hard delete).
     * User의 @SQLRestriction을 우회하여 soft deleted 사용자의 데이터도 삭제합니다.
     *
     * @param userId 사용자 ID
     */
    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE FROM refresh_tokens WHERE refresh_tokens_user_id = :userId", nativeQuery = true)
    void hardDeleteByUserId(@Param("userId") Long userId);
}
