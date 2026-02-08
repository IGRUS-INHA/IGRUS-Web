package igrus.web.security.auth.common.repository;

import igrus.web.security.auth.common.domain.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    /** 특정 사용자의 로그인 히스토리 조회 (최신순) */
    Page<LoginHistory> findByUserIdOrderByAttemptedAtDesc(Long userId, Pageable pageable);

    /** 특정 학번의 로그인 히스토리 조회 (최신순) */
    Page<LoginHistory> findByStudentIdOrderByAttemptedAtDesc(String studentId, Pageable pageable);

    /** 특정 IP의 로그인 히스토리 조회 (최신순) */
    Page<LoginHistory> findByIpAddressOrderByAttemptedAtDesc(String ipAddress, Pageable pageable);

    /** 특정 사용자의 최근 로그인 성공 기록 조회 */
    List<LoginHistory> findTop10ByUserIdAndSuccessTrueOrderByAttemptedAtDesc(Long userId);

    /** 특정 기간 내 특정 IP의 실패 횟수 조회 */
    @Query("SELECT COUNT(lh) FROM LoginHistory lh " +
           "WHERE lh.ipAddress = :ipAddress " +
           "AND lh.success = false " +
           "AND lh.attemptedAt >= :since")
    long countFailuresByIpAddressSince(@Param("ipAddress") String ipAddress,
                                       @Param("since") Instant since);

    /** 특정 기간 내 특정 학번의 실패 횟수 조회 */
    @Query("SELECT COUNT(lh) FROM LoginHistory lh " +
           "WHERE lh.studentId = :studentId " +
           "AND lh.success = false " +
           "AND lh.attemptedAt >= :since")
    long countFailuresByStudentIdSince(@Param("studentId") String studentId,
                                       @Param("since") Instant since);

    /** 관리자용 복합 필터 로그인 이력 조회 (N+1 방지를 위해 LEFT JOIN FETCH) */
    @Query(value = "SELECT lh FROM LoginHistory lh LEFT JOIN FETCH lh.user " +
           "WHERE (:studentId IS NULL OR lh.studentId = :studentId) " +
           "AND (:success IS NULL OR lh.success = :success) " +
           "AND (:ipAddress IS NULL OR lh.ipAddress = :ipAddress) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR lh.attemptedAt >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR lh.attemptedAt <= :endDate)",
           countQuery = "SELECT COUNT(lh) FROM LoginHistory lh " +
           "WHERE (:studentId IS NULL OR lh.studentId = :studentId) " +
           "AND (:success IS NULL OR lh.success = :success) " +
           "AND (:ipAddress IS NULL OR lh.ipAddress = :ipAddress) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR lh.attemptedAt >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR lh.attemptedAt <= :endDate)")
    Page<LoginHistory> findByFilters(@Param("studentId") String studentId,
                                     @Param("success") Boolean success,
                                     @Param("ipAddress") String ipAddress,
                                     @Param("startDate") Instant startDate,
                                     @Param("endDate") Instant endDate,
                                     Pageable pageable);
}
