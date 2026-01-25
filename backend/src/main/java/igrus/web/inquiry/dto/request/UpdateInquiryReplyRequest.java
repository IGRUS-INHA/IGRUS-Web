package igrus.web.inquiry.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "문의 답변 수정 요청")
public class UpdateInquiryReplyRequest {

    @Schema(description = "수정할 답변 내용", example = "안녕하세요. 수정된 답변 내용입니다.")
    @NotBlank(message = "답변 내용은 필수입니다")
    private String content;
}
