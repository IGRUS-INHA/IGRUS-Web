package igrus.web.inquiry.dto.request;

import igrus.web.inquiry.domain.InquiryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CreateInquiryRequest {

    @NotNull(message = "문의 유형은 필수입니다")
    private InquiryType type;

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자 이내여야 합니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String content;

    @Email(message = "유효한 이메일 형식이어야 합니다")
    private String guestEmail;

    @Size(max = 50, message = "이름은 50자 이내여야 합니다")
    private String guestName;

    private String password;

    @Valid
    @Size(max = 3, message = "첨부파일은 최대 3개까지 가능합니다")
    private List<AttachmentInfo> attachments;

    @Getter
    @Builder
    public static class AttachmentInfo {
        @NotBlank(message = "파일 URL은 필수입니다")
        @Pattern(
                regexp = "^https?://[\\w.-]+(?:\\.[\\w.-]+)+[\\w.,@?^=%&:/~+#-]*$",
                message = "유효한 URL 형식이어야 합니다"
        )
        private String fileUrl;

        @NotBlank(message = "파일명은 필수입니다")
        @Size(max = 255, message = "파일명은 255자 이내여야 합니다")
        private String fileName;

        @NotNull(message = "파일 크기는 필수입니다")
        @Positive(message = "파일 크기는 양수여야 합니다")
        private Long fileSize;
    }
}
