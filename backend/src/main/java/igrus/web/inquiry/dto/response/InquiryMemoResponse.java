package igrus.web.inquiry.dto.response;

import igrus.web.inquiry.domain.InquiryMemo;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class InquiryMemoResponse {

    private Long id;
    private String content;
    private String writtenByName;
    private Instant createdAt;

    public static InquiryMemoResponse from(InquiryMemo memo) {
        return InquiryMemoResponse.builder()
                .id(memo.getId())
                .content(memo.getContent())
                .writtenByName(memo.getWrittenBy().getName())
                .createdAt(memo.getCreatedAt())
                .build();
    }
}
