package igrus.web.survey.response.repository;

import igrus.web.survey.response.domain.SurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * 특정 회원의 특정 설문 응답을 답변 및 관련 엔티티와 함께 조회합니다.
     * N+1 쿼리 방지를 위해 answers, question, selectedOption, selectedRow를 fetch join합니다.
     *
     * @param surveyId 설문 ID
     * @param userId   사용자 ID
     * @return 응답 Optional (답변 포함)
     */
    @Query("SELECT DISTINCT r FROM SurveyResponse r " +
            "LEFT JOIN FETCH r.answers a " +
            "LEFT JOIN FETCH a.question " +
            "LEFT JOIN FETCH a.selectedOption " +
            "LEFT JOIN FETCH a.selectedRow " +
            "WHERE r.survey.id = :surveyId AND r.user.id = :userId")
    Optional<SurveyResponse> findBySurveyIdAndUserIdWithAnswers(
            @Param("surveyId") Long surveyId,
            @Param("userId") Long userId);

    /**
     * 특정 설문의 모든 응답 목록을 조회합니다. (결과 조회용)
     *
     * @param surveyId 설문 ID
     * @return 응답 목록
     */
    List<SurveyResponse> findBySurveyId(Long surveyId);

    /**
     * 특정 설문의 유효(deleted=false) 응답 목록을 createdAt 오름차순으로 조회합니다.
     * 통계 집계 시 삭제된 응답을 제외하기 위해 사용합니다.
     *
     * @param surveyId 설문 ID
     * @return 유효 응답 목록 (createdAt 오름차순)
     */
    List<SurveyResponse> findBySurveyIdAndDeletedFalseOrderByCreatedAtAsc(Long surveyId);
}
