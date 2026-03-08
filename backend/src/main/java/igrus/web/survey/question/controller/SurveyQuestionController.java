package igrus.web.survey.question.controller;

import igrus.web.common.util.EnumUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.generated.api.SurveyQuestionApi;
import igrus.web.generated.model.ApiCreateQuestionRequest;
import igrus.web.generated.model.ApiQuestionResponse;
import igrus.web.generated.model.ApiSurveyDetailResponse;
import igrus.web.generated.model.ApiUpdateQuestionRequest;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.dto.response.SurveyDetailResponseMapper;
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
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiSurveyDetailResponse> createQuestion(
            Long surveyId,
            ApiCreateQuestionRequest createQuestionRequest
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
        return ResponseEntity.status(HttpStatus.CREATED).body(SurveyDetailResponseMapper.toApiResponse(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<ApiQuestionResponse>> getQuestionList(Long surveyId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("질문 목록 조회 요청 - surveyId: {}, userId: {}", surveyId, user.userId());

        List<SurveyDetailResponse.QuestionResponse> response = surveyQuestionService.getQuestionList(surveyId, user);
        return ResponseEntity.ok(response.stream()
                .map(SurveyDetailResponseMapper::toQuestionInner)
                .toList());
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiSurveyDetailResponse> updateQuestion(
            Long surveyId,
            Long questionId,
            ApiUpdateQuestionRequest updateQuestionRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("질문 수정 요청 - surveyId: {}, questionId: {}, userId: {}", surveyId, questionId, user.userId());

        UpdateQuestionRequest request = new UpdateQuestionRequest(
                EnumUtils.fromStringOrNull(SurveyQuestionType.class, updateQuestionRequest.getQuestionType().name()),
                updateQuestionRequest.getTitle(),
                updateQuestionRequest.getDescription(),
                Boolean.TRUE.equals(updateQuestionRequest.getRequired()),
                updateQuestionRequest.getDisplayOrder() != null ? updateQuestionRequest.getDisplayOrder() : 0
        );

        SurveyDetailResponse response = surveyQuestionService.updateQuestion(surveyId, questionId, request, user);
        return ResponseEntity.ok(SurveyDetailResponseMapper.toApiResponse(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> deleteQuestion(Long surveyId, Long questionId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("질문 삭제 요청 - surveyId: {}, questionId: {}, userId: {}", surveyId, questionId, user.userId());

        surveyQuestionService.deleteQuestion(surveyId, questionId, user);
        return ResponseEntity.noContent().build();
    }

}
