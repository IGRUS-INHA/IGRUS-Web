package igrus.web.event.repository;

import igrus.web.event.domain.EventReopenHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 행사 등록 수동 재오픈 감사 이력 Repository.
 */
public interface EventReopenHistoryRepository extends JpaRepository<EventReopenHistory, Long> {

    /**
     * 특정 행사의 재오픈 이력을 조회합니다.
     *
     * @param eventId 행사 ID
     * @return 재오픈 이력 목록
     */
    List<EventReopenHistory> findByEventId(Long eventId);
}
