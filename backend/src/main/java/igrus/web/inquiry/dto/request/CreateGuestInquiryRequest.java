package igrus.web.inquiry.dto.request;

import igrus.web.inquiry.domain.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "비회원 문의 생성 요청")
public class CreateGuestInquiryRequest {

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

    @Schema(description = "비회원 이메일", example = "guest@example.com", format = "email")
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이어야 합니다")
    private String email;

    @Schema(description = "비회원 이름 (50자 이내)", example = "김철수")
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자 이내여야 합니다")
    private String name;

    @Schema(description = "문의 조회용 비밀번호", example = "password123", format = "password")
    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    @Schema(description = "첨부파일 목록 (최대 3개)")
    @Valid
    @Size(max = 3, message = "첨부파일은 최대 3개까지 가능합니다")
    private List<AttachmentInfo> attachments;
}
