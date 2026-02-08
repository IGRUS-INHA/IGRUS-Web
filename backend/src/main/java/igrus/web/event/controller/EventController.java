package igrus.web.event.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.common.exception.ErrorResponse;
import igrus.web.event.domain.EventStatus;
import igrus.web.event.dto.request.CreateEventRequest;
import igrus.web.event.dto.request.UpdateEventRequest;
import igrus.web.event.dto.response.EventCreateResponse;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 행사 컨트롤러.
 * 행사 생성, 조회, 수정, 삭제 및 상태 관리 API를 제공합니다.
 */
@Tag(name = "Event", description = "행사 API")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@Slf4j
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // ===== 행사 CRUD =====

    @Operation(summary = "행사 생성", description = "새로운 행사를 생성합니다. 운영진 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "행사 생성 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<EventCreateResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행사 생성 요청 - userId: {}, title: {}", user.userId(), request.title());
        EventCreateResponse response = eventService.createEvent(request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "행사 목록 조회", description = "행사 목록을 조회합니다. 상태별 필터링이 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "행사 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<EventListResponse>> getEventList(
            @Parameter(description = "행사 상태 필터 (UPCOMING, OPEN, CLOSED, ONGOING, COMPLETED)")
            @RequestParam(required = false) EventStatus status
    ) {
        log.info("행사 목록 조회 요청 - status: {}", status);
        List<EventListResponse> response = eventService.getEventList(status);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "행사 상세 조회", description = "행사의 상세 정보를 조회합니다. 준회원은 조회할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "행사 상세 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "준회원 접근 불가",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "행사를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailResponse> getEvent(
            @Parameter(description = "행사 ID") @PathVariable Long eventId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행사 상세 조회 요청 - eventId: {}, userId: {}", eventId, user.userId());
        EventDetailResponse response = eventService.getEvent(eventId, user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "행사 수정", description = "행사 정보를 수정합니다. 작성자 또는 관리자만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "행사 수정 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 수정 불가능한 상태",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "행사를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{eventId}")
    public ResponseEntity<EventDetailResponse> updateEvent(
            @Parameter(description = "행사 ID") @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행사 수정 요청 - eventId: {}, userId: {}", eventId, user.userId());
        EventDetailResponse response = eventService.updateEvent(eventId, request, user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "행사 삭제", description = "행사를 삭제합니다. 작성자 또는 관리자만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "행사 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "행사를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @Parameter(description = "행사 ID") @PathVariable Long eventId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행사 삭제 요청 - eventId: {}, userId: {}", eventId, user.userId());
        eventService.deleteEvent(eventId, user.userId());
        return ResponseEntity.noContent().build();
    }

    // ===== 행사 상태 관리 =====

    @Operation(summary = "행사 수동 마감", description = "행사를 수동으로 마감합니다. 작성자 또는 관리자만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "행사 마감 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "마감 불가능한 상태",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{eventId}/close")
    public ResponseEntity<EventDetailResponse> closeEvent(
            @Parameter(description = "행사 ID") @PathVariable Long eventId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행사 마감 요청 - eventId: {}, userId: {}", eventId, user.userId());
        EventDetailResponse response = eventService.closeEvent(eventId, user.userId());
        return ResponseEntity.ok(response);
    }

}
