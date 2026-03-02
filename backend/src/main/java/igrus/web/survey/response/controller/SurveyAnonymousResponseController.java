package igrus.web.survey.response.controller;

import igrus.web.generated.api.SurveyAnonymousResponseApi;
import igrus.web.generated.model.GetMyResponse200Response;
import igrus.web.generated.model.GetMyResponse200ResponseAnswersInner;
import igrus.web.generated.model.GetMyResponse200ResponseAnswersInnerGridAnswersInner;
import igrus.web.generated.model.UpdateMyResponseRequest;
import igrus.web.survey.response.dto.request.SubmitAnswerRequest;
import igrus.web.survey.response.dto.request.SubmitSurveyResponseRequest;
import igrus.web.survey.response.dto.response.SurveyResponseDetailResponse;
import igrus.web.survey.response.service.SurveyResponseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 비회원(익명) 설문 응답 컨트롤러.
 * PUBLIC 설문에 대한 비회원 응답 API를 제공합니다.
 * 인증 없이 접근 가능합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SurveyAnonymousResponseController implements SurveyAnonymousResponseApi {

    private final SurveyResponseService surveyResponseService;

    @Override
    public ResponseEntity<GetMyResponse200Response> submitAnonymousResponse(
            Long surveyId, UpdateMyResponseRequest updateMyResponseRequest) {
        log.info("비회원 설문 응답 제출 요청 - surveyId: {}", surveyId);

        SubmitSurveyResponseRequest internalRequest = mapToInternalRequest(updateMyResponseRequest);
        SurveyResponseDetailResponse result = surveyResponseService.submitAnonymousResponse(surveyId, internalRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(result));
    }

    private SubmitSurveyResponseRequest mapToInternalRequest(UpdateMyResponseRequest request) {
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

    private GetMyResponse200Response mapToResponse(SurveyResponseDetailResponse result) {
        return new GetMyResponse200Response()
                .responseId(result.responseId())
                .surveyId(result.surveyId())
                .userId(result.userId())
                .answers(result.answers().stream()
                        .map(a -> new GetMyResponse200ResponseAnswersInner()
                                .questionId(a.questionId())
                                .questionType(GetMyResponse200ResponseAnswersInner.QuestionTypeEnum.fromValue(
                                        a.questionType().name()))
                                .textValue(a.textValue())
                                .selectedOptionIds(a.selectedOptionIds())
                                .numericValue(a.numericValue())
                                .gridAnswers(a.gridAnswers() != null
                                        ? a.gridAnswers().stream()
                                        .map(g -> new GetMyResponse200ResponseAnswersInnerGridAnswersInner()
                                                .rowId(g.rowId())
                                                .selectedOptionIds(g.selectedOptionIds()))
                                        .toList()
                                        : null))
                        .toList())
                .createdAt(result.createdAt());
    }
}
