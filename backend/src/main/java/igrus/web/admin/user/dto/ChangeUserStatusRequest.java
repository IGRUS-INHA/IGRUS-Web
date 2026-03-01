package igrus.web.admin.user.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ChangeUserStatusRequest(
        @NotNull
        Action action,

        String reason,

        Instant suspendedUntil
) {
    public enum Action {
        SUSPEND, LIFT
    }
}
