package igrus.web.inquiry.repository;

import igrus.web.inquiry.domain.InquiryMemo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryMemoRepository extends JpaRepository<InquiryMemo, Long> {

    List<InquiryMemo> findByInquiryIdOrderByCreatedAtDesc(Long inquiryId);

    int countByInquiryId(Long inquiryId);

    void deleteByInquiryId(Long inquiryId);
}
