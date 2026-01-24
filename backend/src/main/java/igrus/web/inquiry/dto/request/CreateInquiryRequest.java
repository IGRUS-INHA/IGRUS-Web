package igrus.web.inquiry.dto.request;

import igrus.web.inquiry.domain.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "문의 생성 요청 (통합)")
public class CreateInquiryRequest {

    @Schema(description = "문의 유형", example = "JOIN", allowableValues = {"JOIN", "GENERAL", "BUG_REPORT", "SUGGESTION"})
    @NotNull(message = "문의 유형은 필수입니다")
    private InquiryType type;

    @Schema(description = "문의 제목 (100자 이내)", example = "동아리 가입 문의드립니다")
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자 이내여야 합니다")
    private String title;

    @Schema(description = "문의 내용", example = "안녕하세요. 동아리 가입 절차에 대해 문의드립니다.")
    @NotBlank(message = "내용은 필수입니다")
    private String content;

    @Schema(description = "비회원 이메일 (비회원 문의 시 필수)", example = "guest@example.com", format = "email", nullable = true)
    @Email(message = "유효한 이메일 형식이어야 합니다")
    private String guestEmail;

    @Schema(description = "비회원 이름 (비회원 문의 시 필수)", example = "김철수", nullable = true)
    @Size(max = 50, message = "이름은 50자 이내여야 합니다")
    private String guestName;

    @Schema(description = "비회원 문의 비밀번호 (비회원 문의 시 필수)", example = "password123", format = "password", nullable = true)
    private String password;

    @Schema(description = "첨부파일 목록 (최대 3개)")
    @Valid
    @Size(max = 3, message = "첨부파일은 최대 3개까지 가능합니다")
    private List<AttachmentInfo> attachments;

    @Getter
    @Builder
    @Schema(description = "첨부파일 정보")
    public static class AttachmentInfo {
        @Schema(description = "파일 URL", example = "https://storage.example.com/files/document.pdf")
        @NotBlank(message = "파일 URL은 필수입니다")
        @Pattern(
                regexp = "^https?://[\\w.-]+(?:\\.[\\w.-]+)+[\\w.,@?^=%&:/~+#-]*$",
                message = "유효한 URL 형식이어야 합니다"
        )
        private String fileUrl;

        @Schema(description = "파일명 (255자 이내)", example = "document.pdf")
        @NotBlank(message = "파일명은 필수입니다")
        @Size(max = 255, message = "파일명은 255자 이내여야 합니다")
        private String fileName;

        @Schema(description = "파일 크기 (바이트)", example = "1024")
        @NotNull(message = "파일 크기는 필수입니다")
        @Positive(message = "파일 크기는 양수여야 합니다")
        private Long fileSize;
    }
}
