package igrus.web.event.repository;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * 특정 사용자가 특정 행사에 취소 요청 중인 유효한 신청이 있는지 확인합니다.
     * 행사 상세 조회 시 "취소 요청됨" 표시용.
     */
    boolean existsByEventIdAndUserIdAndStatusInAndCancelRequestedAtIsNotNull(
            Long eventId, Long userId, Collection<EventRegistrationStatus> statuses);


    /**
     * 취소 요청을 원자적으로 기록합니다.
     * 유효한 신청(REGISTERED/WAITING/APPROVED)이고 아직 요청이 없을 때만 기록합니다.
     * 엔티티 전체 UPDATE로 동시에 변경된 status를 덮어쓰지 않도록 두 컬럼만 조건부로 갱신합니다.
     *
     * @return 변경된 행 수 (1이면 성공, 0이면 취소/거절된 신청이거나 이미 요청됨)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE EventRegistration r SET r.cancelReason = :reason, r.cancelRequestedAt = :requestedAt "
            + "WHERE r.id = :id AND r.status IN ('REGISTERED', 'WAITING', 'APPROVED') "
            + "AND r.cancelRequestedAt IS NULL")
    int requestCancelIfActive(@Param("id") Long id, @Param("reason") String reason,
                              @Param("requestedAt") Instant requestedAt);

    /**
     * 처리 대기 중인 취소 요청을 원자적으로 초기화합니다. (운영진의 요청 거절)
     * 이미 취소 처리된 신청의 요청 기록은 알림 이력이므로 지우지 않습니다.
     *
     * @return 변경된 행 수 (1이면 성공, 0이면 대기 중인 요청 없음)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE EventRegistration r SET r.cancelReason = NULL, r.cancelRequestedAt = NULL "
            + "WHERE r.id = :id AND r.cancelRequestedAt IS NOT NULL AND r.status <> 'CANCELED'")
    int clearPendingCancelRequest(@Param("id") Long id);


    /**
     * 취소 요청이 접수된 신청 목록을 조회합니다. (처리 대기 + 이미 취소된 것 모두 포함)
     * 요청 후 운영진이 신청 자체를 거절(REJECTED)한 건은 더 처리할 것이 없으므로 제외합니다.
     * 운영진 알림 페이지용. 행사/신청자를 fetch join하여 N+1을 방지합니다.
     */
    @Query(value = "SELECT r FROM EventRegistration r JOIN FETCH r.event LEFT JOIN FETCH r.user "
            + "WHERE r.cancelRequestedAt IS NOT NULL AND r.status <> 'REJECTED'",
            countQuery = "SELECT COUNT(r) FROM EventRegistration r JOIN r.event "
                    + "WHERE r.cancelRequestedAt IS NOT NULL AND r.status <> 'REJECTED'")
    Page<EventRegistration> findCancelRequested(Pageable pageable);


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

    long countByEventIdAndStatusIn(Long eventId, Collection<EventRegistrationStatus> statuses);


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

}
