package igrus.web.event.dto.response;

import igrus.web.event.domain.EventAttachment;

/**
 * 행사 첨부파일 DTO.
 *
 * @param id               첨부파일 연결 ID
 * @param fileMetadataId   파일 메타데이터 ID
 * @param objectKey        S3 Object Key
 * @param originalFileName 원본 파일명
 * @param contentType      파일 MIME 타입
 * @param isThumbnail      썸네일 여부
 * @param displayOrder     표시 순서
 */
public record EventAttachmentDto(
        Long id,
        Long fileMetadataId,
        String objectKey,
        String originalFileName,
        String contentType,
        boolean isThumbnail,
        int displayOrder
) {
    public static EventAttachmentDto from(EventAttachment attachment) {
        return new EventAttachmentDto(
                attachment.getId(),
                attachment.getFileMetadata().getId(),
                attachment.getFileMetadata().getObjectKey(),
                attachment.getFileMetadata().getOriginalFileName(),
                attachment.getFileMetadata().getContentType(),
                attachment.isThumbnail(),
                attachment.getDisplayOrder()
        );
    }
}
