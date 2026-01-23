package igrus.web.inquiry.domain;

import igrus.web.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inquiry_attachments")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "inquiry_attachments_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "inquiry_attachments_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "inquiry_attachments_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "inquiry_attachments_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_attachments_id")
    private Long id;

    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_attachments_inquiry_id", nullable = false)
    private Inquiry inquiry;

    @Column(name = "inquiry_attachments_file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "inquiry_attachments_file_name", nullable = false)
    private String fileName;

    @Column(name = "inquiry_attachments_file_size", nullable = false)
    private Long fileSize;

    // === 정적 팩토리 메서드 ===
    public static InquiryAttachment create(String fileUrl, String fileName, Long fileSize) {
        InquiryAttachment attachment = new InquiryAttachment();
        attachment.fileUrl = fileUrl;
        attachment.fileName = fileName;
        attachment.fileSize = fileSize;
        return attachment;
    }
}
