package igrus.web.security.auth.approval.dto.request;

import jakarta.validation.constraints.Size;

public record MemberApprovalRequest(
    @Size(max = 255, message = "승인 사유는 255자 이내여야 합니다")
    String reason
) {}
