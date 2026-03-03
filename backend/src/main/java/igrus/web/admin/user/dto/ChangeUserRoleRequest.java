package igrus.web.admin.user.dto;

import igrus.web.user.domain.UserRole;
import jakarta.validation.constraints.NotNull;

public record ChangeUserRoleRequest(
        @NotNull
        UserRole role
) {}
