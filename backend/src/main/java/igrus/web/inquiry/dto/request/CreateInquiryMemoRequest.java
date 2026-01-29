package igrus.web.inquiry.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "문의 메모 작성 요청 (관리자용)")
public class CreateInquiryMemoRequest {

    @Schema(description = "메모 내용", example = "추가 확인 필요")
    @NotBlank(message = "메모 내용은 필수입니다")
    private String content;
}
