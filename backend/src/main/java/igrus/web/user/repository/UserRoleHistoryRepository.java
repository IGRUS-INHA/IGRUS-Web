package igrus.web.user.repository;

import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserRoleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    // === 관리자용 권한 변경 이력 필터링 조회 ===

    /**
     * 권한 변경 이력을 필터링하여 조회합니다.
     * 각 파라미터가 null이면 해당 조건을 무시합니다.
     *
     * @param userId 대상 사용자 ID
     * @param previousRole 변경 전 역할
     * @param newRole 변경 후 역할
     * @param changedBy 변경자 ID
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @param pageable 페이징 정보
     * @return 이력 페이지
     */
    @Query(value = "SELECT h FROM UserRoleHistory h LEFT JOIN FETCH h.user WHERE " +
           "(:userId IS NULL OR h.userId = :userId) " +
           "AND (:previousRole IS NULL OR h.previousRole = :previousRole) " +
           "AND (:newRole IS NULL OR h.newRole = :newRole) " +
           "AND (:changedBy IS NULL OR h.createdBy = :changedBy) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR h.createdAt >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR h.createdAt <= :endDate)",
           countQuery = "SELECT COUNT(h) FROM UserRoleHistory h WHERE " +
           "(:userId IS NULL OR h.userId = :userId) " +
           "AND (:previousRole IS NULL OR h.previousRole = :previousRole) " +
           "AND (:newRole IS NULL OR h.newRole = :newRole) " +
           "AND (:changedBy IS NULL OR h.createdBy = :changedBy) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR h.createdAt >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR h.createdAt <= :endDate)")
    Page<UserRoleHistory> findByFilters(@Param("userId") Long userId,
                                        @Param("previousRole") UserRole previousRole,
                                        @Param("newRole") UserRole newRole,
                                        @Param("changedBy") Long changedBy,
                                        @Param("startDate") Instant startDate,
                                        @Param("endDate") Instant endDate,
                                        Pageable pageable);
}
