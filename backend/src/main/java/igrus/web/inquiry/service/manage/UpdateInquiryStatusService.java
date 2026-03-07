package igrus.web.inquiry.service.manage;

import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.domain.InquiryChangeType;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.dto.request.UpdateInquiryStatusRequest;
import igrus.web.inquiry.audit.InquiryStatusChanged;
import igrus.web.inquiry.service.support.InquiryFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 문의 상태를 변경합니다.
     *
     * @param inquiryId 문의 ID
     * @param request 상태 변경 요청
     * @param operatorId 운영자 ID
     */
    public void updateInquiryStatus(Long inquiryId, UpdateInquiryStatusRequest request, Long operatorId) {
        Inquiry inquiry = inquiryFinder.findById(inquiryId);

        InquiryStatus previousStatus = inquiry.getStatus();
        inquiry.changeStatus(request.getStatus());

        if (!previousStatus.equals(inquiry.getStatus())) {
            eventPublisher.publishEvent(new InquiryStatusChanged(
                    inquiryId, operatorId, InquiryChangeType.STATUS_CHANGED,
                    previousStatus.name(), inquiry.getStatus().name()));
        }

        log.info("문의 상태 변경: inquiryId={}, newStatus={}", inquiryId, request.getStatus());
    }
}
