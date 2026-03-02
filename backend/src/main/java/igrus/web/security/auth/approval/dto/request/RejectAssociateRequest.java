package igrus.web.security.auth.approval.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectAssociateRequest(
        @NotBlank(message = "거절 사유는 필수입니다")
        @Size(max = 255, message = "거절 사유는 255자 이내여야 합니다")
        String reason
) {}
