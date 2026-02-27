package igrus.web.survey.response.repository;

import igrus.web.survey.response.domain.SurveyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 설문 답변 Repository.
 */
public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, Long> {

    /**
     * 특정 응답의 모든 답변을 조회합니다.
     *
     * @param responseId 응답 ID
     * @return 답변 목록
     */
    List<SurveyAnswer> findByResponseId(Long responseId);

    /**
     * 특정 선택지를 참조하는 답변이 존재하는지 확인합니다.
     *
     * @param optionId 선택지 ID
     * @return 참조 중이면 true
     */
    boolean existsBySelectedOptionId(Long optionId);

    /**
     * 특정 행을 참조하는 답변이 존재하는지 확인합니다.
     *
     * @param rowId 행 ID
     * @return 참조 중이면 true
     */
    boolean existsBySelectedRowId(Long rowId);

    /**
     * 특정 질문을 참조하는 답변이 존재하는지 확인합니다.
     *
     * @param questionId 질문 ID
     * @return 참조 중이면 true
     */
    boolean existsByQuestionId(Long questionId);

    /**
     * 특정 설문의 유효 답변 목록을 조회합니다.
     * SurveyAnswer.deleted=false AND SurveyResponse.deleted=false 조건을 적용합니다.
     * N+1 방지를 위해 question, selectedOption, selectedRow, response를 fetch join합니다.
     *
     * @param surveyId 설문 ID
     * @return 유효 답변 목록
     */
    @Query("SELECT a FROM SurveyAnswer a " +
            "JOIN FETCH a.response r " +
            "LEFT JOIN FETCH r.user " +
            "JOIN FETCH a.question q " +
            "LEFT JOIN FETCH a.selectedOption " +
            "LEFT JOIN FETCH a.selectedRow " +
            "WHERE r.survey.id = :surveyId " +
            "AND r.deleted = false " +
            "AND a.deleted = false")
    List<SurveyAnswer> findValidAnswersBySurveyId(@Param("surveyId") Long surveyId);
}
