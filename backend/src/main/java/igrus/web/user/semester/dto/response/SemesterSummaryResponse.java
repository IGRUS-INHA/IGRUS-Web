package igrus.web.user.semester.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학기 요약 정보 응답")
public record SemesterSummaryResponse(
        @Schema(description = "연도", example = "2026")
        int year,
        @Schema(description = "학기 (1 또는 2)", example = "1")
        int semester,
        @Schema(description = "회원 수", example = "30")
        long memberCount,
        @Schema(description = "표시 이름", example = "2026년 1학기")
        String displayName
) {
    public static SemesterSummaryResponse of(int year, int semester, long memberCount) {
        String displayName = year + "년 " + semester + "학기";
        return new SemesterSummaryResponse(year, semester, memberCount, displayName);
    }
}
