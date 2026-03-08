package igrus.web.community.comment.controller;

import igrus.web.common.util.EnumUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.community.comment.domain.ReportStatus;
import igrus.web.community.comment.dto.request.CreateCommentReportRequest;
import igrus.web.community.comment.dto.request.UpdateReportStatusRequest;
import igrus.web.community.comment.dto.response.CommentReportResponse;
import igrus.web.community.comment.service.support.GetPendingReportsService;
import igrus.web.community.comment.service.support.ReportCommentService;
import igrus.web.community.comment.service.support.UpdateReportStatusService;
import igrus.web.generated.api.CommentReportApi;
import igrus.web.generated.model.ApiCommentReportResponse;
import igrus.web.generated.model.ApiCreateCommentReportRequest;
import igrus.web.generated.model.ApiUpdateReportStatusRequest;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 댓글 신고 컨트롤러.
 * 댓글 신고 및 관리자 신고 처리 API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CommentReportController implements CommentReportApi {

    private final ReportCommentService reportCommentService;
    private final GetPendingReportsService getPendingReportsService;
    private final UpdateReportStatusService updateReportStatusService;

    @Override
    public ResponseEntity<ApiCommentReportResponse> reportComment(
            Long commentId, ApiCreateCommentReportRequest createCommentReportRequest) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("댓글 신고 요청 - commentId: {}, userId: {}", commentId, user.userId());

        var internalRequest =
                new CreateCommentReportRequest(createCommentReportRequest.getReason());

        var result = reportCommentService.reportComment(commentId, internalRequest, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(result));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<ApiCommentReportResponse>> getPendingReports() {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("신고 목록 조회 요청 - userId: {}", user.userId());

        var reports = getPendingReportsService.getPendingReports();
        List<ApiCommentReportResponse> response = reports.stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> updateReportStatus(
            Long reportId, ApiUpdateReportStatusRequest updateReportStatusRequest) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("신고 처리 요청 - reportId: {}, status: {}, userId: {}",
                reportId, updateReportStatusRequest.getStatus(), user.userId());

        ReportStatus status = EnumUtils.fromStringOrNull(ReportStatus.class, updateReportStatusRequest.getStatus().getValue());
        var internalRequest =
                new UpdateReportStatusRequest(status);

        updateReportStatusService.updateReportStatus(reportId, internalRequest, user.userId());
        return ResponseEntity.noContent().build();
    }

    private ApiCommentReportResponse mapToResponse(CommentReportResponse result) {
        return new ApiCommentReportResponse()
                .id(result.getId())
                .commentId(result.getCommentId())
                .commentContent(result.getCommentContent())
                .reporterId(result.getReporterId())
                .reporterName(result.getReporterName())
                .reason(result.getReason())
                .status(ApiCommentReportResponse.StatusEnum.fromValue(result.getStatus().name()))
                .createdAt(result.getCreatedAt())
                .resolvedAt(result.getResolvedAt())
                .resolvedById(result.getResolvedById())
                .resolvedByName(result.getResolvedByName());
    }
}
