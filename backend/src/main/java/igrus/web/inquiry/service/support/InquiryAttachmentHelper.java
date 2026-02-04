package igrus.web.inquiry.service.support;

import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.domain.InquiryAttachment;
import igrus.web.inquiry.dto.request.AttachmentInfo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 문의 첨부파일 처리 헬퍼.
 */
@Component
@Transactional
public class InquiryAttachmentHelper {

    /**
     * 문의에 첨부파일을 추가합니다.
     *
     * @param inquiry 문의 엔티티
     * @param attachments 첨부파일 정보 목록
     */
    public void addAttachments(Inquiry inquiry, List<AttachmentInfo> attachments) {
        if (attachments != null && !attachments.isEmpty()) {
            for (AttachmentInfo attachmentInfo : attachments) {
                InquiryAttachment attachment = InquiryAttachment.create(
                        attachmentInfo.getFileUrl(),
                        attachmentInfo.getFileName(),
                        attachmentInfo.getFileSize()
                );
                inquiry.addAttachment(attachment);
            }
        }
    }
}
