package igrus.web.community.comment.service.support;

import igrus.web.community.comment.domain.ReportStatus;
import igrus.web.community.comment.dto.response.CommentReportResponse;
import igrus.web.community.comment.repository.CommentReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 대기 중인 댓글 신고 목록 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetPendingReportsService {

    private final CommentReportRepository commentReportRepository;

    /**
     * 대기 중인 신고 목록을 조회합니다.
     *
     * @return 신고 목록
     */
    @Transactional(readOnly = true)
    public List<CommentReportResponse> getPendingReports() {
        return commentReportRepository.findByStatus(ReportStatus.PENDING)
                .stream()
                .map(CommentReportResponse::from)
                .toList();
    }
}
