package igrus.web.user.semester.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학기별 회원 등록 결과 응답")
public record RegisterSemesterMembersResponse(
        @Schema(description = "등록된 회원 수", example = "5")
        int registeredCount,
        @Schema(description = "이미 등록되어 건너뛴 회원 수", example = "2")
        int skippedCount,
        @Schema(description = "요청된 총 회원 수", example = "7")
        int totalRequested
) {}
