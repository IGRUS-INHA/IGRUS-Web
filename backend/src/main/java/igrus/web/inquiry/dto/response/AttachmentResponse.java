package igrus.web.inquiry.dto.response;

import igrus.web.inquiry.domain.InquiryAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "첨부파일 응답")
public class AttachmentResponse {

    @Schema(description = "첨부파일 ID", example = "1")
    private Long id;

    @Schema(description = "파일 URL", example = "https://storage.example.com/files/document.pdf")
    private String fileUrl;

    @Schema(description = "파일명", example = "document.pdf")
    private String fileName;

    @Schema(description = "파일 크기 (바이트)", example = "1024")
    private Long fileSize;

    public static AttachmentResponse from(InquiryAttachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .fileUrl(attachment.getFileUrl())
                .fileName(attachment.getFileName())
                .fileSize(attachment.getFileSize())
                .build();
    }
}
