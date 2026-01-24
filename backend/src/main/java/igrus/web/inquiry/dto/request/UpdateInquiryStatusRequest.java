package igrus.web.inquiry.dto.request;

import igrus.web.inquiry.domain.InquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "문의 상태 변경 요청")
public class UpdateInquiryStatusRequest {

    @Schema(description = "변경할 문의 상태", example = "IN_PROGRESS", allowableValues = {"PENDING", "IN_PROGRESS", "ANSWERED", "CLOSED"})
    @NotNull(message = "상태값은 필수입니다")
    private InquiryStatus status;
}
