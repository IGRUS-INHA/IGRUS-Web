package igrus.web.event.repository;

import igrus.web.event.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 행사 Repository.
 *
 * <p>기본 제공 메서드:</p>
 * <ul>
 *   <li>{@code save(Event)} - 행사 저장</li>
 *   <li>{@code findById(Long)} - ID로 행사 조회</li>
 *   <li>{@code delete(Event)} - 행사 삭제</li>
 *   <li>{@code findAll()} - 전체 행사 조회</li>
 * </ul>
 */
public interface EventRepository extends JpaRepository<Event, Long> {

    // TODO: 기능 분석 후 필요한 메서드 추가
}
