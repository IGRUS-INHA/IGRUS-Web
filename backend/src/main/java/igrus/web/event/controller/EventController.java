package igrus.web.event.controller;

import igrus.web.common.util.EnumUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.event.domain.EventStatus;
import igrus.web.event.domain.RegistrationStatus;
import igrus.web.event.dto.request.CreateEventRequest;
import igrus.web.event.dto.request.UpdateEventRequest;
import igrus.web.event.dto.response.EventAttachmentDto;
import igrus.web.event.dto.response.EventCreateResponse;
import igrus.web.event.dto.response.EventDetailResponse;
import igrus.web.event.dto.response.EventListResponse;
import igrus.web.event.service.EventService;
import igrus.web.generated.api.EventApi;
import igrus.web.generated.model.CreateEvent201Response;
import igrus.web.generated.model.GetEvent200Response;
import igrus.web.generated.model.GetEvent200ResponseAttachmentsInner;
import igrus.web.generated.model.GetEventList200ResponseInner;
import igrus.web.generated.model.ReopenRegistrationRequest;
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

    // ===== 행사 CRUD =====

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CreateEvent201Response> createEvent(
            igrus.web.generated.model.CreateEventRequest createEventRequest
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
                EnumUtils.fromStringOrNull(igrus.web.event.domain.EventRegistrationType.class,
                        createEventRequest.getRegistrationType().getValue()),
                createEventRequest.getSurveyId(),
                createEventRequest.getAttachmentFileIds(),
                createEventRequest.getThumbnailFileId()
        );

        EventCreateResponse result = eventService.createEvent(request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateEvent201Response()
                .id(result.id())
                .title(result.title())
                .createdAt(result.createdAt())
                .surveyId(result.surveyId()));
    }

    @Override
    public ResponseEntity<List<GetEventList200ResponseInner>> getEventList(
            String eventStatus,
            String registrationStatus
    ) {
        EventStatus eventStatusEnum = EnumUtils.fromStringOrNull(EventStatus.class, eventStatus);
        RegistrationStatus registrationStatusEnum = EnumUtils.fromStringOrNull(
                RegistrationStatus.class, registrationStatus);

        log.info("행사 목록 조회 요청 - eventStatus: {}, registrationStatus: {}",
                eventStatusEnum, registrationStatusEnum);

        List<EventListResponse> responses = eventService.getEventList(eventStatusEnum, registrationStatusEnum);
        List<GetEventList200ResponseInner> result = responses.stream()
                .map(this::mapToEventListResponseInner)
                .toList();
        return ResponseEntity.ok(result);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GetEvent200Response> getEvent(Long eventId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행사 상세 조회 요청 - eventId: {}, userId: {}", eventId, user.userId());
        EventDetailResponse response = eventService.getEvent(eventId, user.userId());
        return ResponseEntity.ok(mapToGetEvent200Response(response));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GetEvent200Response> updateEvent(
            Long eventId,
            igrus.web.generated.model.UpdateEventRequest updateEventRequest
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
                updateEventRequest.getAttachmentFileIds(),
                updateEventRequest.getThumbnailFileId()
        );

        EventDetailResponse response = eventService.updateEvent(eventId, request, user.userId());
        return ResponseEntity.ok(mapToGetEvent200Response(response));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteEvent(Long eventId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행사 삭제 요청 - eventId: {}, userId: {}", eventId, user.userId());
        eventService.deleteEvent(eventId, user.userId());
        return ResponseEntity.noContent().build();
    }

    // ===== 행사 상태 관리 =====

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GetEvent200Response> closeEvent(
            Long eventId,
            ReopenRegistrationRequest reopenRegistrationRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("등록 마감 요청 - eventId: {}, userId: {}, reason: {}",
                eventId, user.userId(), reopenRegistrationRequest.getReason());
        EventDetailResponse response = eventService.closeEvent(
                eventId, user.userId(), reopenRegistrationRequest.getReason());
        return ResponseEntity.ok(mapToGetEvent200Response(response));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GetEvent200Response> cancelEvent(
            Long eventId,
            ReopenRegistrationRequest reopenRegistrationRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행사 취소 요청 - eventId: {}, userId: {}, reason: {}",
                eventId, user.userId(), reopenRegistrationRequest.getReason());
        EventDetailResponse response = eventService.cancelEvent(
                eventId, user.userId(), reopenRegistrationRequest.getReason());
        return ResponseEntity.ok(mapToGetEvent200Response(response));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GetEvent200Response> reactivateEvent(
            Long eventId,
            ReopenRegistrationRequest reopenRegistrationRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행사 재활성화 요청 - eventId: {}, userId: {}, reason: {}",
                eventId, user.userId(), reopenRegistrationRequest.getReason());
        EventDetailResponse response = eventService.reactivateEvent(
                eventId, user.userId(), reopenRegistrationRequest.getReason());
        return ResponseEntity.ok(mapToGetEvent200Response(response));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GetEvent200Response> reopenRegistration(
            Long eventId,
            ReopenRegistrationRequest reopenRegistrationRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("등록 재오픈 요청 - eventId: {}, userId: {}, reason: {}",
                eventId, user.userId(), reopenRegistrationRequest.getReason());
        EventDetailResponse response = eventService.reopenRegistration(
                eventId, user.userId(), reopenRegistrationRequest.getReason());
        return ResponseEntity.ok(mapToGetEvent200Response(response));
    }

    // ===== 매핑 헬퍼 =====

    private GetEvent200Response mapToGetEvent200Response(EventDetailResponse r) {
        GetEvent200Response response = new GetEvent200Response()
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
                        ? GetEvent200Response.VisibilityEnum.fromValue(r.visibility().name())
                        : null)
                .registrationStatus(r.registrationStatus() != null
                        ? GetEvent200Response.RegistrationStatusEnum.fromValue(r.registrationStatus().name())
                        : null)
                .eventStatus(r.eventStatus() != null
                        ? GetEvent200Response.EventStatusEnum.fromValue(r.eventStatus().name())
                        : null)
                .closeReason(r.closeReason() != null
                        ? GetEvent200Response.CloseReasonEnum.fromValue(r.closeReason().name())
                        : null)
                .registrationType(r.registrationType() != null
                        ? GetEvent200Response.RegistrationTypeEnum.fromValue(r.registrationType().name())
                        : null)
                .isRegistrable(r.isRegistrable())
                .createdAt(r.createdAt())
                .updatedAt(r.updatedAt())
                .canEdit(r.canEdit())
                .isRegistered(r.isRegistered())
                .surveyId(r.surveyId());

        if (r.attachments() != null) {
            response.setAttachments(r.attachments().stream()
                    .map(this::mapToAttachmentResponse)
                    .toList());
        }

        return response;
    }

    private GetEventList200ResponseInner mapToEventListResponseInner(EventListResponse r) {
        return new GetEventList200ResponseInner()
                .id(r.id())
                .title(r.title())
                .location(r.location())
                .eventStartAt(r.eventStartAt())
                .eventEndAt(r.eventEndAt())
                .registrationEndAt(r.registrationEndAt())
                .capacity(r.capacity())
                .currentCount(r.currentCount())
                .visibility(r.visibility() != null
                        ? GetEventList200ResponseInner.VisibilityEnum.fromValue(r.visibility().name())
                        : null)
                .registrationStatus(r.registrationStatus() != null
                        ? GetEventList200ResponseInner.RegistrationStatusEnum.fromValue(
                                r.registrationStatus().name())
                        : null)
                .eventStatus(r.eventStatus() != null
                        ? GetEventList200ResponseInner.EventStatusEnum.fromValue(r.eventStatus().name())
                        : null)
                .registrationType(r.registrationType() != null
                        ? GetEventList200ResponseInner.RegistrationTypeEnum.fromValue(
                                r.registrationType().name())
                        : null)
                .isRegistrable(r.isRegistrable())
                .surveyId(r.surveyId())
                .thumbnailUrl(r.thumbnailUrl());
    }

    private GetEvent200ResponseAttachmentsInner mapToAttachmentResponse(EventAttachmentDto a) {
        return new GetEvent200ResponseAttachmentsInner()
                .id(a.id())
                .fileMetadataId(a.fileMetadataId())
                .objectKey(a.objectKey())
                .originalFileName(a.originalFileName())
                .contentType(a.contentType())
                .isThumbnail(a.isThumbnail())
                .displayOrder(a.displayOrder());
    }
}
