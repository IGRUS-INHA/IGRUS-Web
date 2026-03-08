package igrus.web.event.repository;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventStatus;
import igrus.web.event.domain.EventVisibility;
import igrus.web.event.domain.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 행사 Repository.
 *
 * <p>Event 엔티티에 {@code @SQLRestriction("event_deleted = false")}가 적용되어 있으므로
 * SELECT 쿼리에서 soft delete 필터링이 자동으로 수행됩니다.
 * {@code @Modifying} UPDATE 쿼리에는 @SQLRestriction이 적용되지 않으므로
 * 명시적으로 {@code e.deleted = false} 조건을 유지합니다.</p>
 */
public interface EventRepository extends JpaRepository<Event, Long> {

    // === 기본 조회 (soft delete 자동 필터링 by @SQLRestriction) ===

    /**
     * 삭제되지 않은 행사를 ID로 조회합니다.
     *
     * @param id 행사 ID
     * @return 삭제되지 않은 행사
     */
    default Optional<Event> findByIdAndNotDeleted(Long id) {
        return findById(id);
    }

    /**
     * 삭제되지 않은 모든 행사를 조회합니다.
     *
     * @return 삭제되지 않은 행사 목록
     */
    default List<Event> findAllNotDeleted() {
        return findAll();
    }

    /**
     * 삭제되지 않은 특정 행사 진행 상태의 행사 목록을 조회합니다.
     *
     * @param eventStatus 행사 진행 상태
     * @return 해당 상태의 삭제되지 않은 행사 목록
     */
    List<Event> findByEventStatus(EventStatus eventStatus);

    /**
     * 삭제되지 않은 특정 등록 상태의 행사 목록을 조회합니다.
     *
     * @param registrationStatus 등록 상태
     * @return 해당 상태의 삭제되지 않은 행사 목록
     */
    List<Event> findByRegistrationStatus(RegistrationStatus registrationStatus);

    /**
     * 삭제되지 않은 특정 사용자(운영자)가 생성한 행사 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 해당 사용자가 생성한 삭제되지 않은 행사 목록
     */
    List<Event> findByUserId(Long userId);

    // === 공개 API 조회 (visibility = PUBLISHED 필터) ===

    /**
     * 공개(PUBLISHED) 행사를 ID로 조회합니다.
     * 공개 API 단건 조회용입니다.
     *
     * @param id         행사 ID
     * @param visibility 공개 상태 (PUBLISHED)
     * @return 공개 상태의 행사
     */
    Optional<Event> findByIdAndVisibility(Long id, EventVisibility visibility);

    /**
     * 공개(PUBLISHED) 행사 목록을 복합 필터로 조회합니다.
     * 공개 API 목록 조회용입니다. visibility는 항상 PUBLISHED로 고정됩니다.
     * eventStatus, registrationStatus 파라미터가 null이면 해당 필터를 적용하지 않습니다.
     * 두 필터를 동시에 지정해도 모두 적용됩니다.
     *
     * @param visibility         공개 상태 (항상 PUBLISHED)
     * @param eventStatus        행사 진행 상태 필터 (null이면 전체)
     * @param registrationStatus 등록 상태 필터 (null이면 전체)
     * @return 필터 조건에 맞는 공개 행사 목록
     */
    @Query("SELECT e FROM Event e WHERE " +
           "e.visibility = :visibility AND " +
           "(:eventStatus IS NULL OR e.eventStatus = :eventStatus) AND " +
           "(:registrationStatus IS NULL OR e.registrationStatus = :registrationStatus) " +
           "ORDER BY e.eventStartAt ASC")
    List<Event> findByVisibilityAndFilters(
            @Param("visibility") EventVisibility visibility,
            @Param("eventStatus") EventStatus eventStatus,
            @Param("registrationStatus") RegistrationStatus registrationStatus);

    // === 관리자 API 조회 (visibility 선택적 필터) ===

    /**
     * 관리자용 행사 목록을 조회합니다.
     * visibility, eventStatus, registrationStatus 파라미터가 null이면 해당 필터를 적용하지 않습니다.
     *
     * @param visibility         공개 상태 필터 (null이면 전체)
     * @param eventStatus        행사 진행 상태 필터 (null이면 전체)
     * @param registrationStatus 등록 상태 필터 (null이면 전체)
     * @return 필터 조건에 맞는 행사 목록
     */
    @Query("SELECT e FROM Event e WHERE " +
           "(:visibility IS NULL OR e.visibility = :visibility) AND " +
           "(:eventStatus IS NULL OR e.eventStatus = :eventStatus) AND " +
           "(:registrationStatus IS NULL OR e.registrationStatus = :registrationStatus)")
    List<Event> findAllByAdminFilters(
            @Param("visibility") EventVisibility visibility,
            @Param("eventStatus") EventStatus eventStatus,
            @Param("registrationStatus") RegistrationStatus registrationStatus);

    /**
     * 특정 설문과 연결된 행사를 조회합니다.
     * 설문 응답 삭제 시 연결된 행사 신청 취소용.
     *
     * @param surveyId 설문 ID
     * @return 연결된 행사 (없으면 empty)
     */
    Optional<Event> findBySurveyId(Long surveyId);

    /**
     * 특정 설문이 다른 행사에 이미 연결되어 있는지 확인합니다. (수정 시 자기 자신 제외)
     *
     * @param surveyId 설문 ID
     * @param eventId  제외할 행사 ID (자기 자신)
     * @return 다른 행사에 이미 연결되어 있으면 true
     */
    boolean existsBySurveyIdAndIdNot(Long surveyId, Long eventId);

    // === 원자적 UPDATE (@SQLRestriction 미적용, 명시적 deleted 조건 필요) ===

    /**
     * 신청자 수를 원자적으로 1 증가시킵니다.
     * 정원이 남아있고 등록 상태가 OPEN일 때만 증가합니다.
     *
     * <p>clearAutomatically: UPDATE 후 영속성 컨텍스트를 자동 초기화하여
     * 이후 조회 시 DB의 최신 값을 가져오도록 합니다.</p>
     *
     * @param id 행사 ID
     * @return 변경된 행 수 (1이면 성공, 0이면 정원 초과 또는 OPEN 아님)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Event e SET e.currentCount = e.currentCount + 1 " +
           "WHERE e.id = :id AND e.currentCount < e.capacity AND e.registrationStatus = 'OPEN' AND e.deleted = false")
    int incrementCurrentCountIfAvailable(@Param("id") Long id);

    /**
     * 신청자 수를 원자적으로 1 증가시킵니다. (선발제 승인 전용)
     * 정원이 남아있을 때만 증가합니다. 등록 상태는 체크하지 않습니다.
     *
     * <p>선발제 승인은 신청 기간이 종료된 후에도 가능해야 하므로
     * 등록 상태(OPEN/CLOSED)와 관계없이 정원만 체크합니다.</p>
     *
     * @param id 행사 ID
     * @return 변경된 행 수 (1이면 성공, 0이면 정원 초과)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Event e SET e.currentCount = e.currentCount + 1 " +
           "WHERE e.id = :id AND e.currentCount < e.capacity AND e.deleted = false")
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
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Event e SET e.currentCount = e.currentCount - 1 " +
           "WHERE e.id = :id AND e.currentCount > 0 AND e.deleted = false")
    int decrementCurrentCount(@Param("id") Long id);
}
