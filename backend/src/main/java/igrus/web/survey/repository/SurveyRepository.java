package igrus.web.survey.repository;

import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 설문 Repository.
 */
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    /**
     * 삭제되지 않은 설문을 ID로 조회합니다.
     *
     * @param id 설문 ID
     * @return 설문 Optional
     */
    Optional<Survey> findByIdAndDeletedFalse(Long id);

    /**
     * 삭제되지 않은 특정 상태의 설문 목록을 조회합니다.
     *
     * @param status 설문 상태
     * @return 해당 상태의 설문 목록
     */
    List<Survey> findByStatusAndDeletedFalse(SurveyStatus status);

    /**
     * 마감일이 경과한 PUBLISHED 설문 목록을 조회합니다. (자동 마감 스케줄러용)
     *
     * @param status   설문 상태 (PUBLISHED)
     * @param deadline 기준 시각 (현재 시각)
     * @return 마감 대상 설문 목록
     */
    List<Survey> findByStatusAndDeletedFalseAndDeadlineBefore(SurveyStatus status, Instant deadline);
}
