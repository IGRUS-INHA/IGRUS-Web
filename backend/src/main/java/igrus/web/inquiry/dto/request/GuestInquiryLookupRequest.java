package igrus.web.inquiry.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "비회원 문의 조회 요청")
public class GuestInquiryLookupRequest {

    @Schema(description = "조회할 문의 번호", example = "INQ-20240115-001")
    @NotBlank(message = "문의 번호는 필수입니다")
    private String inquiryNumber;

    @Schema(description = "문의 등록 시 사용한 이메일", example = "guest@example.com", format = "email")
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이어야 합니다")
    private String email;

    @Schema(description = "문의 등록 시 설정한 비밀번호", example = "password123", format = "password")
    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}
