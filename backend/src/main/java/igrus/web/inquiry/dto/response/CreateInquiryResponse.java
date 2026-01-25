package igrus.web.inquiry.dto.response;

import igrus.web.inquiry.domain.Inquiry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "문의 생성 응답")
public class CreateInquiryResponse {

    @Schema(description = "생성된 문의 ID", example = "1")
    private Long id;

    @Schema(description = "생성된 문의 번호", example = "INQ-20240115-001")
    private String inquiryNumber;

    @Schema(description = "결과 메시지", example = "문의가 접수되었습니다. 문의 번호: INQ-20240115-001")
    private String message;

    public static CreateInquiryResponse from(Inquiry inquiry) {
        return CreateInquiryResponse.builder()
                .id(inquiry.getId())
                .inquiryNumber(inquiry.getInquiryNumber())
                .message("문의가 접수되었습니다. 문의 번호: " + inquiry.getInquiryNumber())
                .build();
    }
}
