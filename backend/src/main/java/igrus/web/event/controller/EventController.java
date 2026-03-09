package igrus.web.event.controller;

import igrus.web.common.util.EnumUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.event.domain.EventStatus;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.domain.RegistrationStatus;
import igrus.web.survey.question.domain.SurveyQuestionType;
import igrus.web.survey.question.exception.SurveyQuestionValidationException;
import igrus.web.event.dto.response.EventDetailResponse;
import igrus.web.event.dto.response.EventListResponse;
import igrus.web.event.dto.request.CreateEventRequest;
import igrus.web.event.dto.request.UpdateEventRequest;
import igrus.web.event.dto.response.EventAttachmentDto;
import igrus.web.event.service.EventService;
import igrus.web.event.service.EventWithSurveyService;
import igrus.web.event.service.EventWithSurveyService.CreateEventWithSurveyRequest;
import igrus.web.generated.api.EventApi;
import igrus.web.generated.model.ApiEventCreateResponse;
import igrus.web.generated.model.ApiEventDetailResponse;
import igrus.web.generated.model.ApiEventAttachmentResponse;
import igrus.web.generated.model.ApiEventImageDownloadUrlResponse;
import igrus.web.generated.model.ApiEventListResponse;
import igrus.web.generated.model.ApiEventStatusChangeReasonRequest;
import igrus.web.generated.model.ApiCreateEventRequest;
import igrus.web.generated.model.ApiCreateEventWithSurveyRequest;
import igrus.web.generated.model.ApiCreateEventSurveyQuestionRequest;
import igrus.web.generated.model.ApiUpdateEventRequest;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 행사 컨트롤러.
 * 행사 생성, 조회, 수정, 삭제 및 상태 관리 API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class EventController implements EventApi {

    private final EventService eventService;
    private final EventWithSurveyService eventWithSurveyService;

    // ===== 행사 CRUD =====

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiEventCreateResponse> createEvent(
            ApiCreateEventRequest createEventRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행사 생성 요청 - userId: {}, title: {}", user.userId(), createEventRequest.getTitle());

        CreateEventRequest request = new CreateEventRequest(
                createEventRequest.getTitle(),
                createEventRequest.getDescription(),
                createEventRequest.getLocation(),
                createEventRequest.getEventStartAt(),
                createEventRequest.getEventEndAt(),
                createEventRequest.getRegistrationStartAt(),
                createEventRequest.getRegistrationEndAt(),
                createEventRequest.getCapacity(),
                EnumUtils.fromStringOrNull(EventRegistrationType.class,
                        createEventRequest.getRegistrationType().getValue()),
                createEventRequest.getSurveyId(),
                createEventRequest.getAttachmentObjectKeys(),
                createEventRequest.getAllowExternal()
        );

        var result = eventService.createEvent(request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiEventCreateResponse()
                .id(result.id())
                .title(result.title())
                .createdAt(result.createdAt())
                .surveyId(result.surveyId())
                .allowExternal(result.allowExternal()));
    }

    @Override
    public ResponseEntity<List<ApiEventListResponse>> getEventList(
            String eventStatus,
            String registrationStatus
    ) {
        EventStatus eventStatusEnum = EnumUtils.fromStringOrNull(EventStatus.class, eventStatus);
        RegistrationStatus registrationStatusEnum = EnumUtils.fromStringOrNull(
                RegistrationStatus.class, registrationStatus);

        log.info("행사 목록 조회 요청 - eventStatus: {}, registrationStatus: {}",
                eventStatusEnum, registrationStatusEnum);

        var responses = eventService.getEventList(eventStatusEnum, registrationStatusEnum);
        List<ApiEventListResponse> result = responses.stream()
                .map(this::mapToEventListResponseInner)
                .toList();
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<ApiEventDetailResponse> getEvent(Long eventId) {
        AuthenticatedUser user = SecurityUtils.getCurrentUser();
        Long userId = user != null ? user.userId() : null;
        log.info("행사 상세 조회 요청 - eventId: {}, userId: {}", eventId, userId);
        var response = eventService.getEvent(eventId, userId);
        return ResponseEntity.ok(mapToEventDetailResponse(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiEventDetailResponse> updateEvent(
            Long eventId,
            ApiUpdateEventRequest updateEventRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행사 수정 요청 - eventId: {}, userId: {}", eventId, user.userId());

        UpdateEventRequest request = new UpdateEventRequest(
                updateEventRequest.getTitle(),
                updateEventRequest.getDescription(),
                updateEventRequest.getLocation(),
                updateEventRequest.getEventStartAt(),
                updateEventRequest.getEventEndAt(),
                updateEventRequest.getRegistrationStartAt(),
                updateEventRequest.getRegistrationEndAt(),
                updateEventRequest.getCapacity(),
                updateEventRequest.getSurveyId(),
                updateEventRequest.getAttachmentObjectKeys(),
                updateEventRequest.getAllowExternal()
        );

        var response = eventService.updateEvent(eventId, request, user.userId());
        return ResponseEntity.ok(mapToEventDetailResponse(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> deleteEvent(Long eventId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행사 삭제 요청 - eventId: {}, userId: {}", eventId, user.userId());
        eventService.deleteEvent(eventId, user.userId());
        return ResponseEntity.noContent().build();
    }

    // ===== 행사 상태 관리 =====

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiEventDetailResponse> closeEvent(
            Long eventId,
            ApiEventStatusChangeReasonRequest eventStatusChangeReasonRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("등록 마감 요청 - eventId: {}, userId: {}, reason: {}",
                eventId, user.userId(), eventStatusChangeReasonRequest.getReason());
        var response = eventService.closeEvent(
                eventId, user.userId(), eventStatusChangeReasonRequest.getReason());
        return ResponseEntity.ok(mapToEventDetailResponse(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiEventDetailResponse> cancelEvent(
            Long eventId,
            ApiEventStatusChangeReasonRequest eventStatusChangeReasonRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행사 취소 요청 - eventId: {}, userId: {}, reason: {}",
                eventId, user.userId(), eventStatusChangeReasonRequest.getReason());
        var response = eventService.cancelEvent(
                eventId, user.userId(), eventStatusChangeReasonRequest.getReason());
        return ResponseEntity.ok(mapToEventDetailResponse(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiEventDetailResponse> reactivateEvent(
            Long eventId,
            ApiEventStatusChangeReasonRequest eventStatusChangeReasonRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행사 재활성화 요청 - eventId: {}, userId: {}, reason: {}",
                eventId, user.userId(), eventStatusChangeReasonRequest.getReason());
        var response = eventService.reactivateEvent(
                eventId, user.userId(), eventStatusChangeReasonRequest.getReason());
        return ResponseEntity.ok(mapToEventDetailResponse(response));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiEventDetailResponse> reopenRegistration(
            Long eventId,
            ApiEventStatusChangeReasonRequest eventStatusChangeReasonRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("등록 재오픈 요청 - eventId: {}, userId: {}, reason: {}",
                eventId, user.userId(), eventStatusChangeReasonRequest.getReason());
        var response = eventService.reopenRegistration(
                eventId, user.userId(), eventStatusChangeReasonRequest.getReason());
        return ResponseEntity.ok(mapToEventDetailResponse(response));
    }

    // ===== 매핑 헬퍼 =====

    private ApiEventDetailResponse mapToEventDetailResponse(EventDetailResponse r) {
        ApiEventDetailResponse response = new ApiEventDetailResponse()
                .id(r.id())
                .title(r.title())
                .description(r.description())
                .location(r.location())
                .authorName(r.authorName())
                .eventStartAt(r.eventStartAt())
                .eventEndAt(r.eventEndAt())
                .registrationStartAt(r.registrationStartAt())
                .registrationEndAt(r.registrationEndAt())
                .capacity(r.capacity())
                .currentCount(r.currentCount())
                .visibility(r.visibility() != null
                        ? ApiEventDetailResponse.VisibilityEnum.fromValue(r.visibility().name())
                        : null)
                .registrationStatus(r.registrationStatus() != null
                        ? ApiEventDetailResponse.RegistrationStatusEnum.fromValue(r.registrationStatus().name())
                        : null)
                .eventStatus(r.eventStatus() != null
                        ? ApiEventDetailResponse.EventStatusEnum.fromValue(r.eventStatus().name())
                        : null)
                .closeReason(r.closeReason() != null
                        ? ApiEventDetailResponse.CloseReasonEnum.fromValue(r.closeReason().name())
                        : null)
                .registrationType(r.registrationType() != null
                        ? ApiEventDetailResponse.RegistrationTypeEnum.fromValue(r.registrationType().name())
                        : null)
                .isRegistrable(r.isRegistrable())
                .createdAt(r.createdAt())
                .updatedAt(r.updatedAt())
                .canEdit(r.canEdit())
                .isRegistered(r.isRegistered())
                .surveyId(r.surveyId())
                .allowExternal(r.allowExternal());

        if (r.attachments() != null) {
            response.setAttachments(r.attachments().stream()
                    .map(this::mapToAttachmentResponse)
                    .toList());
        }

        return response;
    }

    private ApiEventListResponse mapToEventListResponseInner(EventListResponse r) {
        return new ApiEventListResponse()
                .id(r.id())
                .title(r.title())
                .location(r.location())
                .eventStartAt(r.eventStartAt())
                .eventEndAt(r.eventEndAt())
                .registrationEndAt(r.registrationEndAt())
                .capacity(r.capacity())
                .currentCount(r.currentCount())
                .visibility(r.visibility() != null
                        ? ApiEventListResponse.VisibilityEnum.fromValue(r.visibility().name())
                        : null)
                .registrationStatus(r.registrationStatus() != null
                        ? ApiEventListResponse.RegistrationStatusEnum.fromValue(
                                r.registrationStatus().name())
                        : null)
                .eventStatus(r.eventStatus() != null
                        ? ApiEventListResponse.EventStatusEnum.fromValue(r.eventStatus().name())
                        : null)
                .registrationType(r.registrationType() != null
                        ? ApiEventListResponse.RegistrationTypeEnum.fromValue(
                                r.registrationType().name())
                        : null)
                .isRegistrable(r.isRegistrable())
                .surveyId(r.surveyId())
                .allowExternal(r.allowExternal())
                .thumbnailObjectKey(r.thumbnailObjectKey());
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiEventCreateResponse> createEventWithSurvey(
            ApiCreateEventWithSurveyRequest req) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행사+설문 원자적 생성 요청 - userId: {}, title: {}", user.userId(), req.getTitle());

        // Defense-in-depth: Bean Validation(@NotNull)이 차단하지만, 직접 호출 시 NPE 방지
        if (req.getSurvey() == null || req.getSurvey().getQuestions() == null) {
            throw new SurveyQuestionValidationException("설문 정보와 질문 목록은 필수입니다");
        }

        List<CreateEventWithSurveyRequest.QuestionData> questions = req.getSurvey().getQuestions().stream()
                .map(q -> new CreateEventWithSurveyRequest.QuestionData(
                        EnumUtils.fromStringOrNull(SurveyQuestionType.class, q.getQuestionType().getValue()),
                        q.getTitle(),
                        Boolean.TRUE.equals(q.getRequired()),
                        q.getDisplayOrder() != null ? q.getDisplayOrder() : 0,
                        q.getOptions()
                ))
                .toList();

        CreateEventWithSurveyRequest request = new CreateEventWithSurveyRequest(
                req.getTitle(),
                req.getDescription(),
                req.getLocation(),
                req.getEventStartAt(),
                req.getEventEndAt(),
                req.getRegistrationStartAt(),
                req.getRegistrationEndAt(),
                req.getCapacity(),
                EnumUtils.fromStringOrNull(EventRegistrationType.class,
                        req.getRegistrationType().getValue()),
                req.getAttachmentObjectKeys(),
                req.getAllowExternal(),
                req.getSurvey().getTitle(),
                req.getSurvey().getDescription(),
                questions
        );

        var result = eventWithSurveyService.createEventWithSurvey(request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiEventCreateResponse()
                .id(result.id())
                .title(result.title())
                .createdAt(result.createdAt())
                .surveyId(result.surveyId())
                .allowExternal(result.allowExternal()));
    }

    @Override
    public ResponseEntity<ApiEventImageDownloadUrlResponse> getEventImageDownloadUrl(
            Long eventId, String objectKey) {
        log.info("행사 이미지 다운로드 URL 요청 - eventId: {}, objectKey: {}", eventId, objectKey);
        var result = eventService.getEventImageDownloadUrl(eventId, objectKey);
        return ResponseEntity.ok(new ApiEventImageDownloadUrlResponse()
                .presignedUrl(result.presignedUrl()));
    }

    private ApiEventAttachmentResponse mapToAttachmentResponse(EventAttachmentDto a) {
        return new ApiEventAttachmentResponse()
                .id(a.id())
                .fileMetadataId(a.fileMetadataId())
                .objectKey(a.objectKey())
                .originalFileName(a.originalFileName())
                .contentType(a.contentType());
    }
}
