package igrus.web.survey.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.dto.request.SaveQuestionOptionRequest;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.service.SurveyQuestionOptionService;
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
 * 설문 질문 선택지 컨트롤러.
 * 선택지 CRUD API를 제공합니다.
 */
@Tag(name = "Survey Question Option", description = "설문 질문 선택지 API")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@Slf4j
@RestController
@RequestMapping("/api/v1/surveys/{surveyId}/questions/{questionId}/options")
@RequiredArgsConstructor
public class SurveyQuestionOptionController {

    private final SurveyQuestionOptionService surveyQuestionOptionService;

    @Operation(summary = "선택지 추가", description = "질문에 새로운 선택지를 추가합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "선택지 추가 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문 또는 질문을 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<List<SurveyDetailResponse.OptionResponse>> createOption(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @Parameter(description = "질문 ID") @PathVariable Long questionId,
            @Valid @RequestBody SaveQuestionOptionRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("선택지 추가 요청 - surveyId: {}, questionId: {}, userId: {}", surveyId, questionId, user.userId());
        List<SurveyDetailResponse.OptionResponse> response = surveyQuestionOptionService.createOption(surveyId, questionId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "선택지 목록 조회", description = "질문의 선택지 목록을 조회합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "선택지 목록 조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문 또는 질문을 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<List<SurveyDetailResponse.OptionResponse>> getOptionList(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @Parameter(description = "질문 ID") @PathVariable Long questionId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("선택지 목록 조회 요청 - surveyId: {}, questionId: {}, userId: {}", surveyId, questionId, user.userId());
        List<SurveyDetailResponse.OptionResponse> response = surveyQuestionOptionService.getOptionList(surveyId, questionId, user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "선택지 수정", description = "선택지를 수정합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "선택지 수정 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "선택지를 찾을 수 없음")
    })
    @PatchMapping("/{optionId}")
    public ResponseEntity<List<SurveyDetailResponse.OptionResponse>> updateOption(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @Parameter(description = "질문 ID") @PathVariable Long questionId,
            @Parameter(description = "선택지 ID") @PathVariable Long optionId,
            @Valid @RequestBody SaveQuestionOptionRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("선택지 수정 요청 - surveyId: {}, questionId: {}, optionId: {}, userId: {}", surveyId, questionId, optionId, user.userId());
        List<SurveyDetailResponse.OptionResponse> response = surveyQuestionOptionService.updateOption(surveyId, questionId, optionId, request, user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "선택지 삭제", description = "선택지를 삭제합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "선택지 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "선택지를 찾을 수 없음")
    })
    @DeleteMapping("/{optionId}")
    public ResponseEntity<Void> deleteOption(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @Parameter(description = "질문 ID") @PathVariable Long questionId,
            @Parameter(description = "선택지 ID") @PathVariable Long optionId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("선택지 삭제 요청 - surveyId: {}, questionId: {}, optionId: {}, userId: {}", surveyId, questionId, optionId, user.userId());
        surveyQuestionOptionService.deleteOption(surveyId, questionId, optionId, user);
        return ResponseEntity.noContent().build();
    }
}
