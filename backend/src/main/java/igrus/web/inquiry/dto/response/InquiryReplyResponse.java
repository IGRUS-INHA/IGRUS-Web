package igrus.web.inquiry.dto.response;

import igrus.web.inquiry.domain.InquiryReply;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class InquiryReplyResponse {

    private Long id;
    private String content;
    private String repliedByName;
    private Instant createdAt;

    public static InquiryReplyResponse from(InquiryReply reply) {
        return InquiryReplyResponse.builder()
                .id(reply.getId())
                .content(reply.getContent())
                .repliedByName(reply.getRepliedBy().getName())
                .createdAt(reply.getCreatedAt())
                .build();
    }
}
