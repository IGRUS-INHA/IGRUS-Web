package igrus.web.survey.response.controller;

import igrus.web.common.util.SecurityUtils;
import igrus.web.generated.api.AdminSurveyResponseApi;
import igrus.web.generated.model.ApiAdminSurveyResponseListItem;
import igrus.web.generated.model.ApiAnswerResponse;
import igrus.web.generated.model.ApiGridAnswerResponse;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.response.dto.response.AdminSurveyResponseListItem;
import igrus.web.survey.response.dto.response.SurveyResponseDetailResponse;
import igrus.web.survey.response.service.SurveyResponseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 설문 응답 조회 컨트롤러.
 * 관리자가 특정 설문에 제출된 응답 목록을 조회하는 API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AdminSurveyResponseController implements AdminSurveyResponseApi {

    private final SurveyResponseService surveyResponseService;

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<ApiAdminSurveyResponseListItem>> getAdminSurveyResponses(Long surveyId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("관리자 설문 응답 목록 조회 요청 - surveyId: {}, userId: {}", surveyId, user.userId());

        List<AdminSurveyResponseListItem> responses = surveyResponseService.getResponsesBySurveyId(surveyId);
        return ResponseEntity.ok(responses.stream()
                .map(this::mapToApiResponse)
                .toList());
    }

    private ApiAdminSurveyResponseListItem mapToApiResponse(AdminSurveyResponseListItem item) {
        return new ApiAdminSurveyResponseListItem()
                .responseId(item.responseId())
                .registrationId(item.registrationId())
                .userId(item.userId())
                .userName(item.userName())
                .submittedAt(item.submittedAt())
                .answers(item.answers().stream()
                        .map(this::mapToApiAnswerResponse)
                        .toList());
    }

    private ApiAnswerResponse mapToApiAnswerResponse(SurveyResponseDetailResponse.AnswerResponse a) {
        return new ApiAnswerResponse()
                .questionId(a.questionId())
                .questionType(ApiAnswerResponse.QuestionTypeEnum.fromValue(a.questionType().name()))
                .textValue(a.textValue())
                .selectedOptionIds(a.selectedOptionIds())
                .numericValue(a.numericValue())
                .gridAnswers(a.gridAnswers() != null
                        ? a.gridAnswers().stream()
                        .map(g -> new ApiGridAnswerResponse()
                                .rowId(g.rowId())
                                .selectedOptionIds(g.selectedOptionIds()))
                        .toList()
                        : null);
    }
}
