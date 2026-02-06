package igrus.web.admin.dashboard.dto;

/**
 * 관리자 대시보드 통계 응답 DTO.
 *
 * @param todayPostCount         오늘 게시글 수 (삭제 제외)
 * @param todayCommentCount      오늘 댓글 수 (삭제 제외)
 * @param newMemberCount         이번 주 신규 가입자 수 (월요일 기준)
 * @param pendingInquiryCount    대기 중 문의 수
 * @param pendingAssociateCount  승인 대기 준회원 수
 */
public record DashboardStatsResponse(
        long todayPostCount,
        long todayCommentCount,
        long newMemberCount,
        long pendingInquiryCount,
        long pendingAssociateCount
) {
}
