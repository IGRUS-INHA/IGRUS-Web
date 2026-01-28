package igrus.web.community.comment.controller;

import igrus.web.community.comment.dto.request.CreateCommentReportRequest;
import igrus.web.community.comment.dto.request.UpdateReportStatusRequest;
import igrus.web.community.comment.dto.response.CommentReportResponse;
import igrus.web.community.comment.service.CommentReportService;
import igrus.web.common.exception.ErrorResponse;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 댓글 신고 컨트롤러.
 * 댓글 신고 및 관리자 신고 처리 API를 제공합니다.
 */
@Tag(name = "Comment Report", description = "댓글 신고 API")
@Slf4j
@RestController
@RequiredArgsConstructor
public class CommentReportController {

    private final CommentReportService commentReportService;

    @Operation(
            summary = "댓글 신고",
            description = "댓글을 신고합니다. 동일 사용자가 동일 댓글을 중복 신고할 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "신고 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentReportResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (신고 사유 누락, 중복 신고)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "댓글을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/api/v1/comments/{commentId}/reports")
    public ResponseEntity<CommentReportResponse> reportComment(
            @Parameter(description = "댓글 ID", example = "1")
            @PathVariable Long commentId,
            @Valid @RequestBody CreateCommentReportRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("댓글 신고 요청 - commentId: {}, userId: {}", commentId, user.userId());

        CommentReportResponse response = commentReportService.reportComment(commentId, request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "신고 목록 조회 (관리자)",
            description = "대기 중인 신고 목록을 조회합니다. OPERATOR 이상 권한이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "신고 목록 조회 성공"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (OPERATOR 이상 필요)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @GetMapping("/api/v1/admin/comment-reports")
    public ResponseEntity<List<CommentReportResponse>> getPendingReports(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("신고 목록 조회 요청 - userId: {}", user.userId());

        List<CommentReportResponse> reports = commentReportService.getPendingReports();
        return ResponseEntity.ok(reports);
    }

    @Operation(
            summary = "신고 처리 (관리자)",
            description = "신고를 처리합니다 (승인/반려). OPERATOR 이상 권한이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "신고 처리 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (상태 누락)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (OPERATOR 이상 필요)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "신고를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    @PatchMapping("/api/v1/admin/comment-reports/{reportId}")
    public ResponseEntity<Void> updateReportStatus(
            @Parameter(description = "신고 ID", example = "1")
            @PathVariable Long reportId,
            @Valid @RequestBody UpdateReportStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("신고 처리 요청 - reportId: {}, status: {}, userId: {}",
                reportId, request.getStatus(), user.userId());

        commentReportService.updateReportStatus(reportId, request, user.userId());
        return ResponseEntity.noContent().build();
    }
}
