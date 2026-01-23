package igrus.web.inquiry.repository;

import igrus.web.inquiry.domain.InquiryReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InquiryReplyRepository extends JpaRepository<InquiryReply, Long> {

    Optional<InquiryReply> findByInquiryId(Long inquiryId);

    boolean existsByInquiryId(Long inquiryId);

    void deleteByInquiryId(Long inquiryId);
}
