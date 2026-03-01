package igrus.web.community.comment.controller;

import igrus.web.common.util.SecurityUtils;
import igrus.web.community.comment.domain.ReportStatus;
import igrus.web.community.comment.dto.request.CreateCommentReportRequest;
import igrus.web.community.comment.dto.response.CommentReportResponse;
import igrus.web.community.comment.service.support.GetPendingReportsService;
import igrus.web.community.comment.service.support.ReportCommentService;
import igrus.web.community.comment.service.support.UpdateReportStatusService;
import igrus.web.generated.api.CommentReportApi;
import igrus.web.generated.model.ReopenRegistrationRequest;
import igrus.web.generated.model.ReportComment201Response;
import igrus.web.generated.model.UpdateReportStatusRequest;
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
    public ResponseEntity<ReportComment201Response> reportComment(
            Long commentId, ReopenRegistrationRequest reopenRegistrationRequest) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("댓글 신고 요청 - commentId: {}, userId: {}", commentId, user.userId());

        CreateCommentReportRequest internalRequest =
                new CreateCommentReportRequest(reopenRegistrationRequest.getReason());

        CommentReportResponse result = reportCommentService.reportComment(commentId, internalRequest, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(result));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<ReportComment201Response>> getPendingReports() {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("신고 목록 조회 요청 - userId: {}", user.userId());

        List<CommentReportResponse> reports = getPendingReportsService.getPendingReports();
        List<ReportComment201Response> response = reports.stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> updateReportStatus(
            Long reportId, UpdateReportStatusRequest updateReportStatusRequest) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("신고 처리 요청 - reportId: {}, status: {}, userId: {}",
                reportId, updateReportStatusRequest.getStatus(), user.userId());

        ReportStatus status = ReportStatus.valueOf(updateReportStatusRequest.getStatus().getValue());
        igrus.web.community.comment.dto.request.UpdateReportStatusRequest internalRequest =
                new igrus.web.community.comment.dto.request.UpdateReportStatusRequest(status);

        updateReportStatusService.updateReportStatus(reportId, internalRequest, user.userId());
        return ResponseEntity.noContent().build();
    }

    private ReportComment201Response mapToResponse(CommentReportResponse result) {
        return new ReportComment201Response()
                .id(result.getId())
                .commentId(result.getCommentId())
                .commentContent(result.getCommentContent())
                .reporterId(result.getReporterId())
                .reporterName(result.getReporterName())
                .reason(result.getReason())
                .status(ReportComment201Response.StatusEnum.fromValue(result.getStatus().name()))
                .createdAt(result.getCreatedAt())
                .resolvedAt(result.getResolvedAt())
                .resolvedById(result.getResolvedById())
                .resolvedByName(result.getResolvedByName());
    }
}
