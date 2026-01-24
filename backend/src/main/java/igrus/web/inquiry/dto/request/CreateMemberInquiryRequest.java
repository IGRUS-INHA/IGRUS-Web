package igrus.web.inquiry.dto.request;

import igrus.web.inquiry.domain.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "회원 문의 생성 요청")
public class CreateMemberInquiryRequest {

    @Schema(description = "문의 유형", example = "GENERAL", allowableValues = {"JOIN", "GENERAL", "BUG_REPORT", "SUGGESTION"})
    @NotNull(message = "문의 유형은 필수입니다")
    private InquiryType type;

    @Schema(description = "문의 제목 (100자 이내)", example = "활동 관련 문의드립니다")
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자 이내여야 합니다")
    private String title;

    @Schema(description = "문의 내용", example = "안녕하세요. 다음 활동 일정에 대해 문의드립니다.")
    @NotBlank(message = "내용은 필수입니다")
    private String content;

    @Schema(description = "첨부파일 목록 (최대 3개)")
    @Valid
    @Size(max = 3, message = "첨부파일은 최대 3개까지 가능합니다")
    private List<AttachmentInfo> attachments;
}
