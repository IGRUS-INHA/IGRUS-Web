package igrus.web.community.comment.dto.response;

import igrus.web.community.comment.domain.CommentReport;
import igrus.web.community.comment.domain.ReportStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 댓글 신고 응답 DTO.
 */
@Getter
@Builder
public class CommentReportResponse {

    private Long id;
    private Long commentId;
    private String commentContent;
    private Long reporterId;
    private String reporterName;
    private String reason;
    private ReportStatus status;
    private Instant createdAt;
    private Instant resolvedAt;
    private Long resolvedById;
    private String resolvedByName;

    /**
     * CommentReport 엔티티를 CommentReportResponse로 변환합니다.
     *
     * @param report 신고 엔티티
     * @return CommentReportResponse
     */
    public static CommentReportResponse from(CommentReport report) {
        return CommentReportResponse.builder()
                .id(report.getId())
                .commentId(report.getComment().getId())
                .commentContent(report.getComment().getContent())
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getName())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .resolvedById(report.getResolvedBy() != null ? report.getResolvedBy().getId() : null)
                .resolvedByName(report.getResolvedBy() != null ? report.getResolvedBy().getName() : null)
                .build();
    }
}
