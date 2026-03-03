package igrus.web.security.auth.approval.dto.response;

public record BulkApprovalResultResponse(
        int approvedCount,

        int failedCount,

        int totalRequested
) {}
