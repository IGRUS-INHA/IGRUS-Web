package igrus.web.event.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.event.dto.response.MyRegistrationResponse;
import igrus.web.event.dto.response.RegistrationListResponse;
import igrus.web.event.dto.response.RegistrationResponse;
import igrus.web.event.service.EventRegistrationService;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 행사 신청 컨트롤러.
 * 행사 신청, 취소, 조회, 승인/거절 API를 제공합니다.
 */
@Tag(name = "Event Registration", description = "행사 신청 API")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EventRegistrationController {

    private final EventRegistrationService eventRegistrationService;

    // ===== 신청자용 API =====

    @Operation(summary = "행사 신청", description = "행사에 신청합니다. 정회원 이상만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "신청 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegistrationResponse.class))),
            @ApiResponse(responseCode = "400", description = "신청 불가 (정원 초과, 기간 외 등)"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (준회원)"),
            @ApiResponse(responseCode = "404", description = "행사를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 신청함")
    })
    @PostMapping("/events/{eventId}/registrations")
    public ResponseEntity<RegistrationResponse> registerEvent(
            @Parameter(description = "행사 ID") @PathVariable Long eventId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("행사 신청 요청 - eventId: {}, userId: {}", eventId, user.userId());
        RegistrationResponse response = eventRegistrationService.registerEvent(eventId, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "신청 취소", description = "행사 신청을 취소합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "취소 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegistrationResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "신청을 찾을 수 없음")
    })
    @DeleteMapping("/events/{eventId}/registrations")
    public ResponseEntity<RegistrationResponse> cancelRegistration(
            @Parameter(description = "행사 ID") @PathVariable Long eventId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("신청 취소 요청 - eventId: {}, userId: {}", eventId, user.userId());
        RegistrationResponse response = eventRegistrationService.cancelRegistration(eventId, user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 신청 목록 조회", description = "내가 신청한 행사 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/my/registrations")
    public ResponseEntity<List<MyRegistrationResponse>> getMyRegistrations(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("내 신청 목록 조회 요청 - userId: {}", user.userId());
        List<MyRegistrationResponse> response = eventRegistrationService.getMyRegistrations(user.userId());
        return ResponseEntity.ok(response);
    }

    // ===== 관리자용 API =====

    @Operation(summary = "신청자 목록 조회", description = "행사 신청자 목록을 조회합니다. 작성자 또는 관리자만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "행사를 찾을 수 없음")
    })
    @GetMapping("/events/{eventId}/registrations")
    public ResponseEntity<Page<RegistrationListResponse>> getRegistrationList(
            @Parameter(description = "행사 ID") @PathVariable Long eventId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @ParameterObject @PageableDefault(size = 20, sort = "registeredAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("신청자 목록 조회 요청 - eventId: {}, userId: {}", eventId, user.userId());
        Page<RegistrationListResponse> response = eventRegistrationService.getRegistrationList(eventId, user.userId(), pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "신청 승인", description = "신청을 승인합니다. (선발제) 작성자 또는 관리자만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "승인 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegistrationResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "신청을 찾을 수 없음")
    })
    @PostMapping("/registrations/{registrationId}/approve")
    public ResponseEntity<RegistrationResponse> approveRegistration(
            @Parameter(description = "신청 ID") @PathVariable Long registrationId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("신청 승인 요청 - registrationId: {}, userId: {}", registrationId, user.userId());
        RegistrationResponse response = eventRegistrationService.approveRegistration(registrationId, user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "신청 거절", description = "신청을 거절합니다. (선발제) 작성자 또는 관리자만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "거절 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegistrationResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "신청을 찾을 수 없음")
    })
    @PostMapping("/registrations/{registrationId}/reject")
    public ResponseEntity<RegistrationResponse> rejectRegistration(
            @Parameter(description = "신청 ID") @PathVariable Long registrationId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("신청 거절 요청 - registrationId: {}, userId: {}", registrationId, user.userId());
        RegistrationResponse response = eventRegistrationService.rejectRegistration(registrationId, user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "승인/거절 되돌리기", description = "승인 또는 거절한 신청을 대기 상태로 되돌립니다. (선발제) 운영진 이상만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "되돌리기 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegistrationResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "신청을 찾을 수 없음")
    })
    @PostMapping("/registrations/{registrationId}/revert")
    public ResponseEntity<RegistrationResponse> revertRegistration(
            @Parameter(description = "신청 ID") @PathVariable Long registrationId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("승인/거절 되돌리기 요청 - registrationId: {}, userId: {}", registrationId, user.userId());
        RegistrationResponse response = eventRegistrationService.revertRegistration(registrationId, user.userId());
        return ResponseEntity.ok(response);
    }
}
