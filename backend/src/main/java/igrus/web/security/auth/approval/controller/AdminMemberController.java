package igrus.web.security.auth.approval.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.security.auth.approval.dto.request.BulkApprovalRequest;
import igrus.web.security.auth.approval.dto.request.BulkRejectionRequest;
import igrus.web.security.auth.approval.dto.request.RejectAssociateRequest;
import igrus.web.security.auth.approval.dto.response.AssociateInfoResponse;
import igrus.web.security.auth.approval.dto.response.BulkApprovalResultResponse;
import igrus.web.security.auth.approval.dto.response.BulkRejectionResultResponse;
import igrus.web.security.auth.approval.dto.response.RejectedAssociateInfoResponse;
import igrus.web.security.auth.approval.service.read.GetPendingAssociatesService;
import igrus.web.security.auth.approval.service.read.GetRejectedAssociatesService;
import igrus.web.security.auth.approval.service.write.ApproveAssociateService;
import igrus.web.security.auth.approval.service.write.BulkApproveAssociatesService;
import igrus.web.security.auth.approval.service.write.BulkRejectAssociatesService;
import igrus.web.security.auth.approval.service.write.RejectAssociateService;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/associates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Associate Approval", description = "관리자 준회원 승인 API (ADMIN 전용)")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
public class AdminMemberController {

    private final GetPendingAssociatesService getPendingAssociatesService;
    private final GetRejectedAssociatesService getRejectedAssociatesService;
    private final ApproveAssociateService approveAssociateService;
    private final BulkApproveAssociatesService bulkApproveAssociatesService;
    private final RejectAssociateService rejectAssociateService;
    private final BulkRejectAssociatesService bulkRejectAssociatesService;

    @Operation(
            summary = "승인 대기 준회원 목록 조회",
            description = "승인 대기 중인 준회원 목록을 페이지네이션하여 조회합니다. ADMIN 권한이 필요합니다."
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
    public ResponseEntity<Page<AssociateInfoResponse>> getPendingAssociates(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        Page<AssociateInfoResponse> pendingAssociates = getPendingAssociatesService.getPendingAssociates(
                pageable,
                authenticatedUser.userId()
        );
        return ResponseEntity.ok(pendingAssociates);
    }

    @Operation(
            summary = "개별 준회원 승인",
            description = "특정 준회원을 정회원으로 승인합니다. ADMIN 권한이 필요합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "승인 성공"
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
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "해당 사용자가 준회원이 아님",
                    content = @Content
            )
    })
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveAssociate(
            @Parameter(description = "승인할 사용자 ID", required = true, example = "1") @PathVariable("id") Long userId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        approveAssociateService.approveAssociate(userId, authenticatedUser.userId());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "준회원 일괄 승인",
            description = "여러 준회원을 한 번에 정회원으로 승인합니다. ADMIN 권한이 필요합니다. " +
                    "일부 사용자 승인이 실패해도 나머지는 정상 처리됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "일괄 승인 처리 완료 (부분 성공 가능)",
                    content = @Content(schema = @Schema(implementation = BulkApprovalResultResponse.class))
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
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "승인할 사용자 목록이 비어있음",
                    content = @Content
            )
    })
    @PostMapping("/approve-batch")
    public ResponseEntity<BulkApprovalResultResponse> approveBulk(
            @Valid @RequestBody BulkApprovalRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        int approvedCount = bulkApproveAssociatesService.approveBulk(
                request.userIds(),
                authenticatedUser.userId()
        );

        int totalRequested = request.userIds().size();
        int failedCount = totalRequested - approvedCount;

        BulkApprovalResultResponse response = new BulkApprovalResultResponse(
                approvedCount,
                failedCount,
                totalRequested
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "개별 준회원 거절",
            description = "특정 준회원의 가입을 거절합니다. ADMIN 권한이 필요합니다. 거절 사유는 필수입니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "거절 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "해당 사용자가 준회원이 아니거나 이미 처리됨",
                    content = @Content
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
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content
            )
    })
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectAssociate(
            @Parameter(description = "거절할 사용자 ID", required = true, example = "1") @PathVariable("id") Long userId,
            @Valid @RequestBody RejectAssociateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        rejectAssociateService.rejectAssociate(userId, authenticatedUser.userId(), request.reason());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "준회원 일괄 거절",
            description = "여러 준회원의 가입을 한 번에 거절합니다. ADMIN 권한이 필요합니다. " +
                    "일부 사용자 거절이 실패해도 나머지는 정상 처리됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "일괄 거절 처리 완료 (부분 성공 가능)",
                    content = @Content(schema = @Schema(implementation = BulkRejectionResultResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "거절할 사용자 목록이 비어있음",
                    content = @Content
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
    @PostMapping("/reject-batch")
    public ResponseEntity<BulkRejectionResultResponse> rejectBulk(
            @Valid @RequestBody BulkRejectionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        int rejectedCount = bulkRejectAssociatesService.rejectBulk(
                request.userIds(),
                authenticatedUser.userId(),
                request.reason()
        );

        int totalRequested = request.userIds().size();
        int failedCount = totalRequested - rejectedCount;

        BulkRejectionResultResponse response = new BulkRejectionResultResponse(
                rejectedCount,
                failedCount,
                totalRequested
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "거절된 준회원 목록 조회",
            description = "거절된 준회원 목록을 페이지네이션하여 조회합니다. ADMIN 권한이 필요합니다."
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
    @GetMapping("/rejected")
    public ResponseEntity<Page<RejectedAssociateInfoResponse>> getRejectedAssociates(
            @ParameterObject @PageableDefault(size = 20, sort = "decidedAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        Page<RejectedAssociateInfoResponse> rejectedAssociates = getRejectedAssociatesService.getRejectedAssociates(
                pageable,
                authenticatedUser.userId()
        );
        return ResponseEntity.ok(rejectedAssociates);
    }
}
