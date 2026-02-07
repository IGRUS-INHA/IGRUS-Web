package igrus.web.user.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.dto.response.AccountStatusChangeHistoryResponse;
import igrus.web.user.service.GetAccountStatusChangeHistoryService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/account-status-histories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Account Status Change History", description = "계정 상태 변경 감사 이력 조회 API (ADMIN 전용)")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
public class AdminAccountStatusChangeHistoryController {

    private final GetAccountStatusChangeHistoryService getAccountStatusChangeHistoryService;

    @Operation(
            summary = "계정 상태 변경 감사 이력 조회",
            description = "계정 상태 변경 감사 이력을 조회합니다. 사용자, 변경자, 변경 유형, 기간으로 필터링 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 전용)", content = @Content)
    })
    @GetMapping
    public ResponseEntity<Page<AccountStatusChangeHistoryResponse>> getHistories(
            @Parameter(description = "대상 사용자 ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "변경자 사용자 ID") @RequestParam(required = false) Long changedByUserId,
            @Parameter(description = "변경 유형") @RequestParam(required = false) AccountChangeType changeType,
            @Parameter(description = "조회 시작일") @RequestParam(required = false) Instant startDate,
            @Parameter(description = "조회 종료일") @RequestParam(required = false) Instant endDate,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(
                getAccountStatusChangeHistoryService.getHistories(
                        userId, changedByUserId, changeType, startDate, endDate, pageable
                )
        );
    }
}
