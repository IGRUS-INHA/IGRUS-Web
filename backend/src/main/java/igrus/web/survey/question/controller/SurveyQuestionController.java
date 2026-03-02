package igrus.web.survey.question.controller;

import igrus.web.common.util.EnumUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.generated.api.SurveyQuestionApi;
import igrus.web.generated.model.GetSurveyDetail200Response;
import igrus.web.generated.model.GetSurveyDetail200ResponseQuestionsInner;
import igrus.web.generated.model.GetSurveyDetail200ResponseQuestionsInnerOptionsInner;
import igrus.web.generated.model.GetSurveyDetail200ResponseQuestionsInnerRowsInner;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.question.domain.SurveyQuestionType;
import igrus.web.survey.question.dto.request.CreateQuestionRequest;
import igrus.web.survey.question.dto.request.UpdateQuestionRequest;
import igrus.web.survey.question.service.SurveyQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 설문 질문 컨트롤러.
 * 설문 질문 CRUD API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SurveyQuestionController implements SurveyQuestionApi {

    private final SurveyQuestionService surveyQuestionService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GetSurveyDetail200Response> createQuestion(
            Long surveyId,
            igrus.web.generated.model.CreateQuestionRequest createQuestionRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("질문 추가 요청 - surveyId: {}, userId: {}, title: {}", surveyId, user.userId(), createQuestionRequest.getTitle());

        CreateQuestionRequest request = new CreateQuestionRequest(
                EnumUtils.fromStringOrNull(SurveyQuestionType.class, createQuestionRequest.getQuestionType().name()),
                createQuestionRequest.getTitle(),
                createQuestionRequest.getDescription(),
                Boolean.TRUE.equals(createQuestionRequest.getRequired()),
                createQuestionRequest.getDisplayOrder() != null ? createQuestionRequest.getDisplayOrder() : 0
        );

        SurveyDetailResponse response = surveyQuestionService.createQuestion(surveyId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToSurveyDetailResponse(response));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GetSurveyDetail200ResponseQuestionsInner>> getQuestionList(Long surveyId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("질문 목록 조회 요청 - surveyId: {}, userId: {}", surveyId, user.userId());

        List<SurveyDetailResponse.QuestionResponse> response = surveyQuestionService.getQuestionList(surveyId, user);
        return ResponseEntity.ok(response.stream()
                .map(this::mapToQuestionInner)
                .toList());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GetSurveyDetail200Response> updateQuestion(
            Long surveyId,
            Long questionId,
            igrus.web.generated.model.CreateQuestionRequest createQuestionRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("질문 수정 요청 - surveyId: {}, questionId: {}, userId: {}", surveyId, questionId, user.userId());

        UpdateQuestionRequest request = new UpdateQuestionRequest(
                EnumUtils.fromStringOrNull(SurveyQuestionType.class, createQuestionRequest.getQuestionType().name()),
                createQuestionRequest.getTitle(),
                createQuestionRequest.getDescription(),
                Boolean.TRUE.equals(createQuestionRequest.getRequired()),
                createQuestionRequest.getDisplayOrder() != null ? createQuestionRequest.getDisplayOrder() : 0
        );

        SurveyDetailResponse response = surveyQuestionService.updateQuestion(surveyId, questionId, request, user);
        return ResponseEntity.ok(mapToSurveyDetailResponse(response));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteQuestion(Long surveyId, Long questionId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("질문 삭제 요청 - surveyId: {}, questionId: {}, userId: {}", surveyId, questionId, user.userId());

        surveyQuestionService.deleteQuestion(surveyId, questionId, user);
        return ResponseEntity.noContent().build();
    }

    // === Private helper methods ===

    private GetSurveyDetail200Response mapToSurveyDetailResponse(SurveyDetailResponse response) {
        return new GetSurveyDetail200Response()
                .id(response.id())
                .title(response.title())
                .description(response.description())
                .visibility(response.visibility() != null
                        ? GetSurveyDetail200Response.VisibilityEnum.fromValue(response.visibility().name()) : null)
                .responseStatus(response.responseStatus() != null
                        ? GetSurveyDetail200Response.ResponseStatusEnum.fromValue(response.responseStatus().name()) : null)
                .accessLevel(response.accessLevel() != null
                        ? GetSurveyDetail200Response.AccessLevelEnum.fromValue(response.accessLevel().name()) : null)
                .deadline(response.deadline())
                .createdAt(response.createdAt())
                .updatedAt(response.updatedAt())
                .questions(response.questions() != null
                        ? response.questions().stream()
                                .map(this::mapToQuestionInner)
                                .toList()
                        : List.of());
    }

    private GetSurveyDetail200ResponseQuestionsInner mapToQuestionInner(SurveyDetailResponse.QuestionResponse q) {
        return new GetSurveyDetail200ResponseQuestionsInner()
                .id(q.id())
                .questionType(q.questionType() != null
                        ? GetSurveyDetail200ResponseQuestionsInner.QuestionTypeEnum.fromValue(q.questionType().name()) : null)
                .title(q.title())
                .description(q.description())
                .required(q.required())
                .displayOrder(q.displayOrder())
                .scaleMin(q.scaleMin())
                .scaleMax(q.scaleMax())
                .options(q.options() != null
                        ? q.options().stream()
                                .map(this::mapToOptionInner)
                                .toList()
                        : List.of())
                .rows(q.rows() != null
                        ? q.rows().stream()
                                .map(this::mapToRowInner)
                                .toList()
                        : List.of());
    }

    private GetSurveyDetail200ResponseQuestionsInnerOptionsInner mapToOptionInner(SurveyDetailResponse.OptionResponse o) {
        return new GetSurveyDetail200ResponseQuestionsInnerOptionsInner()
                .id(o.id())
                .text(o.text())
                .displayOrder(o.displayOrder());
    }

    private GetSurveyDetail200ResponseQuestionsInnerRowsInner mapToRowInner(SurveyDetailResponse.RowResponse r) {
        return new GetSurveyDetail200ResponseQuestionsInnerRowsInner()
                .id(r.id())
                .label(r.label())
                .displayOrder(r.displayOrder());
    }
}
