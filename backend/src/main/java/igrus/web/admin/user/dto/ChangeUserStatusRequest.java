package igrus.web.admin.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "회원 상태 변경 요청")
public record ChangeUserStatusRequest(
        @Schema(description = "수행할 행위", example = "SUSPEND")
        @NotNull
        Action action,

        @Schema(description = "정지 사유 (SUSPEND일 때 필수)", example = "규칙 위반")
        String reason,

        @Schema(description = "정지 종료일 (SUSPEND일 때 필수)", example = "2025-03-01T00:00:00Z")
        Instant suspendedUntil
) {
    public enum Action {
        SUSPEND, LIFT
    }
}
