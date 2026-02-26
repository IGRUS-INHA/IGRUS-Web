package igrus.web.survey.repository;

import igrus.web.survey.domain.SurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 설문 질문 Repository.
 */
public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, Long> {

    /**
     * 삭제되지 않은 질문을 ID로 조회합니다.
     *
     * @param id 질문 ID
     * @return 질문 Optional
     */
    Optional<SurveyQuestion> findByIdAndDeletedFalse(Long id);

    /**
     * 특정 설문의 삭제되지 않은 질문 목록을 표시 순서로 조회합니다.
     *
     * @param surveyId 설문 ID
     * @return 질문 목록
     */
    List<SurveyQuestion> findBySurveyIdAndDeletedFalseOrderByDisplayOrderAsc(Long surveyId);

    /**
     * 특정 설문의 삭제되지 않은 질문 수를 조회합니다. (INV-04: 질문 수 1~50 검증용)
     *
     * @param surveyId 설문 ID
     * @return 질문 수
     */
    long countBySurveyIdAndDeletedFalse(Long surveyId);
}
