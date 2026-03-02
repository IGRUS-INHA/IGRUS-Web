package igrus.web.survey.statistics.controller;

import igrus.web.common.util.SecurityUtils;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.statistics.dto.response.SurveyStatisticsResponse;
import igrus.web.survey.statistics.service.SurveyStatisticsService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 설문 통계 컨트롤러.
 * 설문 응답 데이터의 통계 조회 API를 제공합니다.
 *
 * <p>이 컨트롤러는 아직 OpenAPI 스펙에 정의되지 않아 Contract-First 마이그레이션 대상이 아니다.
 * 추후 OpenAPI 스펙에 추가되면 생성된 인터페이스를 implements하도록 전환할 것.</p>
 */
@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
@Validated
public class SurveyStatisticsController {

    private final SurveyStatisticsService surveyStatisticsService;

    @GetMapping("/{surveyId}/statistics")
    public ResponseEntity<SurveyStatisticsResponse> getSurveyStatistics(
            @PathVariable @Positive Long surveyId
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        SurveyStatisticsResponse response = surveyStatisticsService.getSurveyStatistics(surveyId, user.userId());
        return ResponseEntity.ok(response);
    }
}
