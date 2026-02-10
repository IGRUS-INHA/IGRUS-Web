package igrus.web.community.pinnedpost.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateDisplayOrderRequest(
        @NotNull(message = "표시 순서는 필수입니다")
        @Min(value = 1, message = "표시 순서는 1 이상이어야 합니다")
        Integer displayOrder
) {}
