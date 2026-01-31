package igrus.web.community.comment.service;

import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.domain.CommentReport;
import igrus.web.community.comment.domain.ReportStatus;
import igrus.web.community.comment.dto.request.CreateCommentReportRequest;
import igrus.web.community.comment.dto.request.UpdateReportStatusRequest;
import igrus.web.community.comment.dto.response.CommentReportResponse;
import igrus.web.community.comment.exception.CommentNotFoundException;
import igrus.web.community.comment.exception.CommentReportException;
import igrus.web.community.comment.repository.CommentReportRepository;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 댓글 신고 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentReportService {

    private final CommentRepository commentRepository;
    private final CommentReportRepository commentReportRepository;
    private final UserRepository userRepository;

    /**
     * 댓글을 신고합니다.
     *
     * @param commentId  댓글 ID
     * @param request    신고 요청
     * @param reporterId 신고자 ID
     * @return 신고 응답
     */
    @Transactional
    public CommentReportResponse reportComment(Long commentId, CreateCommentReportRequest request, Long reporterId) {
        Comment comment = findCommentById(commentId);
        User reporter = findUserById(reporterId);

        validateNotAlreadyReported(commentId, reporterId);

        CommentReport report = CommentReport.create(comment, reporter, request.getReason());
        CommentReport savedReport = commentReportRepository.save(report);

        return CommentReportResponse.from(savedReport);
    }

    /**
     * 대기 중인 신고 목록을 조회합니다.
     *
     * @return 신고 목록
     */
    public List<CommentReportResponse> getPendingReports() {
        return commentReportRepository.findByStatus(ReportStatus.PENDING)
                .stream()
                .map(CommentReportResponse::from)
                .toList();
    }

    /**
     * 신고 상태를 업데이트합니다.
     *
     * @param reportId 신고 ID
     * @param request  상태 업데이트 요청
     * @param adminId  처리하는 관리자 ID
     */
    @Transactional
    public void updateReportStatus(Long reportId, UpdateReportStatusRequest request, Long adminId) {
        CommentReport report = findReportById(reportId);
        User admin = findUserById(adminId);

        if (request.getStatus() == ReportStatus.RESOLVED) {
            report.resolve(admin);
        } else if (request.getStatus() == ReportStatus.DISMISSED) {
            report.dismiss(admin);
        }
    }

    // === Private Helper Methods ===

    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    private CommentReport findReportById(Long reportId) {
        return commentReportRepository.findById(reportId)
                .orElseThrow(CommentReportException::reportNotFound);
    }

    private void validateNotAlreadyReported(Long commentId, Long reporterId) {
        if (commentReportRepository.existsByCommentIdAndReporterId(commentId, reporterId)) {
            throw CommentReportException.alreadyReported();
        }
    }
}
