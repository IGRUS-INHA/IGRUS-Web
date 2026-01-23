package igrus.web.inquiry.dto.response;

import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class InquiryResponse {

    private Long id;
    private String inquiryNumber;
    private InquiryType type;
    private String typeDescription;
    private InquiryStatus status;
    private String statusDescription;
    private String title;
    private String content;
    private String authorName;
    private String authorEmail;
    private boolean isGuest;
    private List<AttachmentResponse> attachments;
    private InquiryReplyResponse reply;
    private Instant createdAt;
    private Instant updatedAt;

    public static InquiryResponse from(Inquiry inquiry) {
        return InquiryResponse.builder()
                .id(inquiry.getId())
                .inquiryNumber(inquiry.getInquiryNumber())
                .type(inquiry.getType())
                .typeDescription(inquiry.getType().getDescription())
                .status(inquiry.getStatus())
                .statusDescription(inquiry.getStatus().getDescription())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .authorName(inquiry.getAuthorName())
                .authorEmail(inquiry.getAuthorEmail())
                .isGuest(inquiry.isGuestInquiry())
                .attachments(inquiry.getAttachments().stream()
                        .map(AttachmentResponse::from)
                        .toList())
                .reply(inquiry.hasReply() ? InquiryReplyResponse.from(inquiry.getReply()) : null)
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .build();
    }
}
