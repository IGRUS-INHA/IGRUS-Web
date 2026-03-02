package igrus.web.admin.dashboard.controller;

import igrus.web.admin.dashboard.dto.DashboardStatsResponse;
import igrus.web.admin.dashboard.service.GetDashboardStatsService;
import igrus.web.generated.api.AdminDashboardApi;
import igrus.web.generated.model.GetDashboardStats200Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController implements AdminDashboardApi {

    private final GetDashboardStatsService getDashboardStatsService;

    @Override
    public ResponseEntity<GetDashboardStats200Response> getDashboardStats() {
        DashboardStatsResponse internal = getDashboardStatsService.getDashboardStats();

        GetDashboardStats200Response response = new GetDashboardStats200Response()
                .todayPostCount(internal.todayPostCount())
                .todayCommentCount(internal.todayCommentCount())
                .weeklyApprovedMemberCount(internal.weeklyApprovedMemberCount())
                .pendingInquiryCount(internal.pendingInquiryCount())
                .pendingAssociateCount(internal.pendingAssociateCount());

        return ResponseEntity.ok(response);
    }
}
