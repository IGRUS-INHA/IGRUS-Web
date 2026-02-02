package igrus.web.inquiry.service.manage;

import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.dto.request.UpdateInquiryStatusRequest;
import igrus.web.inquiry.service.support.InquiryFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문의 상태 변경 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateInquiryStatusService {

    private final InquiryFinder inquiryFinder;

    /**
     * 문의 상태를 변경합니다.
     *
     * @param inquiryId 문의 ID
     * @param request 상태 변경 요청
     */
    public void updateInquiryStatus(Long inquiryId, UpdateInquiryStatusRequest request) {
        Inquiry inquiry = inquiryFinder.findById(inquiryId);
        inquiry.changeStatus(request.getStatus());

        log.info("문의 상태 변경: inquiryId={}, newStatus={}", inquiryId, request.getStatus());
    }
}
