package igrus.web.event.repository;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 행사 신청 Repository.
 */
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {


    /**
     * 특정 행사에 주어진 상태의 신청이 존재하는지 확인합니다.
     * 행사 삭제 시 활성 신청자 존재 여부 확인용.
     *
     * @param eventId  행사 ID
     * @param statuses 확인 대상 신청 상태 목록
     * @return 해당 상태의 신청이 있으면 true
     */
    boolean existsByEventIdAndStatusIn(Long eventId, Collection<EventRegistrationStatus> statuses);


    /**
     * 특정 사용자가 특정 행사에 이미 신청했는지 확인합니다.
     * 중복 신청 방지용.
     *
     * @param eventId 행사 ID
     * @param userId  사용자 ID
     * @return 이미 신청했으면 true
     */
    boolean existsByEventIdAndUserId(Long eventId, Long userId);


    /**
     * 특정 사용자가 특정 행사에 유효한(활성) 신청이 있는지 확인합니다.
     * REGISTERED, WAITING, APPROVED 상태만 유효한 신청으로 판단합니다.
     * 행사 상세 조회 시 현재 사용자의 신청 여부 표시용.
     *
     * @param eventId  행사 ID
     * @param userId   사용자 ID
     * @param statuses 유효한 신청 상태 목록
     * @return 유효한 신청이 있으면 true
     */
    boolean existsByEventIdAndUserIdAndStatusIn(Long eventId, Long userId, Collection<EventRegistrationStatus> statuses);


    /**
     * 특정 사용자의 특정 행사 신청을 조회합니다.
     * 신청 취소, 재신청 등에서 사용.
     * 조회 후 status를 CANCELED로 변경하여 soft delete 처리.
     *
     * @param eventId 행사 ID
     * @param userId  사용자 ID
     * @return 신청 정보 (없으면 empty)
     */
    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);


    /**
     * 특정 사용자가 신청한 모든 행사 신청 목록을 조회합니다.
     * 마이페이지에서 내 신청 내역 확인용.
     *
     * @param userId 사용자 ID
     * @return 해당 사용자의 신청 목록
     */
    @EntityGraph(attributePaths = {"event"})
    List<EventRegistration> findByUserId(Long userId);


    /**
     * 특정 행사의 모든 신청자 목록을 조회합니다.
     * 관리자가 신청 현황을 확인할 때 사용.
     *
     * @param eventId 행사 ID
     * @return 해당 행사의 신청 목록
     */
    List<EventRegistration> findByEventId(Long eventId);


    /**
     * 특정 행사의 신청자 목록을 페이징하여 조회합니다.
     *
     * @param eventId  행사 ID
     * @param pageable 페이징 정보
     * @return 페이징된 신청 목록
     */
    Page<EventRegistration> findByEventId(Long eventId, Pageable pageable);


    /**
     * 특정 행사의 특정 상태인 신청 목록을 조회합니다.
     * 선발제에서 대기 중인 신청자만 조회할 때 사용.
     *
     * @param eventId 행사 ID
     * @param status  신청 상태
     * @return 해당 상태의 신청 목록
     */
    List<EventRegistration> findByEventIdAndStatus(Long eventId, EventRegistrationStatus status);


    /**
     * 특정 행사의 특정 상태인 신청자 수를 카운트합니다.
     * 정원 확인용 (REGISTERED나 APPROVED 상태만 카운트).
     *
     * @param eventId 행사 ID
     * @param status  신청 상태
     * @return 해당 상태의 신청 수
     */
    long countByEventIdAndStatus(Long eventId, EventRegistrationStatus status);


    /**
     * 특정 사용자의 확정된 신청(REGISTERED, APPROVED) 중
     * 주어진 행사 시간과 겹치는 신청이 있는지 확인합니다.
     * 겹침 조건: 기존 행사 시작 < 새 행사 종료 AND 기존 행사 종료 > 새 행사 시작
     *
     * @param userId       사용자 ID
     * @param eventStartAt 신청하려는 행사 시작 시간
     * @param eventEndAt   신청하려는 행사 종료 시간
     * @param statuses     확인 대상 신청 상태 목록 (REGISTERED, APPROVED)
     * @return 시간이 겹치는 신청이 있으면 true
     */
    @Query("SELECT COUNT(r) > 0 FROM EventRegistration r " +
            "WHERE r.user.id = :userId " +
            "AND r.status IN :statuses " +
            "AND r.event.eventStartAt < :eventEndAt " +
            "AND r.event.eventEndAt > :eventStartAt")
    boolean existsOverlappingRegistration(
            @Param("userId") Long userId,
            @Param("eventStartAt") Instant eventStartAt,
            @Param("eventEndAt") Instant eventEndAt,
            @Param("statuses") Collection<EventRegistrationStatus> statuses);


    // === 외부인 중복 검사 쿼리 (DECISION-02: 서비스 레벨만) ===

    /**
     * 동일 행사에서 동일 studentId로 활성(CANCELED 제외) 외부인 신청이 존재하는지 확인합니다.
     * EXT-INV-02: studentId 기준 중복 방지.
     *
     * @param event          행사
     * @param studentId      외부인 학번
     * @param excludedStatus 제외할 상태 (CANCELED)
     * @return 중복 신청이 존재하면 true
     */
    boolean existsByEventAndExternalStudentIdAndStatusNot(Event event, String studentId,
                                                          EventRegistrationStatus excludedStatus);

    /**
     * 동일 행사에서 동일 phone으로 활성(CANCELED 제외) 외부인 신청이 존재하는지 확인합니다.
     * EXT-INV-03: phone 기준 중복 방지.
     *
     * @param event          행사
     * @param phone          외부인 전화번호
     * @param excludedStatus 제외할 상태 (CANCELED)
     * @return 중복 신청이 존재하면 true
     */
    boolean existsByEventAndExternalPhoneAndStatusNot(Event event, String phone,
                                                      EventRegistrationStatus excludedStatus);

    /**
     * 동일 studentId의 외부인 신청 중 시간이 겹치는 활성 신청이 존재하는지 확인합니다.
     * DECISION-06: studentId 기반 시간 겹침 검증.
     *
     * @param studentId      외부인 학번
     * @param eventStartAt   신청하려는 행사 시작 시간
     * @param eventEndAt     신청하려는 행사 종료 시간
     * @param excludedStatus 제외할 상태 (CANCELED)
     * @return 시간이 겹치는 신청이 있으면 true
     */
    @Query("SELECT COUNT(r) > 0 FROM EventRegistration r " +
            "WHERE r.externalStudentId = :studentId " +
            "AND r.isExternal = true " +
            "AND r.status <> :excludedStatus " +
            "AND r.event.eventStartAt < :eventEndAt " +
            "AND r.event.eventEndAt > :eventStartAt")
    boolean existsOverlappingExternalRegistration(
            @Param("studentId") String studentId,
            @Param("eventStartAt") Instant eventStartAt,
            @Param("eventEndAt") Instant eventEndAt,
            @Param("excludedStatus") EventRegistrationStatus excludedStatus);
}
