package igrus.web.survey.response.controller;

import igrus.web.survey.response.dto.request.SubmitSurveyResponseRequest;
import igrus.web.survey.response.dto.response.SurveyResponseDetailResponse;
import igrus.web.survey.response.service.SurveyResponseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 비회원(익명) 설문 응답 컨트롤러.
 * PUBLIC 설문에 대한 비회원 응답 API를 제공합니다.
 * 인증 없이 접근 가능합니다.
 */
@Tag(name = "Survey Anonymous Response", description = "비회원 설문 응답 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/surveys/{surveyId}/responses/anonymous")
@RequiredArgsConstructor
public class SurveyAnonymousResponseController {

    private final SurveyResponseService surveyResponseService;

    @Operation(summary = "비회원 설문 응답 제출", description = "비회원이 PUBLIC 설문에 응답을 제출합니다. 인증 없이 접근 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "응답 제출 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SurveyResponseDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "응답 유효성 검증 실패 또는 응답 불가 상태"),
            @ApiResponse(responseCode = "403", description = "비회원 응답이 허용되지 않는 설문"),
            @ApiResponse(responseCode = "404", description = "설문을 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<SurveyResponseDetailResponse> submitAnonymousResponse(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @Valid @RequestBody SubmitSurveyResponseRequest request
    ) {
        log.info("비회원 설문 응답 제출 요청 - surveyId: {}", surveyId);
        SurveyResponseDetailResponse response = surveyResponseService.submitAnonymousResponse(surveyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
