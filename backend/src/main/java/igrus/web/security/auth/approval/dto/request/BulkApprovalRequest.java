package igrus.web.security.auth.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "일괄 준회원 승인 요청")
public record BulkApprovalRequest(
    @Schema(description = "승인할 사용자 ID 목록", example = "[1, 2, 3]")
    @NotEmpty(message = "승인할 사용자 목록은 필수입니다")
    List<Long> userIds,

    @Schema(description = "승인 사유 (선택)", example = "2026년 1월 일괄 승인")
    @Size(max = 255, message = "승인 사유는 255자 이내여야 합니다")
    String reason
) {}
