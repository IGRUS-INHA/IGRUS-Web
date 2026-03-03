package igrus.web.user.semester.dto.response;

public record SemesterSummaryResponse(
        int year,
        int semester,
        long memberCount,
        String displayName
) {
    public static SemesterSummaryResponse of(int year, int semester, long memberCount) {
        String displayName = year + "년 " + semester + "학기";
        return new SemesterSummaryResponse(year, semester, memberCount, displayName);
    }
}
