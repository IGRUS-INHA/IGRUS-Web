package igrus.web.community.comment.service.support;

import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.domain.CommentReport;
import igrus.web.community.comment.dto.request.CreateCommentReportRequest;
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

/**
 * 댓글 신고 접수 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReportCommentService {

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
    public CommentReportResponse reportComment(Long commentId, CreateCommentReportRequest request, Long reporterId) {
        Comment comment = findCommentById(commentId);
        User reporter = findUserById(reporterId);

        validateNotAlreadyReported(commentId, reporterId);

        CommentReport report = CommentReport.create(comment, reporter, request.getReason());
        CommentReport savedReport = commentReportRepository.save(report);

        return CommentReportResponse.from(savedReport);
    }

    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    private void validateNotAlreadyReported(Long commentId, Long reporterId) {
        if (commentReportRepository.existsByCommentIdAndReporterId(commentId, reporterId)) {
            throw CommentReportException.alreadyReported();
        }
    }
}
