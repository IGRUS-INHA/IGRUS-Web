package igrus.web.inquiry.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttachmentInfo {

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
