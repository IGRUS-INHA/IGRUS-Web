package igrus.web.event.controller;

import igrus.web.common.util.PageResponseMapper;
import igrus.web.common.util.PageableUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.event.dto.response.RegistrationListResponse;
import igrus.web.event.service.EventRegistrationService;
import igrus.web.generated.api.EventRegistrationApi;
import igrus.web.generated.model.ApiMyRegistrationResponse;
import igrus.web.generated.model.ApiPageRegistrationListResponse;
import igrus.web.generated.model.ApiRegistrationListResponse;
import igrus.web.generated.model.ApiRegisterEventRequest;
import igrus.web.generated.model.ApiRegistrationResponse;
import igrus.web.generated.model.ApiSubmitAnswerRequest;
import igrus.web.generated.model.ApiGridAnswerRequest;
import igrus.web.survey.response.dto.request.SubmitAnswerRequest;
import igrus.web.survey.response.dto.request.SubmitAnswerRequest.GridAnswerRequest;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<ApiRegistrationResponse> registerEvent(
            Long eventId,
            ApiRegisterEventRequest registerEventRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행사 신청 요청 - eventId: {}, userId: {}", eventId, user.userId());
        var surveyAnswers = mapToSubmitAnswerRequests(registerEventRequest);
        var response = eventRegistrationService.registerEvent(eventId, user.userId(), surveyAnswers);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToRegistrationResponse(response));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiRegistrationResponse> cancelRegistration(Long eventId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("신청 취소 요청 - eventId: {}, userId: {}", eventId, user.userId());
        var response = eventRegistrationService.cancelRegistration(eventId, user.userId());
        return ResponseEntity.ok(mapToRegistrationResponse(response));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ApiMyRegistrationResponse>> getMyRegistrations1() {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("내 신청 목록 조회 요청 - userId: {}", user.userId());
        var responses = eventRegistrationService.getMyRegistrations(user.userId());
        List<ApiMyRegistrationResponse> result = responses.stream()
                .map(this::mapToMyRegistration200ResponseInner)
                .toList();
        return ResponseEntity.ok(result);
    }

    // ===== 관리자용 API =====

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiPageRegistrationListResponse> getRegistrationList(
            Long eventId,
            Integer page,
            Integer size,
            List<String> sort
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("신청자 목록 조회 요청 - eventId: {}, userId: {}", eventId, user.userId());
        Pageable pageable = PageableUtils.of(page, size, sort);
        var responsePage = eventRegistrationService.getRegistrationList(
                eventId, user.userId(), pageable);

        ApiPageRegistrationListResponse result = PageResponseMapper.toSpringPageResponse(
                responsePage,
                this::mapToRegistrationListContentInner,
                ApiPageRegistrationListResponse::new,
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
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiRegistrationResponse> approveRegistration(Long registrationId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("신청 승인 요청 - registrationId: {}, userId: {}", registrationId, user.userId());
        var response = eventRegistrationService.approveRegistration(
                registrationId, user.userId());
        return ResponseEntity.ok(mapToRegistrationResponse(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiRegistrationResponse> rejectRegistration(Long registrationId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("신청 거절 요청 - registrationId: {}, userId: {}", registrationId, user.userId());
        var response = eventRegistrationService.rejectRegistration(
                registrationId, user.userId());
        return ResponseEntity.ok(mapToRegistrationResponse(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiRegistrationResponse> revertRegistration(Long registrationId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("승인/거절 되돌리기 요청 - registrationId: {}, userId: {}", registrationId, user.userId());
        var response = eventRegistrationService.revertRegistration(
                registrationId, user.userId());
        return ResponseEntity.ok(mapToRegistrationResponse(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiRegistrationResponse> cancelRegistrationByAdmin(Long registrationId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("관리자 행사 신청 취소 요청 - registrationId: {}, userId: {}", registrationId, user.userId());
        var response = eventRegistrationService.cancelRegistrationByAdmin(
                registrationId, user.userId());
        return ResponseEntity.ok(mapToRegistrationResponse(response));
    }

    // ===== 매핑 헬퍼 =====

    /**
     * Generated 모델의 RegisterEventRequest에서 서비스 내부 DTO인 SubmitAnswerRequest 목록으로 변환합니다.
     * 요청 본문이 없거나 surveyAnswers가 비어있으면 빈 리스트를 반환합니다.
     */
    private List<SubmitAnswerRequest> mapToSubmitAnswerRequests(ApiRegisterEventRequest request) {
        if (request == null || request.getSurveyAnswers() == null || request.getSurveyAnswers().isEmpty()) {
            return List.of();
        }
        return request.getSurveyAnswers().stream()
                .map(this::mapToSubmitAnswerRequest)
                .toList();
    }

    private SubmitAnswerRequest mapToSubmitAnswerRequest(ApiSubmitAnswerRequest a) {
        List<GridAnswerRequest> gridAnswers = null;
        if (a.getGridAnswers() != null && !a.getGridAnswers().isEmpty()) {
            gridAnswers = a.getGridAnswers().stream()
                    .map(g -> new GridAnswerRequest(
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

    private ApiRegistrationResponse mapToRegistrationResponse(igrus.web.event.dto.response.RegistrationResponse r) {
        return new ApiRegistrationResponse()
                .registrationId(r.registrationId())
                .status(r.status() != null
                        ? ApiRegistrationResponse.StatusEnum.fromValue(r.status().name())
                        : null)
                .isRegistered(r.isRegistered());
    }

    private ApiMyRegistrationResponse mapToMyRegistration200ResponseInner(igrus.web.event.dto.response.MyRegistrationResponse r) {
        return new ApiMyRegistrationResponse()
                .registrationId(r.registrationId())
                .eventId(r.eventId())
                .eventTitle(r.eventTitle())
                .eventStartAt(r.eventStartAt())
                .status(r.status() != null
                        ? ApiMyRegistrationResponse.StatusEnum.fromValue(r.status().name())
                        : null)
                .registeredAt(r.registeredAt());
    }

    private ApiRegistrationListResponse mapToRegistrationListContentInner(
            RegistrationListResponse r) {
        return new ApiRegistrationListResponse()
                .registrationId(r.registrationId())
                .userId(r.userId())
                .userName(r.userName())
                .userEmail(r.userEmail())
                .studentId(r.studentId())
                .userGender(r.userGender())
                .userGrade(r.userGrade())
                .userDepartment(r.userDepartment())
                .status(r.status() != null
                        ? ApiRegistrationListResponse.StatusEnum.fromValue(r.status().name())
                        : null)
                .registeredAt(r.registeredAt())
                .isExternal(r.isExternal())
                .phone(r.phone());
    }
}
