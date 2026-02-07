package igrus.web.admin.user.dto;

import igrus.web.user.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "회원 권한 변경 요청")
public record ChangeUserRoleRequest(
        @Schema(description = "변경할 역할", example = "MEMBER")
        @NotNull
        UserRole role
) {}
