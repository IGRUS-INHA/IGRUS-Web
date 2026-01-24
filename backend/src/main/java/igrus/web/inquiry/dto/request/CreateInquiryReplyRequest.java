package igrus.web.inquiry.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "문의 답변 작성 요청")
public class CreateInquiryReplyRequest {

    @Schema(description = "답변 내용", example = "안녕하세요. 문의 주신 내용에 대해 답변 드립니다.")
    @NotBlank(message = "답변 내용은 필수입니다")
    private String content;
}
