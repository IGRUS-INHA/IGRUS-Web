package igrus.web.inquiry.repository;

import igrus.web.inquiry.domain.InquiryAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryAttachmentRepository extends JpaRepository<InquiryAttachment, Long> {

    List<InquiryAttachment> findByInquiryId(Long inquiryId);

    int countByInquiryId(Long inquiryId);

    void deleteByInquiryId(Long inquiryId);
}
