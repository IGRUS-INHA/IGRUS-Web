package igrus.web.inquiry.dto.response;

import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class InquiryListResponse {

    private Long id;
    private String inquiryNumber;
    private InquiryType type;
    private String typeDescription;
    private InquiryStatus status;
    private String statusDescription;
    private String title;
    private String authorName;
    private boolean isGuest;
    private boolean hasReply;
    private int attachmentCount;
    private Instant createdAt;

    public static InquiryListResponse from(Inquiry inquiry) {
        return InquiryListResponse.builder()
                .id(inquiry.getId())
                .inquiryNumber(inquiry.getInquiryNumber())
                .type(inquiry.getType())
                .typeDescription(inquiry.getType().getDescription())
                .status(inquiry.getStatus())
                .statusDescription(inquiry.getStatus().getDescription())
                .title(inquiry.getTitle())
                .authorName(inquiry.getAuthorName())
                .isGuest(inquiry.isGuestInquiry())
                .hasReply(inquiry.hasReply())
                .attachmentCount(inquiry.getAttachments().size())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}
