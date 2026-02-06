package igrus.web.admin.controller;

import igrus.web.admin.dto.response.DashboardResponse;
import igrus.web.admin.service.DashboardService;
import igrus.web.common.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@Tag(name = "Admin Dashboard", description = "관리자 대시보드 API")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "대시보드 통계 조회", description = "관리자 대시보드의 통계 데이터를 조회합니다. OPERATOR 이상 권한이 필요합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = DashboardResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (OPERATOR 이상 필요)",
                    content = @Content
            )
    })
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        DashboardResponse response = dashboardService.getDashboard();
        return ResponseEntity.ok(response);
    }
}
