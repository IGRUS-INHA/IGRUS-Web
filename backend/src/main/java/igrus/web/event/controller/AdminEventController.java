package igrus.web.event.controller;

import igrus.web.common.util.EnumUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.event.domain.EventStatus;
import igrus.web.event.domain.EventVisibility;
import igrus.web.event.domain.RegistrationStatus;
import igrus.web.event.dto.response.EventAttachmentDto;
import igrus.web.event.dto.response.EventDetailResponse;
import igrus.web.event.dto.response.EventListResponse;
import igrus.web.event.service.EventService;
import igrus.web.generated.api.AdminEventApi;
import igrus.web.generated.model.ApiAdminEventDetailResponse;
import igrus.web.generated.model.ApiAdminEventListResponse;
import igrus.web.generated.model.ApiEventAttachmentResponse;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 전용 행사 컨트롤러.
 * 행사 목록/상세 조회 및 공개/비공개 관리 API를 제공합니다.
 * OPERATOR 이상 권한이 필요합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AdminEventController implements AdminEventApi {

    private final EventService eventService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ApiAdminEventListResponse>> getAdminEventList(
            String visibility,
            String eventStatus,
            String registrationStatus
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        EventVisibility visibilityEnum = EnumUtils.fromStringOrNull(EventVisibility.class, visibility);
        EventStatus eventStatusEnum = EnumUtils.fromStringOrNull(EventStatus.class, eventStatus);
        RegistrationStatus registrationStatusEnum = EnumUtils.fromStringOrNull(
                RegistrationStatus.class, registrationStatus);

        log.info("[관리자] 행사 목록 조회 요청 - userId: {}, visibility: {}, eventStatus: {}, registrationStatus: {}",
                user.userId(), visibilityEnum, eventStatusEnum, registrationStatusEnum);

        List<EventListResponse> responses = eventService.getAdminEventList(
                visibilityEnum, eventStatusEnum, registrationStatusEnum);
        List<ApiAdminEventListResponse> result = responses.stream()
                .map(this::mapToAdminEventListResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiAdminEventDetailResponse> getAdminEvent(Long eventId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("[관리자] 행사 상세 조회 요청 - eventId: {}, userId: {}", eventId, user.userId());
        EventDetailResponse response = eventService.getAdminEvent(eventId, user.userId());
        return ResponseEntity.ok(mapToAdminEventDetailResponse(response));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiAdminEventDetailResponse> publishEvent(Long eventId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행사 공개 요청 - eventId: {}, userId: {}", eventId, user.userId());
        EventDetailResponse response = eventService.publishEvent(eventId, user.userId());
        return ResponseEntity.ok(mapToAdminEventDetailResponse(response));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiAdminEventDetailResponse> unpublishEvent(Long eventId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("행사 비공개 요청 - eventId: {}, userId: {}", eventId, user.userId());
        EventDetailResponse response = eventService.unpublishEvent(eventId, user.userId());
        return ResponseEntity.ok(mapToAdminEventDetailResponse(response));
    }

    // ===== 매핑 헬퍼 =====

    private ApiAdminEventDetailResponse mapToAdminEventDetailResponse(EventDetailResponse r) {
        ApiAdminEventDetailResponse response = new ApiAdminEventDetailResponse()
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
                .registrationStatus(r.registrationStatus() != null
                        ? ApiAdminEventDetailResponse.RegistrationStatusEnum.fromValue(
                                r.registrationStatus().name())
                        : null)
                .eventStatus(r.eventStatus() != null
                        ? ApiAdminEventDetailResponse.EventStatusEnum.fromValue(r.eventStatus().name())
                        : null)
                .closeReason(r.closeReason() != null
                        ? ApiAdminEventDetailResponse.CloseReasonEnum.fromValue(r.closeReason().name())
                        : null)
                .registrationType(r.registrationType() != null
                        ? ApiAdminEventDetailResponse.RegistrationTypeEnum.fromValue(
                                r.registrationType().name())
                        : null)
                .isRegistrable(r.isRegistrable())
                .createdAt(r.createdAt())
                .updatedAt(r.updatedAt())
                .canEdit(r.canEdit())
                .isRegistered(r.isRegistered())
                .visibility(r.visibility() != null
                        ? ApiAdminEventDetailResponse.VisibilityEnum.fromValue(r.visibility().name())
                        : null)
                .surveyId(r.surveyId())
                .allowExternal(r.allowExternal());

        if (r.attachments() != null) {
            response.setAttachments(r.attachments().stream()
                    .map(this::mapToAttachmentResponse)
                    .toList());
        }

        return response;
    }

    private ApiAdminEventListResponse mapToAdminEventListResponse(EventListResponse r) {
        return new ApiAdminEventListResponse()
                .id(r.id())
                .title(r.title())
                .location(r.location())
                .eventStartAt(r.eventStartAt())
                .eventEndAt(r.eventEndAt())
                .registrationStartAt(r.registrationStartAt())
                .registrationEndAt(r.registrationEndAt())
                .capacity(r.capacity())
                .currentCount(r.currentCount())
                .registrationStatus(r.registrationStatus() != null
                        ? ApiAdminEventListResponse.RegistrationStatusEnum.fromValue(
                                r.registrationStatus().name())
                        : null)
                .eventStatus(r.eventStatus() != null
                        ? ApiAdminEventListResponse.EventStatusEnum.fromValue(
                                r.eventStatus().name())
                        : null)
                .registrationType(r.registrationType() != null
                        ? ApiAdminEventListResponse.RegistrationTypeEnum.fromValue(
                                r.registrationType().name())
                        : null)
                .isRegistrable(r.isRegistrable())
                .visibility(r.visibility() != null
                        ? ApiAdminEventListResponse.VisibilityEnum.fromValue(
                                r.visibility().name())
                        : null)
                .surveyId(r.surveyId())
                .allowExternal(r.allowExternal())
                .thumbnailObjectKey(r.thumbnailObjectKey());
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
