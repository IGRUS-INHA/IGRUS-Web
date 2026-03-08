package igrus.web.survey.question.controller;

import igrus.web.common.util.SecurityUtils;
import igrus.web.generated.api.SurveyQuestionRowApi;
import igrus.web.generated.model.ApiRowResponse;
import igrus.web.generated.model.ApiSaveQuestionRowRequest;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.question.dto.request.SaveQuestionRowRequest;
import igrus.web.survey.question.service.SurveyQuestionRowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 설문 그리드 행 컨트롤러.
 * 그리드 행 CRUD API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SurveyQuestionRowController implements SurveyQuestionRowApi {

    private final SurveyQuestionRowService surveyQuestionRowService;

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<ApiRowResponse>> createRow(
            Long surveyId,
            Long questionId,
            ApiSaveQuestionRowRequest createRowRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행 추가 요청 - surveyId: {}, questionId: {}, userId: {}", surveyId, questionId, user.userId());

        SaveQuestionRowRequest request = new SaveQuestionRowRequest(
                createRowRequest.getLabel(),
                createRowRequest.getDisplayOrder() != null ? createRowRequest.getDisplayOrder() : 0
        );

        List<SurveyDetailResponse.RowResponse> response = surveyQuestionRowService.createRow(surveyId, questionId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToRowList(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<ApiRowResponse>> getRowList(
            Long surveyId,
            Long questionId
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행 목록 조회 요청 - surveyId: {}, questionId: {}, userId: {}", surveyId, questionId, user.userId());

        List<SurveyDetailResponse.RowResponse> response = surveyQuestionRowService.getRowList(surveyId, questionId, user);
        return ResponseEntity.ok(mapToRowList(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<ApiRowResponse>> updateRow(
            Long surveyId,
            Long questionId,
            Long rowId,
            ApiSaveQuestionRowRequest createRowRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행 수정 요청 - surveyId: {}, questionId: {}, rowId: {}, userId: {}", surveyId, questionId, rowId, user.userId());

        SaveQuestionRowRequest request = new SaveQuestionRowRequest(
                createRowRequest.getLabel(),
                createRowRequest.getDisplayOrder() != null ? createRowRequest.getDisplayOrder() : 0
        );

        List<SurveyDetailResponse.RowResponse> response = surveyQuestionRowService.updateRow(surveyId, questionId, rowId, request, user);
        return ResponseEntity.ok(mapToRowList(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> deleteRow(Long surveyId, Long questionId, Long rowId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행 삭제 요청 - surveyId: {}, questionId: {}, rowId: {}, userId: {}", surveyId, questionId, rowId, user.userId());

        surveyQuestionRowService.deleteRow(surveyId, questionId, rowId, user);
        return ResponseEntity.noContent().build();
    }

    // === Private helper methods ===

    private List<ApiRowResponse> mapToRowList(
            List<SurveyDetailResponse.RowResponse> rows
    ) {
        return rows.stream()
                .map(r -> new ApiRowResponse()
                        .id(r.id())
                        .label(r.label())
                        .displayOrder(r.displayOrder()))
                .toList();
    }
}
