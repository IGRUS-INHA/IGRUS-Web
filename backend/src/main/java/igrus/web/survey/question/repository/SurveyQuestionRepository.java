package igrus.web.survey.question.repository;

import igrus.web.survey.question.domain.SurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 설문 질문 Repository.
 */
public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, Long> {

    /**
     * 활성(아카이브되지 않은) 질문을 ID로 조회합니다.
     *
     * @param id 질문 ID
     * @return 질문 Optional
     */
    Optional<SurveyQuestion> findByIdAndArchivedAtIsNull(Long id);

    /**
     * 특정 설문의 활성 질문 목록을 표시 순서로 조회합니다.
     *
     * @param surveyId 설문 ID
     * @return 질문 목록
     */
    List<SurveyQuestion> findBySurveyIdAndArchivedAtIsNullOrderByDisplayOrderAsc(Long surveyId);

    /**
     * 특정 설문의 활성 질문 수를 조회합니다.
     *
     * @param surveyId 설문 ID
     * @return 질문 수
     */
    long countBySurveyIdAndArchivedAtIsNull(Long surveyId);

    /**
     * 특정 설문의 활성 질문을 displayOrder 오름차순으로 조회합니다 (options fetch join).
     * N+1 방지를 위해 options를 함께 조회합니다.
     *
     * <p>rows와 동시에 fetch join하면 MultipleBagFetchException이 발생하므로,
     * {@link #findAllBySurveyIdWithRows(Long)}와 분리하여 사용합니다.
     * 같은 트랜잭션 내에서 두 쿼리를 순차 호출하면 영속성 컨텍스트가 결과를 병합합니다.
     *
     * @param surveyId 설문 ID
     * @return 활성 질문 목록 (displayOrder 오름차순)
     */
    @Query("SELECT DISTINCT q FROM SurveyQuestion q " +
            "LEFT JOIN FETCH q.options " +
            "WHERE q.survey.id = :surveyId " +
            "AND q.archivedAt IS NULL " +
            "ORDER BY q.displayOrder ASC")
    List<SurveyQuestion> findAllBySurveyIdWithOptions(@Param("surveyId") Long surveyId);

    /**
     * 특정 설문의 활성 질문을 displayOrder 오름차순으로 조회합니다 (rows fetch join).
     * {@link #findAllBySurveyIdWithOptions(Long)}와 함께 사용하여
     * MultipleBagFetchException을 방지합니다.
     *
     * @param surveyId 설문 ID
     * @return 활성 질문 목록 (displayOrder 오름차순)
     */
    @Query("SELECT DISTINCT q FROM SurveyQuestion q " +
            "LEFT JOIN FETCH q.rows " +
            "WHERE q.survey.id = :surveyId " +
            "AND q.archivedAt IS NULL " +
            "ORDER BY q.displayOrder ASC")
    List<SurveyQuestion> findAllBySurveyIdWithRows(@Param("surveyId") Long surveyId);
}
