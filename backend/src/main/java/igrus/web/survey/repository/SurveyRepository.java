package igrus.web.survey.repository;

import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyResponseStatus;
import igrus.web.survey.domain.SurveyVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 설문 Repository.
 */
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    // === 기존 유지 (SoftDeletableEntity의 deleted 필드 기반) ===

    /**
     * 삭제되지 않은 설문을 ID로 조회합니다.
     *
     * @param id 설문 ID
     * @return 설문 Optional
     */
    Optional<Survey> findByIdAndDeletedFalse(Long id);

    /**
     * 삭제되지 않은 모든 설문을 조회합니다.
     *
     * @return 삭제되지 않은 설문 목록
     */
    List<Survey> findByDeletedFalse();

    // === 활성 설문 조회 (휴지통 제외) ===

    /**
     * 활성 설문 단건 조회 (삭제되지 않고 휴지통에 있지 않은 설문).
     *
     * @param id 설문 ID
     * @return 설문 Optional
     */
    Optional<Survey> findByIdAndDeletedFalseAndTrashedAtIsNull(Long id);

    /**
     * 활성 설문 목록 조회 (삭제되지 않고 휴지통에 있지 않은 설문). (INV-17)
     *
     * @return 활성 설문 목록
     */
    List<Survey> findByDeletedFalseAndTrashedAtIsNull();

    // === 휴지통 조회 ===

    /**
     * 휴지통 설문 목록 조회 (삭제되지 않고 휴지통에 있는 설문). (INV-17)
     *
     * @return 휴지통 설문 목록
     */
    List<Survey> findByDeletedFalseAndTrashedAtIsNotNull();

    /**
     * 휴지통 내 설문 단건 조회.
     *
     * @param id 설문 ID
     * @return 설문 Optional
     */
    Optional<Survey> findByIdAndDeletedFalseAndTrashedAtIsNotNull(Long id);

    // === 상태 기반 조회 (2축 모델) ===

    /**
     * 공개 상태 기준으로 활성 설문을 조회합니다.
     *
     * @param visibility 공개 상태
     * @return 해당 공개 상태의 활성 설문 목록
     */
    List<Survey> findByVisibilityAndDeletedFalseAndTrashedAtIsNull(SurveyVisibility visibility);

    /**
     * 공개 상태 + 응답 수집 상태 조합으로 활성 설문을 조회합니다.
     *
     * @param visibility     공개 상태
     * @param responseStatus 응답 수집 상태
     * @return 해당 상태 조합의 활성 설문 목록
     */
    List<Survey> findByVisibilityAndResponseStatusAndDeletedFalseAndTrashedAtIsNull(
            SurveyVisibility visibility, SurveyResponseStatus responseStatus);

    /**
     * 마감일이 경과한 OPEN 상태의 활성 설문을 조회합니다. (자동 마감 스케줄러용)
     *
     * @param responseStatus 응답 수집 상태 (OPEN)
     * @param deadline       기준 시각 (현재 시각)
     * @return 마감 대상 설문 목록
     */
    List<Survey> findByResponseStatusAndDeletedFalseAndTrashedAtIsNullAndDeadlineBefore(
            SurveyResponseStatus responseStatus, Instant deadline);
}
