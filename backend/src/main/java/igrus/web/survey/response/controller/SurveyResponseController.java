package igrus.web.survey.response.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.response.dto.request.SubmitSurveyResponseRequest;
import igrus.web.survey.response.dto.response.SurveyResponseDetailResponse;
import igrus.web.survey.response.service.SurveyResponseService;
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

/**
 * 설문 응답 컨트롤러.
 * 회원의 설문 응답 제출, 수정, 조회 API를 제공합니다.
 */
@Tag(name = "Survey Response", description = "설문 응답 API")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@Slf4j
@RestController
@RequestMapping("/api/v1/surveys/{surveyId}/responses")
@RequiredArgsConstructor
public class SurveyResponseController {

    private final SurveyResponseService surveyResponseService;

    @Operation(summary = "설문 응답 제출", description = "회원이 설문에 응답을 제출합니다. 설문당 1회만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "응답 제출 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SurveyResponseDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "응답 유효성 검증 실패 또는 응답 불가 상태"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "응답 권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 응답한 설문")
    })
    @PostMapping
    public ResponseEntity<SurveyResponseDetailResponse> submitResponse(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @Valid @RequestBody SubmitSurveyResponseRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("설문 응답 제출 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        SurveyResponseDetailResponse response = surveyResponseService.submitResponse(surveyId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "본인 응답 수정", description = "본인이 제출한 응답을 수정합니다. 설문이 OPEN 상태에서만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "응답 수정 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SurveyResponseDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "응답 유효성 검증 실패 또는 응답 불가 상태"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "설문 또는 응답을 찾을 수 없음")
    })
    @PutMapping("/me")
    public ResponseEntity<SurveyResponseDetailResponse> updateMyResponse(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @Valid @RequestBody SubmitSurveyResponseRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("본인 응답 수정 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        SurveyResponseDetailResponse response = surveyResponseService.updateMyResponse(surveyId, request, user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "본인 응답 조회", description = "본인이 제출한 응답을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "응답 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SurveyResponseDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "설문 또는 응답을 찾을 수 없음")
    })
    @GetMapping("/me")
    public ResponseEntity<SurveyResponseDetailResponse> getMyResponse(
            @Parameter(description = "설문 ID") @PathVariable Long surveyId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("본인 응답 조회 요청 - surveyId: {}, userId: {}", surveyId, user.userId());
        SurveyResponseDetailResponse response = surveyResponseService.getMyResponse(surveyId, user);
        return ResponseEntity.ok(response);
    }
}
