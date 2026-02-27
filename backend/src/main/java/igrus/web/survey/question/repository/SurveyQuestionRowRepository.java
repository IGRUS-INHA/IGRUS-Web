package igrus.web.survey.question.repository;

import igrus.web.survey.question.domain.SurveyQuestionRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 설문 그리드 행 Repository.
 */
public interface SurveyQuestionRowRepository extends JpaRepository<SurveyQuestionRow, Long> {

    /**
     * 삭제되지 않은 행을 ID로 조회합니다.
     *
     * @param id 행 ID
     * @return 행 Optional
     */
    Optional<SurveyQuestionRow> findByIdAndDeletedFalse(Long id);

    /**
     * 특정 질문의 삭제되지 않은 행 목록을 표시 순서로 조회합니다.
     *
     * @param questionId 질문 ID
     * @return 행 목록
     */
    List<SurveyQuestionRow> findByQuestionIdAndDeletedFalseOrderByDisplayOrderAsc(Long questionId);
}
