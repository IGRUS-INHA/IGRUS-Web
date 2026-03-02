package igrus.web.security.auth.approval.dto.response;

public record BulkRejectionResultResponse(
        int rejectedCount,

        int failedCount,

        int totalRequested
) {}
