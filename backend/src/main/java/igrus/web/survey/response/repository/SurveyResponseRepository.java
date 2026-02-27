package igrus.web.survey.response.repository;

import igrus.web.survey.response.domain.SurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 설문 응답 Repository.
 */
public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Long> {

    /**
     * 특정 회원이 특정 설문에 이미 응답했는지 확인합니다.
     *
     * @param surveyId 설문 ID
     * @param userId   사용자 ID
     * @return 이미 응답했으면 true
     */
    boolean existsBySurveyIdAndUserId(Long surveyId, Long userId);

    /**
     * 특정 회원의 특정 설문 응답을 조회합니다.
     *
     * @param surveyId 설문 ID
     * @param userId   사용자 ID
     * @return 응답 Optional
     */
    Optional<SurveyResponse> findBySurveyIdAndUserId(Long surveyId, Long userId);

    /**
     * 특정 설문의 모든 응답 목록을 조회합니다. (결과 조회용)
     *
     * @param surveyId 설문 ID
     * @return 응답 목록
     */
    List<SurveyResponse> findBySurveyId(Long surveyId);
}
