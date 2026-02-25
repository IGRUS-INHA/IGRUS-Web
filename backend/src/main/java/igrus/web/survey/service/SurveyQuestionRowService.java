package igrus.web.survey.service;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.domain.SurveyQuestion;
import igrus.web.survey.domain.SurveyQuestionRow;
import igrus.web.survey.dto.request.SaveQuestionRowRequest;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.exception.SurveyAccessDeniedException;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.exception.SurveyQuestionNotFoundException;
import igrus.web.survey.exception.SurveyRowNotFoundException;
import igrus.web.survey.repository.SurveyQuestionRepository;
import igrus.web.survey.repository.SurveyQuestionRowRepository;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 설문 그리드 행 서비스.
 * 그리드 행 CRUD 비즈니스 로직을 처리합니다.
 *
 * <p>제공 기능:</p>
 * <ul>
 *   <li>{@link #createRow} - 행 추가 (운영진 이상)</li>
 *   <li>{@link #updateRow} - 행 수정 (운영진 이상)</li>
 *   <li>{@link #deleteRow} - 행 삭제 (운영진 이상)</li>
 *   <li>{@link #getRowList} - 행 목록 조회 (운영진 이상)</li>
 * </ul>
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class SurveyQuestionRowService {

    private final SurveyRepository surveyRepository;
    private final SurveyQuestionRepository questionRepository;
    private final SurveyQuestionRowRepository rowRepository;
    private final UserRepository userRepository;

    /**
     * 질문에 그리드 행을 추가합니다.
     *
     * @param surveyId          설문 ID
     * @param questionId        질문 ID
     * @param request           행 저장 요청
     * @param authenticatedUser 인증된 사용자 정보
     * @return 행 목록
     */
    public List<SurveyDetailResponse.RowResponse> createRow(Long surveyId, Long questionId,
                                                             SaveQuestionRowRequest request,
                                                             AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));

        SurveyQuestion question = questionRepository.findByIdAndDeletedFalse(questionId)
                .orElseThrow(() -> new SurveyQuestionNotFoundException(questionId));
        validateQuestionBelongsToSurvey(question, surveyId);

        SurveyQuestionRow row = SurveyQuestionRow.create(
                question,
                request.label(),
                request.displayOrder()
        );

        rowRepository.save(row);
        question.getRows().add(row);

        return question.getRows().stream()
                .filter(r -> !r.isDeleted())
                .map(SurveyDetailResponse.RowResponse::from)
                .toList();
    }

    /**
     * 그리드 행을 수정합니다.
     *
     * @param surveyId          설문 ID
     * @param questionId        질문 ID
     * @param rowId             행 ID
     * @param request           행 저장 요청
     * @param authenticatedUser 인증된 사용자 정보
     * @return 행 목록
     */
    public List<SurveyDetailResponse.RowResponse> updateRow(Long surveyId, Long questionId, Long rowId,
                                                             SaveQuestionRowRequest request,
                                                             AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));

        SurveyQuestion question = questionRepository.findByIdAndDeletedFalse(questionId)
                .orElseThrow(() -> new SurveyQuestionNotFoundException(questionId));
        validateQuestionBelongsToSurvey(question, surveyId);

        SurveyQuestionRow row = rowRepository.findByIdAndDeletedFalse(rowId)
                .orElseThrow(() -> new SurveyRowNotFoundException(rowId));
        validateRowBelongsToQuestion(row, questionId);

        row.update(request.label(), request.displayOrder());

        return question.getRows().stream()
                .filter(r -> !r.isDeleted())
                .map(SurveyDetailResponse.RowResponse::from)
                .toList();
    }

    /**
     * 그리드 행을 삭제(soft delete)합니다.
     *
     * @param surveyId          설문 ID
     * @param questionId        질문 ID
     * @param rowId             행 ID
     * @param authenticatedUser 인증된 사용자 정보
     */
    public void deleteRow(Long surveyId, Long questionId, Long rowId,
                          AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));

        SurveyQuestion question = questionRepository.findByIdAndDeletedFalse(questionId)
                .orElseThrow(() -> new SurveyQuestionNotFoundException(questionId));
        validateQuestionBelongsToSurvey(question, surveyId);

        SurveyQuestionRow row = rowRepository.findByIdAndDeletedFalse(rowId)
                .orElseThrow(() -> new SurveyRowNotFoundException(rowId));
        validateRowBelongsToQuestion(row, questionId);

        row.delete(authenticatedUser.userId());
    }

    /**
     * 질문의 그리드 행 목록을 조회합니다.
     *
     * @param surveyId          설문 ID
     * @param questionId        질문 ID
     * @param authenticatedUser 인증된 사용자 정보
     * @return 행 목록
     */
    @Transactional(readOnly = true)
    public List<SurveyDetailResponse.RowResponse> getRowList(Long surveyId, Long questionId,
                                                              AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));

        SurveyQuestion question = questionRepository.findByIdAndDeletedFalse(questionId)
                .orElseThrow(() -> new SurveyQuestionNotFoundException(questionId));
        validateQuestionBelongsToSurvey(question, surveyId);

        return question.getRows().stream()
                .filter(r -> !r.isDeleted())
                .map(SurveyDetailResponse.RowResponse::from)
                .toList();
    }

    // === Private helper methods ===

    private void validateOperatorPermission(User user) {
        if (!user.isOperatorOrAbove()) {
            throw new SurveyAccessDeniedException("운영진 이상만 설문을 관리할 수 있습니다");
        }
    }

    private void validateQuestionBelongsToSurvey(SurveyQuestion question, Long surveyId) {
        if (!question.getSurvey().getId().equals(surveyId)) {
            throw new SurveyAccessDeniedException("해당 설문의 질문이 아닙니다");
        }
    }

    private void validateRowBelongsToQuestion(SurveyQuestionRow row, Long questionId) {
        if (!row.getQuestion().getId().equals(questionId)) {
            throw new SurveyAccessDeniedException("해당 질문의 행이 아닙니다");
        }
    }
}
