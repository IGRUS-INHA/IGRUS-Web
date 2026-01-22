package igrus.web.user.repository;

import igrus.web.user.domain.UserSuspension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSuspensionRepository extends JpaRepository<UserSuspension, Long> {

    /**
     * 사용자의 모든 정지 이력 조회 (최신순)
     */
    List<UserSuspension> findByUserIdOrderBySuspendedAtDesc(Long userId);

    /**
     * 사용자의 가장 최근 정지 이력 조회
     */
    Optional<UserSuspension> findFirstByUserIdOrderBySuspendedAtDesc(Long userId);

    /**
     * 사용자의 현재 유효한 정지 조회 (해제되지 않았고, 정지 기간 내인 경우)
     */
    @Query("SELECT s FROM UserSuspension s WHERE s.user.id = :userId " +
            "AND s.liftedAt IS NULL " +
            "AND s.suspendedAt <= :now " +
            "AND s.suspendedUntil > :now")
    Optional<UserSuspension> findActiveByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * 사용자의 현재 유효한 정지 존재 여부 확인
     */
    @Query("SELECT COUNT(s) > 0 FROM UserSuspension s WHERE s.user.id = :userId " +
            "AND s.liftedAt IS NULL " +
            "AND s.suspendedAt <= :now " +
            "AND s.suspendedUntil > :now")
    boolean existsActiveByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * 사용자의 해제되지 않은 정지 이력 조회
     */
    List<UserSuspension> findByUserIdAndLiftedAtIsNullOrderBySuspendedAtDesc(Long userId);

    /**
     * 특정 처리자가 생성한 정지 이력 조회
     */
    List<UserSuspension> findBySuspendedByOrderBySuspendedAtDesc(Long suspendedBy);

    /**
     * 만료되었지만 해제 처리되지 않은 정지 이력 조회
     */
    @Query("SELECT s FROM UserSuspension s WHERE s.liftedAt IS NULL AND s.suspendedUntil < :now")
    List<UserSuspension> findExpiredButNotLifted(@Param("now") Instant now);
}
