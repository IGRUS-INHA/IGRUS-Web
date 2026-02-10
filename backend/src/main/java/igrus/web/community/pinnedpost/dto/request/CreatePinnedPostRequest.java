package igrus.web.community.pinnedpost.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreatePinnedPostRequest(
        @NotNull(message = "게시글 ID는 필수입니다")
        Long postId,

        @NotNull(message = "표시 순서는 필수입니다")
        @Min(value = 1, message = "표시 순서는 1 이상이어야 합니다")
        Integer displayOrder
) {}
