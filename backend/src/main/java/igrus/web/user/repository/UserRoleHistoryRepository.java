package igrus.web.user.repository;

import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserRoleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleHistoryRepository extends JpaRepository<UserRoleHistory, Long> {

    List<UserRoleHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserRoleHistory> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    List<UserRoleHistory> findByNewRole(UserRole newRole);

    /**
     * 특정 시각 이후에 지정된 역할로 승인된 사용자 수를 조회합니다.
     *
     * @param newRole 변경된 역할
     * @param startTime 기준 시각
     * @return 승인된 사용자 수
     */
    @Query("SELECT COUNT(h) FROM UserRoleHistory h WHERE h.newRole = :newRole AND h.createdAt >= :startTime")
    long countByNewRoleAndCreatedAtAfter(@Param("newRole") UserRole newRole, @Param("startTime") Instant startTime);
}
