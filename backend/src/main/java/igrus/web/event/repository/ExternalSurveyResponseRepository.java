package igrus.web.event.repository;

import igrus.web.event.domain.ExternalSurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 외부인 설문 응답 Repository. (DECISION-04: 옵션 B - 별도 테이블)
 */
public interface ExternalSurveyResponseRepository extends JpaRepository<ExternalSurveyResponse, Long> {

    /**
     * 특정 설문에 대한 외부인 응답 목록을 조회합니다.
     *
     * @param surveyId 설문 ID
     * @return 외부인 설문 응답 목록
     */
    @Query("SELECT r FROM ExternalSurveyResponse r WHERE r.survey.id = :surveyId")
    List<ExternalSurveyResponse> findBySurveyId(@Param("surveyId") Long surveyId);
}
