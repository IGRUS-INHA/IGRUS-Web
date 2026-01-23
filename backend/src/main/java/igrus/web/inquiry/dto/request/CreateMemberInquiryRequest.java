package igrus.web.inquiry.dto.request;

import igrus.web.inquiry.domain.InquiryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CreateMemberInquiryRequest {

    @NotNull(message = "문의 유형은 필수입니다")
    private InquiryType type;

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자 이내여야 합니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String content;

    @Valid
    @Size(max = 3, message = "첨부파일은 최대 3개까지 가능합니다")
    private List<AttachmentInfo> attachments;
}
