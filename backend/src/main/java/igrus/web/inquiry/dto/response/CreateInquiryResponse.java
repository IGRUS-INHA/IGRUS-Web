package igrus.web.inquiry.dto.response;

import igrus.web.inquiry.domain.Inquiry;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateInquiryResponse {

    private Long id;
    private String inquiryNumber;
    private String message;

    public static CreateInquiryResponse from(Inquiry inquiry) {
        return CreateInquiryResponse.builder()
                .id(inquiry.getId())
                .inquiryNumber(inquiry.getInquiryNumber())
                .message("문의가 접수되었습니다. 문의 번호: " + inquiry.getInquiryNumber())
                .build();
    }
}
