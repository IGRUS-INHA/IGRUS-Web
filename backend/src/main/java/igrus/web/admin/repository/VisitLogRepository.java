package igrus.web.admin.repository;

import igrus.web.admin.domain.VisitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface VisitLogRepository extends JpaRepository<VisitLog, Long> {

    /**
     * 특정 기간 내 방문자 수를 조회합니다.
     *
     * @param start 시작 시각 (포함)
     * @param end   종료 시각 (미포함)
     * @return 방문자 수
     */
    @Query("SELECT COUNT(v) FROM VisitLog v WHERE v.visitedAt >= :start AND v.visitedAt < :end")
    long countByVisitedAtBetween(@Param("start") Instant start, @Param("end") Instant end);
}
