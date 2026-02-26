package igrus.web.survey.service;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.domain.*;
import igrus.web.survey.dto.request.SubmitAnswerRequest;
import igrus.web.survey.dto.request.SubmitAnswerRequest.GridAnswerRequest;
import igrus.web.survey.dto.request.SubmitSurveyResponseRequest;
import igrus.web.survey.dto.response.SurveyResponseDetailResponse;
import igrus.web.survey.exception.*;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.survey.repository.SurveyResponseRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 설문 응답 서비스.
 * 회원/비회원 응답 제출, 본인 응답 수정/조회 비즈니스 로직을 처리합니다.
 *
 * <p>제공 기능:</p>
 * <ul>
 *   <li>{@link #submitResponse} - 회원 응답 제출 (인증 필요, 중복 방지)</li>
 *   <li>{@link #submitAnonymousResponse} - 비회원 응답 제출 (PUBLIC 설문만)</li>
 *   <li>{@link #updateResponse} - 본인 응답 수정 (OPEN 중만, 전체 교체)</li>
 *   <li>{@link #getMyResponse} - 본인 응답 조회 (accessLevel 변경 후에도 조회 가능)</li>
 * </ul>
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class SurveyResponseService {

    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final UserRepository userRepository;
    private final SurveyAnswerValidator answerValidator;

    /**
     * 회원 응답을 제출합니다.
     *
     * @param surveyId          설문 ID
     * @param request           응답 제출 요청
     * @param authenticatedUser 인증된 사용자 정보
     * @return 제출된 응답 상세
     */
    public SurveyResponseDetailResponse submitResponse(Long surveyId,
                                                        SubmitSurveyResponseRequest request,
                                                        AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));

        Survey survey = findActiveSurvey(surveyId);

        // 응답 수집 중인지 검증 (INV-09)
        if (!survey.isAcceptingResponses()) {
            throw new SurveyNotAcceptingResponsesException();
        }

        // accessLevel vs user.role 검증
        validateAccessLevel(survey, user);

        // 중복 응답 검증 (INV-01)
        if (surveyResponseRepository.existsBySurveyIdAndUserIdAndDeletedFalse(surveyId, user.getId())) {
            throw new SurveyResponseDuplicateException();
        }

        // 답변 유효성 검증 (INV-12)
        List<SurveyQuestion> activeQuestions = getActiveQuestions(survey);
        answerValidator.validate(activeQuestions, request.answers());

        // 응답 생성 및 저장
        SurveyResponse response = SurveyResponse.create(survey, user);
        createAnswers(response, activeQuestions, request.answers());

        try {
            SurveyResponse savedResponse = surveyResponseRepository.save(response);
            log.info("설문 응답 제출 - surveyId: {}, userId: {}", surveyId, user.getId());
            return SurveyResponseDetailResponse.from(savedResponse);
        } catch (DataIntegrityViolationException e) {
            // 동시성 방어: unique constraint 위반 시 중복 응답으로 처리
            throw new SurveyResponseDuplicateException();
        }
    }

    /**
     * 비회원 응답을 제출합니다. PUBLIC 설문에서만 가능합니다.
     *
     * @param surveyId 설문 ID
     * @param request  응답 제출 요청
     * @return 제출된 응답 상세
     */
    public SurveyResponseDetailResponse submitAnonymousResponse(Long surveyId,
                                                                 SubmitSurveyResponseRequest request) {
        Survey survey = findActiveSurvey(surveyId);

        // 응답 수집 중인지 검증
        if (!survey.isAcceptingResponses()) {
            throw new SurveyNotAcceptingResponsesException();
        }

        // PUBLIC 설문만 비회원 응답 허용
        if (survey.getAccessLevel() != SurveyAccessLevel.PUBLIC) {
            throw new SurveyAnonymousNotAllowedException();
        }

        // 답변 유효성 검증
        List<SurveyQuestion> activeQuestions = getActiveQuestions(survey);
        answerValidator.validate(activeQuestions, request.answers());

        // 응답 생성 및 저장
        SurveyResponse response = SurveyResponse.createAnonymous(survey);
        createAnswers(response, activeQuestions, request.answers());

        SurveyResponse savedResponse = surveyResponseRepository.save(response);
        log.info("비회원 설문 응답 제출 - surveyId: {}", surveyId);
        return SurveyResponseDetailResponse.from(savedResponse);
    }

    /**
     * 본인 응답을 수정합니다. OPEN 상태에서만 가능합니다. (전체 교체 방식)
     *
     * @param surveyId          설문 ID
     * @param request           응답 수정 요청
     * @param authenticatedUser 인증된 사용자 정보
     * @return 수정된 응답 상세
     */
    public SurveyResponseDetailResponse updateResponse(Long surveyId,
                                                        SubmitSurveyResponseRequest request,
                                                        AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));

        Survey survey = findActiveSurvey(surveyId);

        // CLOSED이면 수정 불가
        if (!survey.isAcceptingResponses()) {
            throw new SurveyNotAcceptingResponsesException();
        }

        // 기존 응답 조회
        SurveyResponse response = surveyResponseRepository
                .findBySurveyIdAndUserIdAndDeletedFalse(surveyId, user.getId())
                .orElseThrow(SurveyResponseNotFoundException::new);

        // 답변 유효성 검증
        List<SurveyQuestion> activeQuestions = getActiveQuestions(survey);
        answerValidator.validate(activeQuestions, request.answers());

        // 기존 답변 전체 삭제 후 새 답변 생성 (orphanRemoval)
        response.getAnswers().clear();
        createAnswers(response, activeQuestions, request.answers());

        log.info("설문 응답 수정 - surveyId: {}, userId: {}", surveyId, user.getId());
        return SurveyResponseDetailResponse.from(response);
    }

    /**
     * 본인 응답을 조회합니다. accessLevel이 변경되어도 조회 가능합니다. (INV-19)
     *
     * @param surveyId          설문 ID
     * @param authenticatedUser 인증된 사용자 정보
     * @return 응답 상세
     */
    @Transactional(readOnly = true)
    public SurveyResponseDetailResponse getMyResponse(Long surveyId, AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.userId()));

        // accessLevel 검증 생략 (INV-19: 응답 후 권한 변경되어도 조회 가능)
        SurveyResponse response = surveyResponseRepository
                .findBySurveyIdAndUserIdAndDeletedFalse(surveyId, user.getId())
                .orElseThrow(SurveyResponseNotFoundException::new);

        return SurveyResponseDetailResponse.from(response);
    }

    // === Private helper methods ===

    private Survey findActiveSurvey(Long surveyId) {
        return surveyRepository.findByIdAndDeletedFalseAndTrashedAtIsNull(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException(surveyId));
    }

    private List<SurveyQuestion> getActiveQuestions(Survey survey) {
        return survey.getQuestions().stream()
                .filter(q -> !q.isDeleted())
                .toList();
    }

    /**
     * accessLevel에 따라 사용자의 응답 권한을 검증합니다.
     * <ul>
     *   <li>PUBLIC - 모든 인증 사용자 허용</li>
     *   <li>ASSOCIATE - 인증 사용자이면 허용 (준회원 이상)</li>
     *   <li>MEMBER - 정회원(MEMBER) 이상만 허용</li>
     *   <li>OPERATOR - 운영진(OPERATOR) 이상만 허용</li>
     * </ul>
     */
    private void validateAccessLevel(Survey survey, User user) {
        switch (survey.getAccessLevel()) {
            case PUBLIC -> { /* 모든 인증 사용자 허용 */ }
            case ASSOCIATE -> { /* 인증 사용자이면 허용 (ASSOCIATE 이상) */ }
            case MEMBER -> {
                if (!user.isMemberOrAbove()) {
                    throw new SurveyResponseAccessDeniedException(
                            "정회원 이상만 응답할 수 있는 설문입니다");
                }
            }
            case OPERATOR -> {
                if (!user.isOperatorOrAbove()) {
                    throw new SurveyResponseAccessDeniedException(
                            "운영진 이상만 응답할 수 있는 설문입니다");
                }
            }
        }
    }

    /**
     * 답변 요청 목록으로부터 SurveyAnswer 엔티티를 생성하여 응답에 추가합니다.
     */
    private void createAnswers(SurveyResponse response, List<SurveyQuestion> activeQuestions,
                                List<SubmitAnswerRequest> answerRequests) {
        Map<Long, SurveyQuestion> questionMap = activeQuestions.stream()
                .collect(Collectors.toMap(SurveyQuestion::getId, Function.identity()));

        for (SubmitAnswerRequest answerRequest : answerRequests) {
            SurveyQuestion question = questionMap.get(answerRequest.questionId());
            createAnswerForQuestion(response, question, answerRequest);
        }
    }

    /**
     * 질문 유형에 따라 SurveyAnswer 엔티티를 생성합니다.
     */
    private void createAnswerForQuestion(SurveyResponse response, SurveyQuestion question,
                                          SubmitAnswerRequest answerRequest) {
        if (question instanceof GridSurveyQuestion gridQ) {
            createGridAnswers(response, question, gridQ, answerRequest);
        } else if (question instanceof OptionSurveyQuestion optionQ) {
            createOptionAnswers(response, question, optionQ, answerRequest);
        } else if (question instanceof LinearScaleSurveyQuestion) {
            if (answerRequest.numericValue() != null) {
                NumericSurveyAnswer answer = NumericSurveyAnswer.create(response, question, answerRequest.numericValue());
                response.addAnswer(answer);
            }
        } else {
            // TextSurveyQuestion
            if (answerRequest.textValue() != null && !answerRequest.textValue().isBlank()) {
                TextSurveyAnswer answer = TextSurveyAnswer.create(response, question, answerRequest.textValue());
                response.addAnswer(answer);
            }
        }
    }

    private void createOptionAnswers(SurveyResponse response, SurveyQuestion question,
                                      OptionSurveyQuestion optionQ, SubmitAnswerRequest answerRequest) {
        Map<Long, SurveyQuestionOption> optionMap = optionQ.getOptions().stream()
                .filter(o -> !o.isDeleted())
                .collect(Collectors.toMap(SurveyQuestionOption::getId, Function.identity()));

        switch (question.getQuestionType()) {
            case MULTIPLE_CHOICE, DROPDOWN -> {
                if (answerRequest.selectedOptionId() != null) {
                    SurveyQuestionOption option = optionMap.get(answerRequest.selectedOptionId());
                    OptionSurveyAnswer answer = OptionSurveyAnswer.create(response, question, option);
                    response.addAnswer(answer);
                }
            }
            case CHECKBOX -> {
                if (answerRequest.selectedOptionIds() != null) {
                    for (Long optionId : answerRequest.selectedOptionIds()) {
                        SurveyQuestionOption option = optionMap.get(optionId);
                        OptionSurveyAnswer answer = OptionSurveyAnswer.create(response, question, option);
                        response.addAnswer(answer);
                    }
                }
            }
            default -> { }
        }
    }

    private void createGridAnswers(SurveyResponse response, SurveyQuestion question,
                                    GridSurveyQuestion gridQ, SubmitAnswerRequest answerRequest) {
        Map<Long, SurveyQuestionOption> optionMap = gridQ.getOptions().stream()
                .filter(o -> !o.isDeleted())
                .collect(Collectors.toMap(SurveyQuestionOption::getId, Function.identity()));
        Map<Long, SurveyQuestionRow> rowMap = gridQ.getRows().stream()
                .filter(r -> !r.isDeleted())
                .collect(Collectors.toMap(SurveyQuestionRow::getId, Function.identity()));

        if (answerRequest.gridAnswers() != null) {
            for (GridAnswerRequest gridAnswer : answerRequest.gridAnswers()) {
                SurveyQuestionRow row = rowMap.get(gridAnswer.rowId());
                for (Long optionId : gridAnswer.selectedOptionIds()) {
                    SurveyQuestionOption option = optionMap.get(optionId);
                    GridSurveyAnswer answer = GridSurveyAnswer.create(response, question, row, option);
                    response.addAnswer(answer);
                }
            }
        }
    }
}
