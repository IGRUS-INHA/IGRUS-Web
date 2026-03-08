package igrus.web.survey.response.controller;

import igrus.web.common.util.SecurityUtils;
import igrus.web.generated.api.SurveyResponseApi;
import igrus.web.generated.model.ApiAnswerResponse;
import igrus.web.generated.model.ApiGridAnswerResponse;
import igrus.web.generated.model.ApiSubmitSurveyResponseRequest;
import igrus.web.generated.model.ApiSurveyResponseDetailResponse;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.response.dto.request.SubmitAnswerRequest;
import igrus.web.survey.response.dto.request.SubmitSurveyResponseRequest;
import igrus.web.survey.response.dto.response.SurveyResponseDetailResponse;
import igrus.web.survey.response.service.SurveyResponseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 설문 응답 컨트롤러.
 * 회원의 설문 응답 제출, 수정, 조회 API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SurveyResponseController implements SurveyResponseApi {

    private final SurveyResponseService surveyResponseService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiSurveyResponseDetailResponse> submitResponse(
            Long surveyId, ApiSubmitSurveyResponseRequest submitSurveyResponseRequest) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("설문 응답 제출 요청 - surveyId: {}, userId: {}", surveyId, user.userId());

        SubmitSurveyResponseRequest internalRequest = mapToInternalRequest(submitSurveyResponseRequest);
        SurveyResponseDetailResponse result = surveyResponseService.submitResponse(surveyId, internalRequest, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(result));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiSurveyResponseDetailResponse> updateMyResponse(
            Long surveyId, ApiSubmitSurveyResponseRequest submitSurveyResponseRequest) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("본인 응답 수정 요청 - surveyId: {}, userId: {}", surveyId, user.userId());

        SubmitSurveyResponseRequest internalRequest = mapToInternalRequest(submitSurveyResponseRequest);
        SurveyResponseDetailResponse result = surveyResponseService.updateMyResponse(surveyId, internalRequest, user);
        return ResponseEntity.ok(mapToResponse(result));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMyResponse(Long surveyId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("본인 응답 삭제 요청 - surveyId: {}, userId: {}", surveyId, user.userId());

        surveyResponseService.deleteMyResponse(surveyId, user);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiSurveyResponseDetailResponse> getMyResponse(Long surveyId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("본인 응답 조회 요청 - surveyId: {}, userId: {}", surveyId, user.userId());

        SurveyResponseDetailResponse result = surveyResponseService.getMyResponse(surveyId, user);
        return ResponseEntity.ok(mapToResponse(result));
    }

    private SubmitSurveyResponseRequest mapToInternalRequest(ApiSubmitSurveyResponseRequest request) {
        List<SubmitAnswerRequest> answers = request.getAnswers().stream()
                .map(a -> new SubmitAnswerRequest(
                        a.getQuestionId(),
                        a.getTextValue(),
                        a.getSelectedOptionIds(),
                        a.getNumericValue(),
                        a.getGridAnswers() != null
                                ? a.getGridAnswers().stream()
                                .map(g -> new SubmitAnswerRequest.GridAnswerRequest(
                                        g.getRowId(),
                                        g.getSelectedOptionIds()))
                                .toList()
                                : null))
                .toList();
        return new SubmitSurveyResponseRequest(answers);
    }

    private ApiSurveyResponseDetailResponse mapToResponse(SurveyResponseDetailResponse result) {
        return new ApiSurveyResponseDetailResponse()
                .responseId(result.responseId())
                .surveyId(result.surveyId())
                .userId(result.userId())
                .answers(result.answers().stream()
                        .map(a -> new ApiAnswerResponse()
                                .questionId(a.questionId())
                                .questionType(ApiAnswerResponse.QuestionTypeEnum.fromValue(
                                        a.questionType().name()))
                                .textValue(a.textValue())
                                .selectedOptionIds(a.selectedOptionIds())
                                .numericValue(a.numericValue())
                                .gridAnswers(a.gridAnswers() != null
                                        ? a.gridAnswers().stream()
                                        .map(g -> new ApiGridAnswerResponse()
                                                .rowId(g.rowId())
                                                .selectedOptionIds(g.selectedOptionIds()))
                                        .toList()
                                        : null))
                        .toList())
                .createdAt(result.createdAt());
    }
}
