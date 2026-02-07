package igrus.web.admin.dashboard.dto;

/**
 * 관리자 대시보드 통계 응답 DTO.
 *
 * @param todayPostCount                오늘 게시글 수 (삭제 제외)
 * @param todayCommentCount             오늘 댓글 수 (삭제 제외)
 * @param weeklyApprovedMemberCount     이번 주 정회원 승인 수 (월요일 기준, MEMBER 역할로 변경된 이력 기준)
 * @param pendingInquiryCount           대기 중 문의 수
 * @param pendingAssociateCount         승인 대기 준회원 수
 */
public record DashboardStatsResponse(
        long todayPostCount,
        long todayCommentCount,
        long weeklyApprovedMemberCount,
        long pendingInquiryCount,
        long pendingAssociateCount
) {
}
