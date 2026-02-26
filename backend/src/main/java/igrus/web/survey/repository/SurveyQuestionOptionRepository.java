package igrus.web.survey.repository;

import igrus.web.survey.domain.SurveyQuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 설문 질문 선택지 Repository.
 */
public interface SurveyQuestionOptionRepository extends JpaRepository<SurveyQuestionOption, Long> {

    /**
     * 삭제되지 않은 선택지를 ID로 조회합니다.
     *
     * @param id 선택지 ID
     * @return 선택지 Optional
     */
    Optional<SurveyQuestionOption> findByIdAndDeletedFalse(Long id);

    /**
     * 특정 질문의 삭제되지 않은 선택지 목록을 표시 순서로 조회합니다.
     *
     * @param questionId 질문 ID
     * @return 선택지 목록
     */
    List<SurveyQuestionOption> findByQuestionIdAndDeletedFalseOrderByDisplayOrderAsc(Long questionId);
}
