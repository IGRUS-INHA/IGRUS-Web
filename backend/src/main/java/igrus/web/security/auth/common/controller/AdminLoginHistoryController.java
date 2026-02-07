package igrus.web.security.auth.common.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.security.auth.common.dto.response.LoginHistoryResponse;
import igrus.web.security.auth.common.service.login.GetLoginHistoryForAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/login-histories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Login History", description = "관리자 로그인 이력 조회 API (ADMIN 전용)")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
public class AdminLoginHistoryController {

    private final GetLoginHistoryForAdminService getLoginHistoryForAdminService;

    @Operation(
            summary = "로그인 이력 조회",
            description = "로그인 이력을 복합 필터로 조회합니다. 모든 필터는 선택적입니다. ADMIN 권한이 필요합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요 (로그인하지 않음)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (ADMIN 권한 필요)",
                    content = @Content
            )
    })
    @GetMapping
    public ResponseEntity<Page<LoginHistoryResponse>> getLoginHistories(
            @Parameter(description = "학번 필터", example = "12345678")
            @RequestParam(required = false) String studentId,

            @Parameter(description = "성공 여부 필터", example = "true")
            @RequestParam(required = false) Boolean success,

            @Parameter(description = "IP 주소 필터", example = "192.168.1.100")
            @RequestParam(required = false) String ipAddress,

            @Parameter(description = "조회 시작일 (ISO 8601)", example = "2024-01-01T00:00:00Z")
            @RequestParam(required = false) Instant startDate,

            @Parameter(description = "조회 종료일 (ISO 8601)", example = "2024-12-31T23:59:59Z")
            @RequestParam(required = false) Instant endDate,

            @ParameterObject @PageableDefault(size = 20, sort = "attemptedAt", direction = Sort.Direction.DESC)
            Pageable pageable,

            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        Page<LoginHistoryResponse> loginHistories = getLoginHistoryForAdminService.getLoginHistories(
                authenticatedUser.userId(), studentId, success, ipAddress, startDate, endDate, pageable
        );
        return ResponseEntity.ok(loginHistories);
    }
}
