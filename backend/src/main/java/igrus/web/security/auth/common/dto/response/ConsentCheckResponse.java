package igrus.web.security.auth.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동의 확인 응답")
public record ConsentCheckResponse(
        @Schema(description = "확인 결과", example = "true")
        boolean result
) {
}
