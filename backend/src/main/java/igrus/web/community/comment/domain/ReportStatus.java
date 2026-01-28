package igrus.web.community.comment.domain;

/**
 * 댓글 신고 처리 상태.
 */
public enum ReportStatus {
    PENDING,    // 검토 대기
    RESOLVED,   // 처리 완료 (승인)
    DISMISSED   // 반려
}
