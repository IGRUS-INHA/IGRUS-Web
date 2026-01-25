package igrus.web.security.auth.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일괄 가입 승인 결과 응답")
public record BulkApprovalResultResponse(
        @Schema(description = "승인 성공한 사용자 수", example = "5")
        int approvedCount,

        @Schema(description = "승인 실패한 사용자 수", example = "1")
        int failedCount,

        @Schema(description = "승인 요청된 총 사용자 수", example = "6")
        int totalRequested
) {
}
