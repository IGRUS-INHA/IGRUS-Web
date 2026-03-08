package igrus.web.event.controller;

import igrus.web.event.dto.response.RegistrationResponse;
import igrus.web.event.service.ExternalEventRegistrationService;
import igrus.web.generated.api.EventExternalRegistrationApi;
import igrus.web.generated.model.ApiExternalRegisterEventRequest;
import igrus.web.generated.model.ApiRegistrationResponse;
import igrus.web.generated.model.ApiSubmitAnswerRequest;
import igrus.web.survey.response.dto.request.SubmitAnswerRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 외부인 행사 신청 컨트롤러.
 * 외부인(비회원)의 행사 신청 API를 제공합니다.
 * 인증이 불필요한 엔드포인트입니다 (security: []).
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ExternalEventRegistrationController implements EventExternalRegistrationApi {

    private final ExternalEventRegistrationService externalEventRegistrationService;

    @Override
    public ResponseEntity<ApiRegistrationResponse> registerEventExternal(
            Long eventId,
            ApiExternalRegisterEventRequest apiExternalRegisterEventRequest
    ) {
        log.info("외부인 행사 신청 요청 - eventId: {}, name: {}, studentId: {}",
                eventId, apiExternalRegisterEventRequest.getName(), apiExternalRegisterEventRequest.getStudentId());

        List<SubmitAnswerRequest> surveyAnswers = mapToSubmitAnswerRequests(apiExternalRegisterEventRequest);

        RegistrationResponse response = externalEventRegistrationService.registerExternal(
                eventId,
                apiExternalRegisterEventRequest.getName(),
                apiExternalRegisterEventRequest.getStudentId(),
                apiExternalRegisterEventRequest.getPhone(),
                apiExternalRegisterEventRequest.getDepartment(),
                surveyAnswers
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapToApiRegistrationResponse(response));
    }

    // ===== 매핑 헬퍼 =====

    /**
     * Generated 모델의 ApiExternalRegisterEventRequest에서 서비스 내부 DTO인 SubmitAnswerRequest 목록으로 변환합니다.
     * surveyAnswers가 null이거나 비어있으면 빈 리스트를 반환합니다.
     */
    private List<SubmitAnswerRequest> mapToSubmitAnswerRequests(ApiExternalRegisterEventRequest request) {
        if (request.getSurveyAnswers() == null || request.getSurveyAnswers().isEmpty()) {
            return List.of();
        }
        return request.getSurveyAnswers().stream()
                .map(this::mapToSubmitAnswerRequest)
                .toList();
    }

    private SubmitAnswerRequest mapToSubmitAnswerRequest(ApiSubmitAnswerRequest a) {
        List<SubmitAnswerRequest.GridAnswerRequest> gridAnswers = null;
        if (a.getGridAnswers() != null && !a.getGridAnswers().isEmpty()) {
            gridAnswers = a.getGridAnswers().stream()
                    .map(g -> new SubmitAnswerRequest.GridAnswerRequest(
                            g.getRowId(),
                            g.getSelectedOptionIds()
                    ))
                    .toList();
        }
        return new SubmitAnswerRequest(
                a.getQuestionId(),
                a.getTextValue(),
                a.getSelectedOptionIds(),
                a.getNumericValue(),
                gridAnswers
        );
    }

    private ApiRegistrationResponse mapToApiRegistrationResponse(RegistrationResponse r) {
        return new ApiRegistrationResponse()
                .registrationId(r.registrationId())
                .status(r.status() != null
                        ? ApiRegistrationResponse.StatusEnum.fromValue(r.status().name())
                        : null)
                .isRegistered(r.isRegistered());
    }
}
