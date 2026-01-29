package igrus.web.inquiry.dto.response;

import igrus.web.inquiry.domain.InquiryReply;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@Schema(description = "문의 답변 응답")
public class InquiryReplyResponse {

    @Schema(description = "답변 ID", example = "1")
    private Long id;

    @Schema(description = "답변 내용", example = "안녕하세요. 문의 주신 내용에 대해 답변 드립니다.")
    private String content;

    @Schema(description = "답변 작성자 이름", example = "관리자")
    private String repliedByName;

    @Schema(description = "답변 작성 일시", example = "2024-01-16T10:30:00Z")
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
