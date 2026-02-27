package igrus.web.survey.statistics.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.survey.statistics.dto.response.SurveyStatisticsResponse;
import igrus.web.survey.statistics.service.SurveyStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 설문 통계 컨트롤러.
 * 설문 응답 데이터의 통계 조회 API를 제공합니다.
 */
@Tag(name = "Survey Statistics", description = "설문 통계 API")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
@Validated
public class SurveyStatisticsController {

    private final SurveyStatisticsService surveyStatisticsService;

    @Operation(summary = "설문 통계 조회", description = "설문의 응답 통계를 조회합니다. 질문 타입별 통계(텍스트, 척도, 선택, 그리드)를 포함합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "통계 조회 성공",
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (음수 ID 등)"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "설문을 찾을 수 없음")
    })
    @GetMapping("/{surveyId}/statistics")
    public ResponseEntity<SurveyStatisticsResponse> getSurveyStatistics(
            @Parameter(description = "설문 ID") @PathVariable @Positive Long surveyId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        SurveyStatisticsResponse response = surveyStatisticsService.getSurveyStatistics(surveyId, user.userId());
        return ResponseEntity.ok(response);
    }
}
