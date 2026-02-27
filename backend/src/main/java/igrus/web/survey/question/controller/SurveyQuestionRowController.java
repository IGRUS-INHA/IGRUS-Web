package igrus.web.survey.question.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.question.dto.request.SaveQuestionRowRequest;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.question.service.SurveyQuestionRowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 설문 그리드 행 컨트롤러.
 * 그리드 행 CRUD API를 제공합니다.
 */
@Tag(name = "Survey Question Row", description = "설문 그리드 행 API")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@Slf4j
@RestController
@RequestMapping("/api/v1/surveys/{surveyId}/questions/{questionId}/rows")
@RequiredArgsConstructor
public class SurveyQuestionRowController {

    private final SurveyQuestionRowService surveyQuestionRowService;

    @Operation(summary = "행 추가", description = "질문에 새로운 그리드 행을 추가합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "행 추가 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문 또는 질문을 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<List<SurveyDetailResponse.RowResponse>> createRow(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @Parameter(description = "질문 ID") @PathVariable Long questionId,
            @Valid @RequestBody SaveQuestionRowRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행 추가 요청 - surveyId: {}, questionId: {}, userId: {}", surveyId, questionId, user.userId());
        List<SurveyDetailResponse.RowResponse> response = surveyQuestionRowService.createRow(surveyId, questionId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "행 목록 조회", description = "질문의 그리드 행 목록을 조회합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "행 목록 조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문 또는 질문을 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<List<SurveyDetailResponse.RowResponse>> getRowList(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @Parameter(description = "질문 ID") @PathVariable Long questionId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행 목록 조회 요청 - surveyId: {}, questionId: {}, userId: {}", surveyId, questionId, user.userId());
        List<SurveyDetailResponse.RowResponse> response = surveyQuestionRowService.getRowList(surveyId, questionId, user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "행 수정", description = "그리드 행을 수정합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "행 수정 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "행을 찾을 수 없음")
    })
    @PatchMapping("/{rowId}")
    public ResponseEntity<List<SurveyDetailResponse.RowResponse>> updateRow(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @Parameter(description = "질문 ID") @PathVariable Long questionId,
            @Parameter(description = "행 ID") @PathVariable Long rowId,
            @Valid @RequestBody SaveQuestionRowRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행 수정 요청 - surveyId: {}, questionId: {}, rowId: {}, userId: {}", surveyId, questionId, rowId, user.userId());
        List<SurveyDetailResponse.RowResponse> response = surveyQuestionRowService.updateRow(surveyId, questionId, rowId, request, user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "행 삭제", description = "그리드 행을 삭제합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "행 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "행을 찾을 수 없음")
    })
    @DeleteMapping("/{rowId}")
    public ResponseEntity<Void> deleteRow(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @Parameter(description = "질문 ID") @PathVariable Long questionId,
            @Parameter(description = "행 ID") @PathVariable Long rowId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행 삭제 요청 - surveyId: {}, questionId: {}, rowId: {}, userId: {}", surveyId, questionId, rowId, user.userId());
        surveyQuestionRowService.deleteRow(surveyId, questionId, rowId, user);
        return ResponseEntity.noContent().build();
    }
}
