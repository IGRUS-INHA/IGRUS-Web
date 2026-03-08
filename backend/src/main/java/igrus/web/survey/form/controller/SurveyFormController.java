package igrus.web.survey.form.controller;

import igrus.web.common.util.SecurityUtils;
import igrus.web.generated.api.SurveyFormApi;
import igrus.web.generated.model.ApiSurveyDetailResponse;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.dto.response.SurveyDetailResponseMapper;
import igrus.web.survey.form.service.SurveyFormService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * 설문 양식 조회 컨트롤러 (회원용).
 * 인증된 사용자가 설문 양식을 조회할 수 있는 API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SurveyFormController implements SurveyFormApi {

    private final SurveyFormService surveyFormService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiSurveyDetailResponse> getSurveyForm(Long surveyId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("설문 양식 조회 요청 - surveyId: {}, userId: {}", surveyId, user.userId());

        SurveyDetailResponse response = surveyFormService.getSurveyForm(surveyId, user);
        return ResponseEntity.ok(SurveyDetailResponseMapper.toApiResponse(response));
    }
}
