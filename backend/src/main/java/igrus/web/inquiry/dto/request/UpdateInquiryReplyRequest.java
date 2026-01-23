package igrus.web.inquiry.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateInquiryReplyRequest {

    @NotBlank(message = "답변 내용은 필수입니다")
    private String content;
}
