package igrus.web.inquiry.dto.response;

import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@Schema(description = "문의 목록 응답")
public class InquiryListResponse {

    @Schema(description = "문의 고유 ID", example = "1")
    private Long id;

    @Schema(description = "문의 번호", example = "INQ-20240115-001")
    private String inquiryNumber;

    @Schema(description = "문의 유형", example = "JOIN", allowableValues = {"JOIN", "GENERAL", "BUG_REPORT", "SUGGESTION"})
    private InquiryType type;

    @Schema(description = "문의 유형 설명", example = "가입 문의")
    private String typeDescription;

    @Schema(description = "문의 상태", example = "PENDING", allowableValues = {"PENDING", "IN_PROGRESS", "ANSWERED", "CLOSED"})
    private InquiryStatus status;

    @Schema(description = "문의 상태 설명", example = "대기중")
    private String statusDescription;

    @Schema(description = "문의 제목", example = "동아리 가입 문의드립니다")
    private String title;

    @Schema(description = "작성자 이름", example = "홍길동")
    private String authorName;

    @Schema(description = "비회원 문의 여부", example = "false")
    private boolean isGuest;

    @Schema(description = "답변 존재 여부", example = "false")
    private boolean hasReply;

    @Schema(description = "첨부파일 개수", example = "2")
    private int attachmentCount;

    @Schema(description = "문의 작성 일시", example = "2024-01-15T10:30:00Z")
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
