package igrus.web.event.repository;

import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 행사 신청 Repository.
 */
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {


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
}
