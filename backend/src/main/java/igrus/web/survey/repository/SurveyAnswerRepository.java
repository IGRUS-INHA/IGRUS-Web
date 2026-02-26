package igrus.web.survey.repository;

import igrus.web.survey.domain.SurveyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

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
     * 특정 선택지를 참조하는 답변이 존재하는지 확인합니다. (INV-10: 선택지 삭제 전 참조 검증)
     *
     * @param optionId 선택지 ID
     * @return 참조 중이면 true
     */
    boolean existsBySelectedOptionId(Long optionId);

    /**
     * 특정 행을 참조하는 답변이 존재하는지 확인합니다. (INV-10: 행 삭제 전 참조 검증)
     *
     * @param rowId 행 ID
     * @return 참조 중이면 true
     */
    boolean existsBySelectedRowId(Long rowId);

    /**
     * 특정 질문을 참조하는 답변이 존재하는지 확인합니다. (INV-14: 질문 삭제 전 참조 검증)
     *
     * @param questionId 질문 ID
     * @return 참조 중이면 true
     */
    boolean existsByQuestionId(Long questionId);
}
