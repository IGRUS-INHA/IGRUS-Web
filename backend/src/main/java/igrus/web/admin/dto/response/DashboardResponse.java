package igrus.web.admin.dto.response;

/**
 * 관리자 대시보드 통계 응답 DTO.
 *
 * @param todayVisitors    오늘 방문자 수 (KST 기준)
 * @param todayPosts       오늘 게시글 수 (KST 기준)
 * @param weeklyNewUsers   주간 신규 회원 수
 * @param pendingInquiries 대기 중인 문의 수
 * @param pendingAssociates 승인 대기 준회원 수
 */
public record DashboardResponse(
        long todayVisitors,
        long todayPosts,
        long weeklyNewUsers,
        long pendingInquiries,
        long pendingAssociates
) {
}
