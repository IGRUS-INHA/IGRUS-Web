package igrus.web.security.auth.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일괄 승인 결과 응답")
public record BulkApprovalResultResponse(
        @Schema(description = "승인 성공 건수", example = "5")
        int approvedCount,

        @Schema(description = "승인 실패 건수", example = "1")
        int failedCount,

        @Schema(description = "총 요청 건수", example = "6")
        int totalRequested
) {}
