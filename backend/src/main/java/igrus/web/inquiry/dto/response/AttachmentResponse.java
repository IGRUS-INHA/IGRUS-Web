package igrus.web.inquiry.dto.response;

import igrus.web.inquiry.domain.InquiryAttachment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttachmentResponse {

    private Long id;
    private String fileUrl;
    private String fileName;
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
