package igrus.web.inquiry.dto.response;

import igrus.web.inquiry.domain.InquiryMemo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@Schema(description = "문의 메모 응답 (관리자용)")
public class InquiryMemoResponse {

    @Schema(description = "메모 ID", example = "1")
    private Long id;

    @Schema(description = "메모 내용", example = "추가 확인 필요")
    private String content;

    @Schema(description = "메모 작성자 이름", example = "관리자")
    private String writtenByName;

    @Schema(description = "메모 작성 일시", example = "2024-01-15T11:30:00Z")
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
