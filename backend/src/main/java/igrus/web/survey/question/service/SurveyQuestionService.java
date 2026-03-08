package igrus.web.survey.question.service;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.exception.SurveyAccessDeniedException;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.question.domain.*;
import igrus.web.survey.question.dto.request.CreateQuestionRequest;
import igrus.web.survey.question.dto.request.UpdateQuestionRequest;
import igrus.web.survey.question.exception.SurveyQuestionLimitExceededException;
import igrus.web.survey.question.exception.SurveyQuestionNotFoundException;
import igrus.web.survey.question.exception.SurveyQuestionValidationException;
import igrus.web.survey.question.repository.SurveyQuestionRepository;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 설문 질문 서비스.
 * 질문 CRUD 비즈니스 로직을 처리합니다.
 *
 * <p>제공 기능:</p>
 * <ul>
 *   <li>{@link #createQuestion} - 질문 추가 (운영진 이상)</li>
 *   <li>{@link #updateQuestion} - 질문 수정 (운영진 이상, 모든 상태)</li>
 *   <li>{@link #deleteQuestion} - 질문 삭제 (운영진 이상, 모든 상태)</li>
 *   <li>{@link #getQuestionList} - 질문 목록 조회 (운영진 이상)</li>
 * </ul>
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class SurveyQuestionService {

    private final SurveyRepository surveyRepository;
    private final SurveyQuestionRepository questionRepository;
    private final UserRepository userRepository;

    private static final int MAX_QUESTIONS = 50;

    /**
     * 설문에 질문을 추가합니다.
     *
     * @param surveyId          설문 ID
     * @param request           질문 생성 요청
     * @param authenticatedUser 인증된 사용자 정보
     * @return 설문 상세 응답 (질문 포함)
     */
    public SurveyDetailResponse createQuestion(Long surveyId, CreateQuestionRequest request,
                                                AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        Survey survey = surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        survey.updateStatusIfNeeded(Instant.now());

        long count = questionRepository.countBySurveyIdAndDeletedFalse(surveyId);
        if (count >= MAX_QUESTIONS) {
            throw new SurveyQuestionLimitExceededException();
        }

        SurveyQuestion question = createQuestionByType(
                survey,
                request.questionType(),
                request.title(),
                request.description(),
                request.required(),
                request.displayOrder()
        );

        survey.getQuestions().add(question);
        questionRepository.save(question);

        return SurveyDetailResponse.from(survey, 0);
    }

    /**
     * 질문을 수정합니다. 모든 상태에서 수정 가능합니다.
     *
     * @param surveyId          설문 ID
     * @param questionId        질문 ID
     * @param request           질문 수정 요청
     * @param authenticatedUser 인증된 사용자 정보
     * @return 설문 상세 응답 (질문 포함)
     */
    public SurveyDetailResponse updateQuestion(Long surveyId, Long questionId,
                                                UpdateQuestionRequest request,
                                                AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        Survey survey = surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
        survey.updateStatusIfNeeded(Instant.now());

        SurveyQuestion question = questionRepository.findByIdAndDeletedFalse(questionId)
                .orElseThrow(() -> new SurveyQuestionNotFoundException(questionId));
        validateQuestionBelongsToSurvey(question, surveyId);

        question.update(
                request.questionType(),
                request.title(),
                request.description(),
                request.required(),
                request.displayOrder()
        );

        return SurveyDetailResponse.from(survey, 0);
    }

    /**
     * 질문을 삭제(soft delete)합니다. 모든 상태에서 삭제 가능합니다.
     *
     * @param surveyId          설문 ID
     * @param questionId        질문 ID
     * @param authenticatedUser 인증된 사용자 정보
     */
    public void deleteQuestion(Long surveyId, Long questionId, AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));

        SurveyQuestion question = questionRepository.findByIdAndDeletedFalse(questionId)
                .orElseThrow(() -> new SurveyQuestionNotFoundException(questionId));
        validateQuestionBelongsToSurvey(question, surveyId);

        question.delete(authenticatedUser.userId());
    }

    /**
     * 설문의 질문 목록을 조회합니다.
     *
     * @param surveyId          설문 ID
     * @param authenticatedUser 인증된 사용자 정보
     * @return 질문 목록
     */
    @Transactional(readOnly = true)
    public List<SurveyDetailResponse.QuestionResponse> getQuestionList(Long surveyId,
                                                                       AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        Survey survey = surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));

        return survey.getQuestions().stream()
                .filter(q -> !q.isDeleted())
                .map(SurveyDetailResponse.QuestionResponse::from)
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

    private SurveyQuestion createQuestionByType(Survey survey, SurveyQuestionType questionType,
                                                 String title, String description,
                                                 boolean required, int displayOrder) {
        return switch (questionType.getCategory()) {
            case "TEXT" -> TextSurveyQuestion.create(survey, questionType, title, description, required, displayOrder);
            case "SCALE" -> LinearScaleSurveyQuestion.create(survey, questionType, title, description, required, displayOrder);
            case "OPTION" -> OptionSurveyQuestion.create(survey, questionType, title, description, required, displayOrder);
            case "GRID" -> GridSurveyQuestion.create(survey, questionType, title, description, required, displayOrder);
            default -> throw new SurveyQuestionValidationException("알 수 없는 질문 카테고리: " + questionType.getCategory());
        };
    }
}
