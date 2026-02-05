package igrus.web.inquiry.service.manage;

import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.dto.request.UpdateInquiryReplyRequest;
import igrus.web.inquiry.dto.response.InquiryReplyResponse;
import igrus.web.inquiry.exception.InquiryReplyNotFoundException;
import igrus.web.inquiry.service.support.InquiryFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문의 답변 수정 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateInquiryReplyService {

    private final InquiryFinder inquiryFinder;

    /**
     * 문의 답변을 수정합니다.
     *
     * @param inquiryId 문의 ID
     * @param request 답변 수정 요청
     * @param operatorId 운영자 ID
     * @return 답변 응답
     */
    public InquiryReplyResponse updateReply(Long inquiryId, UpdateInquiryReplyRequest request, Long operatorId) {
        Inquiry inquiry = inquiryFinder.findById(inquiryId);

        if (!inquiry.hasReply()) {
            throw new InquiryReplyNotFoundException(inquiryId);
        }

        inquiry.getReply().updateContent(request.getContent());

        log.info("문의 답변 수정: inquiryId={}, operatorId={}", inquiryId, operatorId);

        return InquiryReplyResponse.from(inquiry.getReply());
    }
}
