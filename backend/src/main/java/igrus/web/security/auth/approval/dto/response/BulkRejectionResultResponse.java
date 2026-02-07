package igrus.web.security.auth.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "준회원 일괄 거절 결과 응답")
public record BulkRejectionResultResponse(
        @Schema(description = "거절 성공 수", example = "3")
        int rejectedCount,

        @Schema(description = "거절 실패 수", example = "1")
        int failedCount,

        @Schema(description = "총 요청 수", example = "4")
        int totalRequested
) {}
