package igrus.web.security.auth.approval.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkRejectionRequest(
        @NotEmpty(message = "거절할 사용자 목록은 필수입니다")
        List<Long> userIds,

        @NotBlank(message = "거절 사유는 필수입니다")
        @Size(max = 255, message = "거절 사유는 255자 이내여야 합니다")
        String reason
) {}
