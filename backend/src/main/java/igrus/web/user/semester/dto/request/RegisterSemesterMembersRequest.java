package igrus.web.user.semester.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RegisterSemesterMembersRequest(
        @NotEmpty(message = "등록할 회원을 선택해주세요")
        List<Long> userIds
) {}
