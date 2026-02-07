package igrus.web.event.repository;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 행사 Repository.
 */
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * 삭제되지 않은 행사를 ID로 조회합니다.
     *
     * @param id 행사 ID
     * @return 삭제되지 않은 행사
     */
    @Query("SELECT e FROM Event e WHERE e.id = :id AND e.deleted = false")
    Optional<Event> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 삭제되지 않은 모든 행사를 조회합니다.
     *
     * @return 삭제되지 않은 행사 목록
     */
    @Query("SELECT e FROM Event e WHERE e.deleted = false")
    List<Event> findAllNotDeleted();

    /**
     * 신청자 수를 원자적으로 1 증가시킵니다.
     * 정원이 남아있고 행사가 OPEN 상태일 때만 증가합니다.
     *
     * <p>clearAutomatically: UPDATE 후 영속성 컨텍스트를 자동 초기화하여
     * 이후 조회 시 DB의 최신 값을 가져오도록 합니다.</p>
     *
     * @param id 행사 ID
     * @return 변경된 행 수 (1이면 성공, 0이면 정원 초과 또는 OPEN 아님)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Event e SET e.currentCount = e.currentCount + 1 " +
           "WHERE e.id = :id AND e.currentCount < e.capacity AND e.status = 'OPEN'")
    int incrementCurrentCountIfAvailable(@Param("id") Long id);

    /**
     * 신청자 수를 원자적으로 1 증가시킵니다. (선발제 승인 전용)
     * 정원이 남아있을 때만 증가합니다. 행사 상태는 체크하지 않습니다.
     *
     * <p>선발제 승인은 신청 기간이 종료된 후에도 가능해야 하므로
     * 행사 상태(OPEN/CLOSED)와 관계없이 정원만 체크합니다.</p>
     *
     * @param id 행사 ID
     * @return 변경된 행 수 (1이면 성공, 0이면 정원 초과)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Event e SET e.currentCount = e.currentCount + 1 " +
           "WHERE e.id = :id AND e.currentCount < e.capacity")
    int incrementCurrentCountForApproval(@Param("id") Long id);

    /**
     * 신청자 수를 원자적으로 1 감소시킵니다.
     * 현재 신청자 수가 0보다 클 때만 감소합니다.
     *
     * <p>clearAutomatically: UPDATE 후 영속성 컨텍스트를 자동 초기화하여
     * 이후 조회 시 DB의 최신 값을 가져오도록 합니다.</p>
     *
     * @param id 행사 ID
     * @return 변경된 행 수 (1이면 성공, 0이면 이미 0명)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Event e SET e.currentCount = e.currentCount - 1 " +
           "WHERE e.id = :id AND e.currentCount > 0")
    int decrementCurrentCount(@Param("id") Long id);

    /**
     * 삭제되지 않은 특정 상태의 행사 목록을 조회합니다.
     *
     * @param status 행사 상태
     * @return 해당 상태의 삭제되지 않은 행사 목록
     */
    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.deleted = false")
    List<Event> findByStatusAndNotDeleted(@Param("status") EventStatus status);

    /**
     * 삭제되지 않은 특정 사용자(운영자)가 생성한 행사 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 해당 사용자가 생성한 삭제되지 않은 행사 목록
     */
    @Query("SELECT e FROM Event e WHERE e.user.id = :userId AND e.deleted = false")
    List<Event> findByUserIdAndNotDeleted(@Param("userId") Long userId);
}
