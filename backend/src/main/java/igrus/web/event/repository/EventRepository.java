package igrus.web.event.repository;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 행사 Repository.
 */
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * 비관적 락으로 행사를 조회합니다.
     * 신청/취소 시 동시성 제어를 위해 사용합니다.
     *
     * @param id 행사 ID
     * @return 락이 걸린 행사
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdWithLock(@Param("id") Long id);

    /**
     * 특정 상태의 행사 목록을 조회합니다.
     *
     * @param status 행사 상태
     * @return 해당 상태의 행사 목록
     */
    List<Event> findByStatus(EventStatus status);

    /**
     * 특정 사용자(운영자)가 생성한 행사 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 해당 사용자가 생성한 행사 목록
     */
    List<Event> findByUserId(Long userId);
}
