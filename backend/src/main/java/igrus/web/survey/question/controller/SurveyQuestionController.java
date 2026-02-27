package igrus.web.survey.question.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.question.dto.request.CreateQuestionRequest;
import igrus.web.survey.question.dto.request.UpdateQuestionRequest;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.question.service.SurveyQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
 * 설문 질문 컨트롤러.
 * 설문 질문 CRUD API를 제공합니다.
 */
@Tag(name = "Survey Question", description = "설문 질문 API")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@Slf4j
@RestController
@RequestMapping("/api/v1/surveys/{surveyId}/questions")
@RequiredArgsConstructor
public class SurveyQuestionController {

    private final SurveyQuestionService surveyQuestionService;

    @Operation(summary = "질문 추가", description = "설문에 새로운 질문을 추가합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "질문 추가 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SurveyDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문을 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<SurveyDetailResponse> createQuestion(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @Valid @RequestBody CreateQuestionRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("질문 추가 요청 - surveyId: {}, userId: {}, title: {}", surveyId, user.userId(), request.title());
        SurveyDetailResponse response = surveyQuestionService.createQuestion(surveyId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "질문 목록 조회", description = "설문의 질문 목록을 조회합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "질문 목록 조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문을 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<List<SurveyDetailResponse.QuestionResponse>> getQuestionList(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("질문 목록 조회 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        List<SurveyDetailResponse.QuestionResponse> response = surveyQuestionService.getQuestionList(surveyId, user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "질문 수정", description = "질문을 수정합니다. 모든 공개·응답 상태에서 수정 가능합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "질문 수정 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SurveyDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음")
    })
    @PatchMapping("/{questionId}")
    public ResponseEntity<SurveyDetailResponse> updateQuestion(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @Parameter(description = "질문 ID") @PathVariable Long questionId,
            @Valid @RequestBody UpdateQuestionRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("질문 수정 요청 - surveyId: {}, questionId: {}, userId: {}", surveyId, questionId, user.userId());
        SurveyDetailResponse response = surveyQuestionService.updateQuestion(surveyId, questionId, request, user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "질문 삭제", description = "질문을 삭제합니다. 모든 공개·응답 상태에서 삭제 가능합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "질문 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음")
    })
    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @Parameter(description = "질문 ID") @PathVariable Long questionId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("질문 삭제 요청 - surveyId: {}, questionId: {}, userId: {}", surveyId, questionId, user.userId());
        surveyQuestionService.deleteQuestion(surveyId, questionId, user);
        return ResponseEntity.noContent().build();
    }
}
