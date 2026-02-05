package igrus.web.inquiry.service.manage;

import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.service.support.InquiryFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문의 삭제 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteInquiryService {

    private final InquiryFinder inquiryFinder;

    /**
     * 문의를 소프트 삭제합니다.
     *
     * @param inquiryId 문의 ID
     * @param operatorId 운영자 ID
     */
    public void deleteInquiry(Long inquiryId, Long operatorId) {
        Inquiry inquiry = inquiryFinder.findById(inquiryId);
        inquiry.delete(operatorId);

        log.info("문의 삭제: inquiryId={}, deletedBy={}", inquiryId, operatorId);
    }
}
