package igrus.web.event.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.common.exception.ErrorResponse;
import igrus.web.event.dto.request.CreateEventRequest;
import igrus.web.event.dto.request.UpdateEventRequest;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    // TODO: EventService 주입
    // private final EventService eventService;

    // ===== 행사 CRUD =====

    @Operation(
            summary = "행사 생성",
            description = "새로운 행사를 생성합니다. 운영진 이상 권한 필요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "행사 생성 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @PostMapping
    public ResponseEntity<Void> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행사 생성 요청 - userId: {}, title: {}", user.userId(), request.title());
        // TODO: eventService.createEvent(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(
            summary = "행사 목록 조회",
            description = "행사 목록을 페이징하여 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "행사 목록 조회 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PreAuthorize("hasAnyRole('MEMBER', 'OPERATOR', 'ADMIN')")
    @GetMapping
    public ResponseEntity<Void> getEventList(
            @Parameter(description = "행사 상태 필터") @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행사 목록 조회 요청 - userId: {}, status: {}", user.userId(), status);
        // TODO: eventService.getEventList(status, pageable, user);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "행사 상세 조회",
            description = "행사의 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "행사 상세 조회 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "행사를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PreAuthorize("hasAnyRole('MEMBER', 'OPERATOR', 'ADMIN')")
    @GetMapping("/{eventId}")
    public ResponseEntity<Void> getEventDetail(
            @Parameter(description = "행사 ID") @PathVariable Long eventId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행사 상세 조회 요청 - eventId: {}, userId: {}", eventId, user.userId());
        // TODO: eventService.getEventDetail(eventId, user);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "행사 수정",
            description = "행사 정보를 수정합니다. 운영진 이상 권한 필요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "행사 수정 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 또는 수정 불가능한 상태",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "행사를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @PutMapping("/{eventId}")
    public ResponseEntity<Void> updateEvent(
            @Parameter(description = "행사 ID") @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행사 수정 요청 - eventId: {}, userId: {}", eventId, user.userId());
        // TODO: eventService.updateEvent(eventId, request, user);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "행사 삭제",
            description = "행사를 삭제합니다. 운영진 이상 권한 필요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "행사 삭제 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "행사를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @Parameter(description = "행사 ID") @PathVariable Long eventId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행사 삭제 요청 - eventId: {}, userId: {}", eventId, user.userId());
        // TODO: eventService.deleteEvent(eventId, user);
        return ResponseEntity.noContent().build();
    }

    // ===== 행사 상태 관리 =====

    @Operation(
            summary = "행사 수동 마감",
            description = "행사를 수동으로 마감합니다. 운영진 이상 권한 필요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "행사 마감 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "마감 불가능한 상태",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @PostMapping("/{eventId}/close")
    public ResponseEntity<Void> closeEvent(
            @Parameter(description = "행사 ID") @PathVariable Long eventId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행사 마감 요청 - eventId: {}, userId: {}", eventId, user.userId());
        // TODO: eventService.closeEvent(eventId, user);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "행사 취소",
            description = "행사를 취소합니다. 운영진 이상 권한 필요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "행사 취소 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "취소 불가능한 상태",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @PostMapping("/{eventId}/cancel")
    public ResponseEntity<Void> cancelEvent(
            @Parameter(description = "행사 ID") @PathVariable Long eventId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행사 취소 요청 - eventId: {}, userId: {}", eventId, user.userId());
        // TODO: eventService.cancelEvent(eventId, user);
        return ResponseEntity.ok().build();
    }
}
