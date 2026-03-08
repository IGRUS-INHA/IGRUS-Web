package igrus.web.survey.form.controller;

import igrus.web.generated.api.SurveyAnonymousFormApi;
import igrus.web.generated.model.ApiSurveyDetailResponse;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.dto.response.SurveyDetailResponseMapper;
import igrus.web.survey.form.service.SurveyFormService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * 비회원(익명) 설문 양식 조회 컨트롤러.
 * PUBLIC 설문에 대한 비회원 양식 조회 API를 제공합니다.
 * 인증 없이 접근 가능합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SurveyAnonymousFormController implements SurveyAnonymousFormApi {

    private final SurveyFormService surveyFormService;

    @Override
    public ResponseEntity<ApiSurveyDetailResponse> getAnonymousSurveyForm(Long surveyId) {
        log.info("비회원 설문 양식 조회 요청 - surveyId: {}", surveyId);

        SurveyDetailResponse response = surveyFormService.getAnonymousSurveyForm(surveyId);
        return ResponseEntity.ok(SurveyDetailResponseMapper.toApiResponse(response));
    }
}
