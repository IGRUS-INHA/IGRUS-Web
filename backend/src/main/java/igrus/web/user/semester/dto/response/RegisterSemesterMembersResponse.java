package igrus.web.user.semester.dto.response;

public record RegisterSemesterMembersResponse(
        int registeredCount,
        int skippedCount,
        int totalRequested
) {}
