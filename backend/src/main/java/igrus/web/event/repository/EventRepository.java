package igrus.web.event.repository;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 행사 Repository.
 */
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * 신청자 수를 원자적으로 1 증가시킵니다.
     * 정원이 남아있을 때만 증가합니다.
     *
     * @param id 행사 ID
     * @return 변경된 행 수 (1이면 성공, 0이면 정원 초과)
     */
    @Modifying
    @Query("UPDATE Event e SET e.currentCount = e.currentCount + 1 " +
           "WHERE e.id = :id AND e.currentCount < e.capacity")
    int incrementCurrentCountIfAvailable(@Param("id") Long id);

    /**
     * 신청자 수를 원자적으로 1 감소시킵니다.
     * 현재 신청자 수가 0보다 클 때만 감소합니다.
     *
     * @param id 행사 ID
     * @return 변경된 행 수 (1이면 성공, 0이면 이미 0명)
     */
    @Modifying
    @Query("UPDATE Event e SET e.currentCount = e.currentCount - 1 " +
           "WHERE e.id = :id AND e.currentCount > 0")
    int decrementCurrentCount(@Param("id") Long id);

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
