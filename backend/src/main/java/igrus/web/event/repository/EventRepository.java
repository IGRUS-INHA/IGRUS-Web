package igrus.web.event.repository;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 행사 Repository.
 */
public interface EventRepository extends JpaRepository<Event, Long> {

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
