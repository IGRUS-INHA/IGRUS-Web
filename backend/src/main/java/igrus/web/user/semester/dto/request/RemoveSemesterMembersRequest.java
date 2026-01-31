package igrus.web.user.semester.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "학기별 회원 제외 요청")
public record RemoveSemesterMembersRequest(
        @Schema(description = "제외할 회원 ID 목록", example = "[1, 2, 3]")
        @NotEmpty(message = "제외할 회원을 선택해주세요")
        List<Long> userIds
) {}
