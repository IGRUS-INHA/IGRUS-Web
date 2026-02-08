package igrus.web.user.repository;

import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.domain.AccountStatusChangeHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AccountStatusChangeHistoryRepository extends JpaRepository<AccountStatusChangeHistory, Long> {

    @Query(value = """
            SELECT h FROM AccountStatusChangeHistory h
            WHERE (:userId IS NULL OR h.userId = :userId)
            AND (:changedByUserId IS NULL OR h.changedByUserId = :changedByUserId)
            AND (:changeType IS NULL OR h.changeType = :changeType)
            AND (CAST(:startDate AS timestamp) IS NULL OR h.createdAt >= :startDate)
            AND (CAST(:endDate AS timestamp) IS NULL OR h.createdAt <= :endDate)
            """,
            countQuery = """
            SELECT COUNT(h) FROM AccountStatusChangeHistory h
            WHERE (:userId IS NULL OR h.userId = :userId)
            AND (:changedByUserId IS NULL OR h.changedByUserId = :changedByUserId)
            AND (:changeType IS NULL OR h.changeType = :changeType)
            AND (CAST(:startDate AS timestamp) IS NULL OR h.createdAt >= :startDate)
            AND (CAST(:endDate AS timestamp) IS NULL OR h.createdAt <= :endDate)
            """)
    Page<AccountStatusChangeHistory> findByFilters(
            @Param("userId") Long userId,
            @Param("changedByUserId") Long changedByUserId,
            @Param("changeType") AccountChangeType changeType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );
}
