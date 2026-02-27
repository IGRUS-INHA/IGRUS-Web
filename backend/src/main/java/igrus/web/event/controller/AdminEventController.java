package igrus.web.event.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.event.domain.EventStatus;
import igrus.web.event.domain.EventVisibility;
import igrus.web.event.domain.RegistrationStatus;
import igrus.web.event.dto.response.EventDetailResponse;
import igrus.web.event.dto.response.EventListResponse;
import igrus.web.event.service.EventService;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 전용 행사 컨트롤러.
 * 행사 목록/상세 조회 및 공개/비공개 관리 API를 제공합니다.
 * OPERATOR 이상 권한이 필요합니다.
 */
@Tag(name = "Admin Event", description = "관리자 행사 관리 API")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;

    @Operation(summary = "관리자 행사 목록 조회", description = "관리자용 행사 목록을 조회합니다. visibility, 행사 진행 상태, 등록 상태별 필터링이 가능합니다. OPERATOR 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "행사 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping
    public ResponseEntity<List<EventListResponse>> getAdminEventList(
            @Parameter(description = "공개 상태 필터 (PUBLISHED, UNPUBLISHED)")
            @RequestParam(required = false) EventVisibility visibility,
            @Parameter(description = "행사 진행 상태 필터 (UPCOMING, ONGOING, COMPLETED, CANCELED)")
            @RequestParam(required = false) EventStatus eventStatus,
            @Parameter(description = "등록 상태 필터 (NOT_STARTED, OPEN, CLOSED)")
            @RequestParam(required = false) RegistrationStatus registrationStatus,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("[관리자] 행사 목록 조회 요청 - userId: {}, visibility: {}, eventStatus: {}, registrationStatus: {}",
                user.userId(), visibility, eventStatus, registrationStatus);
        List<EventListResponse> response = eventService.getAdminEventList(visibility, eventStatus, registrationStatus);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "관리자 행사 상세 조회", description = "관리자용 행사 상세 정보를 조회합니다. visibility 무관하게 모든 행사를 조회할 수 있습니다. OPERATOR 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "행사 상세 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "행사를 찾을 수 없음")
    })
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailResponse> getAdminEvent(
            @Parameter(description = "행사 ID") @PathVariable Long eventId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("[관리자] 행사 상세 조회 요청 - eventId: {}, userId: {}", eventId, user.userId());
        EventDetailResponse response = eventService.getAdminEvent(eventId, user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "행사 공개", description = "행사를 공개합니다. UNPUBLISHED -> PUBLISHED. body 없이 호출합니다. OPERATOR 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "행사 공개 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "이미 공개 상태"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "행사를 찾을 수 없음")
    })
    @PostMapping("/{eventId}/publish")
    public ResponseEntity<EventDetailResponse> publishEvent(
            @Parameter(description = "행사 ID") @PathVariable Long eventId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행사 공개 요청 - eventId: {}, userId: {}", eventId, user.userId());
        EventDetailResponse response = eventService.publishEvent(eventId, user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "행사 비공개", description = "행사를 비공개로 전환합니다. PUBLISHED -> UNPUBLISHED. 등록 상태가 OPEN이면 자동 마감됩니다. body 없이 호출합니다. OPERATOR 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "행사 비공개 전환 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "이미 비공개 상태"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "행사를 찾을 수 없음")
    })
    @PostMapping("/{eventId}/unpublish")
    public ResponseEntity<EventDetailResponse> unpublishEvent(
            @Parameter(description = "행사 ID") @PathVariable Long eventId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행사 비공개 요청 - eventId: {}, userId: {}", eventId, user.userId());
        EventDetailResponse response = eventService.unpublishEvent(eventId, user.userId());
        return ResponseEntity.ok(response);
    }
}
