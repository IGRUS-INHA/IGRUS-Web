package igrus.web.event.repository;

import igrus.web.event.domain.EventChangeType;
import igrus.web.event.domain.EventStatusChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventStatusChangeHistoryRepository extends JpaRepository<EventStatusChangeHistory, Long> {

    List<EventStatusChangeHistory> findByEventIdOrderByCreatedAtDesc(Long eventId);

    List<EventStatusChangeHistory> findByEventIdAndChangeTypeOrderByCreatedAtDesc(Long eventId,
                                                                                  EventChangeType changeType);
}
