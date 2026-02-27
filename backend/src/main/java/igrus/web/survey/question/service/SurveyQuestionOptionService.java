package igrus.web.survey.question.service;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.exception.SurveyAccessDeniedException;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.question.domain.*;
import igrus.web.survey.question.dto.request.SaveQuestionOptionRequest;
import igrus.web.survey.question.exception.SurveyOptionNotFoundException;
import igrus.web.survey.question.exception.SurveyQuestionNotFoundException;
import igrus.web.survey.question.exception.SurveyQuestionTypeNotSupportedException;
import igrus.web.survey.question.repository.SurveyQuestionOptionRepository;
import igrus.web.survey.question.repository.SurveyQuestionRepository;
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
 * 설문 질문 선택지 서비스.
 * 선택지 CRUD 비즈니스 로직을 처리합니다.
 *
 * <p>제공 기능:</p>
 * <ul>
 *   <li>{@link #createOption} - 선택지 추가 (운영진 이상)</li>
 *   <li>{@link #updateOption} - 선택지 수정 (운영진 이상)</li>
 *   <li>{@link #deleteOption} - 선택지 삭제 (운영진 이상)</li>
 *   <li>{@link #getOptionList} - 선택지 목록 조회 (운영진 이상)</li>
 * </ul>
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class SurveyQuestionOptionService {

    private final SurveyRepository surveyRepository;
    private final SurveyQuestionRepository questionRepository;
    private final SurveyQuestionOptionRepository optionRepository;
    private final UserRepository userRepository;

    /**
     * 질문에 선택지를 추가합니다.
     *
     * @param surveyId          설문 ID
     * @param questionId        질문 ID
     * @param request           선택지 저장 요청
     * @param authenticatedUser 인증된 사용자 정보
     * @return 선택지 목록
     */
    public List<SurveyDetailResponse.OptionResponse> createOption(Long surveyId, Long questionId,
                                                                   SaveQuestionOptionRequest request,
                                                                   AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));

        SurveyQuestion question = questionRepository.findByIdAndDeletedFalse(questionId)
                .orElseThrow(() -> new SurveyQuestionNotFoundException(questionId));
        validateQuestionBelongsToSurvey(question, surveyId);

        SurveyQuestionOption option = SurveyQuestionOption.create(
                question,
                request.text(),
                request.displayOrder()
        );

        optionRepository.save(option);
        List<SurveyQuestionOption> options = getOptionsFromQuestion(question);
        options.add(option);

        return options.stream()
                .filter(o -> !o.isDeleted())
                .map(SurveyDetailResponse.OptionResponse::from)
                .toList();
    }

    /**
     * 선택지를 수정합니다.
     *
     * @param surveyId          설문 ID
     * @param questionId        질문 ID
     * @param optionId          선택지 ID
     * @param request           선택지 저장 요청
     * @param authenticatedUser 인증된 사용자 정보
     * @return 선택지 목록
     */
    public List<SurveyDetailResponse.OptionResponse> updateOption(Long surveyId, Long questionId, Long optionId,
                                                                   SaveQuestionOptionRequest request,
                                                                   AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));

        SurveyQuestion question = questionRepository.findByIdAndDeletedFalse(questionId)
                .orElseThrow(() -> new SurveyQuestionNotFoundException(questionId));
        validateQuestionBelongsToSurvey(question, surveyId);

        SurveyQuestionOption option = optionRepository.findByIdAndDeletedFalse(optionId)
                .orElseThrow(() -> new SurveyOptionNotFoundException(optionId));
        validateOptionBelongsToQuestion(option, questionId);

        option.update(request.text(), request.displayOrder());

        return getOptionsFromQuestion(question).stream()
                .filter(o -> !o.isDeleted())
                .map(SurveyDetailResponse.OptionResponse::from)
                .toList();
    }

    /**
     * 선택지를 삭제(soft delete)합니다.
     *
     * @param surveyId          설문 ID
     * @param questionId        질문 ID
     * @param optionId          선택지 ID
     * @param authenticatedUser 인증된 사용자 정보
     */
    public void deleteOption(Long surveyId, Long questionId, Long optionId,
                             AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));

        SurveyQuestion question = questionRepository.findByIdAndDeletedFalse(questionId)
                .orElseThrow(() -> new SurveyQuestionNotFoundException(questionId));
        validateQuestionBelongsToSurvey(question, surveyId);

        SurveyQuestionOption option = optionRepository.findByIdAndDeletedFalse(optionId)
                .orElseThrow(() -> new SurveyOptionNotFoundException(optionId));
        validateOptionBelongsToQuestion(option, questionId);

        option.delete(authenticatedUser.userId());
    }

    /**
     * 질문의 선택지 목록을 조회합니다.
     *
     * @param surveyId          설문 ID
     * @param questionId        질문 ID
     * @param authenticatedUser 인증된 사용자 정보
     * @return 선택지 목록
     */
    @Transactional(readOnly = true)
    public List<SurveyDetailResponse.OptionResponse> getOptionList(Long surveyId, Long questionId,
                                                                    AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));
        validateOperatorPermission(user);

        surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));

        SurveyQuestion question = questionRepository.findByIdAndDeletedFalse(questionId)
                .orElseThrow(() -> new SurveyQuestionNotFoundException(questionId));
        validateQuestionBelongsToSurvey(question, surveyId);

        return getOptionsFromQuestion(question).stream()
                .filter(o -> !o.isDeleted())
                .map(SurveyDetailResponse.OptionResponse::from)
                .toList();
    }

    // === Private helper methods ===

    private List<SurveyQuestionOption> getOptionsFromQuestion(SurveyQuestion question) {
        if (question instanceof GridSurveyQuestion gridQ) {
            return gridQ.getOptions();
        } else if (question instanceof OptionSurveyQuestion optionQ) {
            return optionQ.getOptions();
        }
        throw new SurveyQuestionTypeNotSupportedException("해당 질문 유형은 선택지를 지원하지 않습니다: " + question.getQuestionType());
    }

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

    private void validateOptionBelongsToQuestion(SurveyQuestionOption option, Long questionId) {
        if (!option.getQuestion().getId().equals(questionId)) {
            throw new SurveyAccessDeniedException("해당 질문의 선택지가 아닙니다");
        }
    }
}
