package igrus.web.security.auth.approval.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkApprovalRequest(
    @NotEmpty(message = "승인할 사용자 목록은 필수입니다")
    List<Long> userIds,

    @Size(max = 255, message = "승인 사유는 255자 이내여야 합니다")
    String reason
) {}
