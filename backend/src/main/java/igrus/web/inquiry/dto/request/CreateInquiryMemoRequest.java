package igrus.web.inquiry.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateInquiryMemoRequest {

    @NotBlank(message = "메모 내용은 필수입니다")
    private String content;
}
