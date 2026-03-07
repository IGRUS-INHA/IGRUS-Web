package igrus.web.event.controller;

import igrus.web.common.util.PageResponseMapper;
import igrus.web.common.util.PageableUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.event.dto.response.MyRegistrationResponse;
import igrus.web.event.dto.response.RegistrationListResponse;
import igrus.web.event.dto.response.RegistrationResponse;
import igrus.web.event.service.EventRegistrationService;
import igrus.web.generated.api.EventRegistrationApi;
import igrus.web.generated.model.GetMyRegistrations200ResponseInner;
import igrus.web.generated.model.GetRegistrationList200Response;
import igrus.web.generated.model.GetRegistrationList200ResponseContentInner;
import igrus.web.generated.model.RegisterEventRequest;
import igrus.web.generated.model.CancelRegistrationByAdmin200Response;
import igrus.web.generated.model.UpdateMyResponseRequestAnswersInner;
import igrus.web.generated.model.UpdateMyResponseRequestAnswersInnerGridAnswersInner;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.response.dto.request.SubmitAnswerRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 행사 신청 컨트롤러.
 * 행사 신청, 취소, 조회, 승인/거절 API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class EventRegistrationController implements EventRegistrationApi {

    private final EventRegistrationService eventRegistrationService;

    // ===== 신청자용 API =====

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CancelRegistrationByAdmin200Response> registerEvent(
            Long eventId,
            RegisterEventRequest registerEventRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행사 신청 요청 - eventId: {}, userId: {}", eventId, user.userId());
        List<SubmitAnswerRequest> surveyAnswers = mapToSubmitAnswerRequests(registerEventRequest);
        RegistrationResponse response = eventRegistrationService.registerEvent(eventId, user.userId(), surveyAnswers);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToCancelRegistrationByAdmin200Response(response));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CancelRegistrationByAdmin200Response> cancelRegistration(Long eventId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("신청 취소 요청 - eventId: {}, userId: {}", eventId, user.userId());
        RegistrationResponse response = eventRegistrationService.cancelRegistration(eventId, user.userId());
        return ResponseEntity.ok(mapToCancelRegistrationByAdmin200Response(response));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GetMyRegistrations200ResponseInner>> getMyRegistrations1() {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("내 신청 목록 조회 요청 - userId: {}", user.userId());
        List<MyRegistrationResponse> responses = eventRegistrationService.getMyRegistrations(user.userId());
        List<GetMyRegistrations200ResponseInner> result = responses.stream()
                .map(this::mapToMyRegistration200ResponseInner)
                .toList();
        return ResponseEntity.ok(result);
    }

    // ===== 관리자용 API =====

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GetRegistrationList200Response> getRegistrationList(
            Long eventId,
            Integer page,
            Integer size,
            List<String> sort
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("신청자 목록 조회 요청 - eventId: {}, userId: {}", eventId, user.userId());
        Pageable pageable = PageableUtils.of(page, size, sort);
        Page<RegistrationListResponse> responsePage = eventRegistrationService.getRegistrationList(
                eventId, user.userId(), pageable);

        GetRegistrationList200Response result = PageResponseMapper.toSpringPageResponse(
                responsePage,
                this::mapToRegistrationListContentInner,
                GetRegistrationList200Response::new,
                (r, content, meta) -> r
                        .content(content)
                        .totalElements(meta.totalElements())
                        .totalPages(meta.totalPages())
                        .number(meta.number())
                        .size(meta.size())
                        .numberOfElements(meta.numberOfElements())
                        .first(meta.first())
                        .last(meta.last())
                        .empty(meta.empty())
                        .pageable(meta.pageable())
                        .sort(meta.sort())
        );
        return ResponseEntity.ok(result);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CancelRegistrationByAdmin200Response> approveRegistration(Long registrationId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("신청 승인 요청 - registrationId: {}, userId: {}", registrationId, user.userId());
        RegistrationResponse response = eventRegistrationService.approveRegistration(
                registrationId, user.userId());
        return ResponseEntity.ok(mapToCancelRegistrationByAdmin200Response(response));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CancelRegistrationByAdmin200Response> rejectRegistration(Long registrationId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("신청 거절 요청 - registrationId: {}, userId: {}", registrationId, user.userId());
        RegistrationResponse response = eventRegistrationService.rejectRegistration(
                registrationId, user.userId());
        return ResponseEntity.ok(mapToCancelRegistrationByAdmin200Response(response));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CancelRegistrationByAdmin200Response> revertRegistration(Long registrationId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("승인/거절 되돌리기 요청 - registrationId: {}, userId: {}", registrationId, user.userId());
        RegistrationResponse response = eventRegistrationService.revertRegistration(
                registrationId, user.userId());
        return ResponseEntity.ok(mapToCancelRegistrationByAdmin200Response(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<CancelRegistrationByAdmin200Response> cancelRegistrationByAdmin(Long registrationId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("관리자 행사 신청 취소 요청 - registrationId: {}, userId: {}", registrationId, user.userId());
        RegistrationResponse response = eventRegistrationService.cancelRegistrationByAdmin(
                registrationId, user.userId());
        return ResponseEntity.ok(mapToCancelRegistrationByAdmin200Response(response));
    }

    // ===== 매핑 헬퍼 =====

    /**
     * Generated 모델의 RegisterEventRequest에서 서비스 내부 DTO인 SubmitAnswerRequest 목록으로 변환합니다.
     * 요청 본문이 없거나 surveyAnswers가 비어있으면 빈 리스트를 반환합니다.
     */
    private List<SubmitAnswerRequest> mapToSubmitAnswerRequests(RegisterEventRequest request) {
        if (request == null || request.getSurveyAnswers() == null || request.getSurveyAnswers().isEmpty()) {
            return List.of();
        }
        return request.getSurveyAnswers().stream()
                .map(this::mapToSubmitAnswerRequest)
                .toList();
    }

    private SubmitAnswerRequest mapToSubmitAnswerRequest(UpdateMyResponseRequestAnswersInner a) {
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

    private CancelRegistrationByAdmin200Response mapToCancelRegistrationByAdmin200Response(RegistrationResponse r) {
        return new CancelRegistrationByAdmin200Response()
                .registrationId(r.registrationId())
                .status(r.status() != null
                        ? CancelRegistrationByAdmin200Response.StatusEnum.fromValue(r.status().name())
                        : null)
                .isRegistered(r.isRegistered());
    }

    private GetMyRegistrations200ResponseInner mapToMyRegistration200ResponseInner(MyRegistrationResponse r) {
        return new GetMyRegistrations200ResponseInner()
                .registrationId(r.registrationId())
                .eventId(r.eventId())
                .eventTitle(r.eventTitle())
                .eventStartAt(r.eventStartAt())
                .status(r.status() != null
                        ? GetMyRegistrations200ResponseInner.StatusEnum.fromValue(r.status().name())
                        : null)
                .registeredAt(r.registeredAt());
    }

    private GetRegistrationList200ResponseContentInner mapToRegistrationListContentInner(
            RegistrationListResponse r) {
        return new GetRegistrationList200ResponseContentInner()
                .registrationId(r.registrationId())
                .userId(r.userId())
                .userName(r.userName())
                .userEmail(r.userEmail())
                .studentId(r.studentId())
                .userGender(r.userGender())
                .userGrade(r.userGrade())
                .userDepartment(r.userDepartment())
                .status(r.status() != null
                        ? GetRegistrationList200ResponseContentInner.StatusEnum.fromValue(r.status().name())
                        : null)
                .registeredAt(r.registeredAt())
                .isExternal(r.isExternal())
                .phone(r.phone());
    }
}
