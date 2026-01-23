package igrus.web.inquiry.dto.request;

import igrus.web.inquiry.domain.InquiryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateInquiryStatusRequest {

    @NotNull(message = "상태값은 필수입니다")
    private InquiryStatus status;
}
