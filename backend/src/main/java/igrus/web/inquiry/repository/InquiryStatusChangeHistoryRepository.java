package igrus.web.inquiry.repository;

import igrus.web.inquiry.domain.InquiryStatusChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryStatusChangeHistoryRepository extends JpaRepository<InquiryStatusChangeHistory, Long> {

    List<InquiryStatusChangeHistory> findByInquiryIdOrderByCreatedAtDesc(Long inquiryId);
}
