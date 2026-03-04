package igrus.web.survey.question.controller;

import igrus.web.common.util.SecurityUtils;
import igrus.web.generated.api.SurveyQuestionOptionApi;
import igrus.web.generated.model.GetSurveyDetail200ResponseQuestionsInnerOptionsInner;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.dto.response.SurveyDetailResponse;
import igrus.web.survey.question.dto.request.SaveQuestionOptionRequest;
import igrus.web.survey.question.service.SurveyQuestionOptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 설문 질문 선택지 컨트롤러.
 * 선택지 CRUD API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SurveyQuestionOptionController implements SurveyQuestionOptionApi {

    private final SurveyQuestionOptionService surveyQuestionOptionService;

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<GetSurveyDetail200ResponseQuestionsInnerOptionsInner>> createOption(
            Long surveyId,
            Long questionId,
            igrus.web.generated.model.CreateOptionRequest createOptionRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("선택지 추가 요청 - surveyId: {}, questionId: {}, userId: {}", surveyId, questionId, user.userId());

        SaveQuestionOptionRequest request = new SaveQuestionOptionRequest(
                createOptionRequest.getText(),
                createOptionRequest.getDisplayOrder() != null ? createOptionRequest.getDisplayOrder() : 0
        );

        List<SurveyDetailResponse.OptionResponse> response = surveyQuestionOptionService.createOption(surveyId, questionId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToOptionList(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<GetSurveyDetail200ResponseQuestionsInnerOptionsInner>> getOptionList(
            Long surveyId,
            Long questionId
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("선택지 목록 조회 요청 - surveyId: {}, questionId: {}, userId: {}", surveyId, questionId, user.userId());

        List<SurveyDetailResponse.OptionResponse> response = surveyQuestionOptionService.getOptionList(surveyId, questionId, user);
        return ResponseEntity.ok(mapToOptionList(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<GetSurveyDetail200ResponseQuestionsInnerOptionsInner>> updateOption(
            Long surveyId,
            Long questionId,
            Long optionId,
            igrus.web.generated.model.CreateOptionRequest createOptionRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("선택지 수정 요청 - surveyId: {}, questionId: {}, optionId: {}, userId: {}", surveyId, questionId, optionId, user.userId());

        SaveQuestionOptionRequest request = new SaveQuestionOptionRequest(
                createOptionRequest.getText(),
                createOptionRequest.getDisplayOrder() != null ? createOptionRequest.getDisplayOrder() : 0
        );

        List<SurveyDetailResponse.OptionResponse> response = surveyQuestionOptionService.updateOption(surveyId, questionId, optionId, request, user);
        return ResponseEntity.ok(mapToOptionList(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> deleteOption(Long surveyId, Long questionId, Long optionId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("선택지 삭제 요청 - surveyId: {}, questionId: {}, optionId: {}, userId: {}", surveyId, questionId, optionId, user.userId());

        surveyQuestionOptionService.deleteOption(surveyId, questionId, optionId, user);
        return ResponseEntity.noContent().build();
    }

    // === Private helper methods ===

    private List<GetSurveyDetail200ResponseQuestionsInnerOptionsInner> mapToOptionList(
            List<SurveyDetailResponse.OptionResponse> options
    ) {
        return options.stream()
                .map(o -> new GetSurveyDetail200ResponseQuestionsInnerOptionsInner()
                        .id(o.id())
                        .text(o.text())
                        .displayOrder(o.displayOrder()))
                .toList();
    }
}
