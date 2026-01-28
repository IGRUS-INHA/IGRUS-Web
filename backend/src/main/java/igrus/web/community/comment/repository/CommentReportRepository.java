package igrus.web.community.comment.repository;

import igrus.web.community.comment.domain.CommentReport;
import igrus.web.community.comment.domain.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 댓글 신고 리포지토리.
 */
public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    /**
     * 특정 상태의 신고 목록을 조회합니다.
     *
     * @param status 신고 상태
     * @return 신고 목록
     */
    List<CommentReport> findByStatus(ReportStatus status);

    /**
     * 특정 댓글의 신고 목록을 조회합니다.
     *
     * @param commentId 댓글 ID
     * @return 신고 목록
     */
    List<CommentReport> findByCommentId(Long commentId);

    /**
     * 특정 사용자가 특정 댓글을 이미 신고했는지 확인합니다.
     *
     * @param commentId  댓글 ID
     * @param reporterId 신고자 ID
     * @return 이미 신고했으면 true
     */
    boolean existsByCommentIdAndReporterId(Long commentId, Long reporterId);
}
