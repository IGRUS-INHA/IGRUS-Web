package igrus.web.community.comment.service.support;

import igrus.web.community.comment.domain.CommentReport;
import igrus.web.community.comment.domain.ReportStatus;
import igrus.web.community.comment.dto.request.UpdateReportStatusRequest;
import igrus.web.community.comment.exception.CommentReportException;
import igrus.web.community.comment.repository.CommentReportRepository;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 신고 상태 업데이트 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateReportStatusService {

    private final CommentReportRepository commentReportRepository;
    private final UserRepository userRepository;

    /**
     * 신고 상태를 업데이트합니다.
     *
     * @param reportId 신고 ID
     * @param request  상태 업데이트 요청
     * @param adminId  처리하는 관리자 ID
     */
    public void updateReportStatus(Long reportId, UpdateReportStatusRequest request, Long adminId) {
        CommentReport report = findReportById(reportId);
        User admin = findUserById(adminId);

        if (request.getStatus() == ReportStatus.RESOLVED) {
            report.resolve(admin);
        } else if (request.getStatus() == ReportStatus.DISMISSED) {
            report.dismiss(admin);
        }
    }

    private CommentReport findReportById(Long reportId) {
        return commentReportRepository.findById(reportId)
                .orElseThrow(CommentReportException::reportNotFound);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }
}
