package igrus.web.security.auth.approval.controller;

import igrus.web.security.auth.approval.dto.request.BulkApprovalRequest;
import igrus.web.security.auth.approval.dto.response.AssociateInfoResponse;
import igrus.web.security.auth.approval.dto.response.BulkApprovalResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin Member Management", description = "관리자 회원 관리 API (ADMIN 전용)")
@SecurityRequirement(name = "bearerAuth")
public interface AdminMemberControllerApi {

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
    ResponseEntity<Page<AssociateInfoResponse>> getPendingAssociates(
            @Parameter(description = "페이지 정보 (page, size, sort)") Pageable pageable
    );

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
    ResponseEntity<Void> approveAssociate(
            @Parameter(description = "승인할 사용자 ID", required = true, example = "1") Long userId
    );

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
    ResponseEntity<BulkApprovalResultResponse> approveBulk(BulkApprovalRequest request);
}
