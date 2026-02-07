package igrus.web.security.auth.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "준회원 일괄 거절 요청")
public record BulkRejectionRequest(
        @Schema(description = "거절할 사용자 ID 목록", example = "[1, 2, 3]")
        @NotEmpty(message = "거절할 사용자 목록은 필수입니다")
        List<Long> userIds,

        @Schema(description = "거절 사유", example = "가입 동기가 불충분합니다.")
        @NotBlank(message = "거절 사유는 필수입니다")
        @Size(max = 255, message = "거절 사유는 255자 이내여야 합니다")
        String reason
) {}
