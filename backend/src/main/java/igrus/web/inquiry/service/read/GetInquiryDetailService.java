package igrus.web.inquiry.service.read;

import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.dto.response.InquiryDetailResponse;
import igrus.web.inquiry.exception.InquiryNotFoundException;
import igrus.web.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문의 상세 조회 서비스 (관리자).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetInquiryDetailService {

    private final InquiryRepository inquiryRepository;

    /**
     * 문의 상세 정보를 조회합니다 (메모 포함).
     *
     * @param inquiryId 문의 ID
     * @return 문의 상세 응답
     */
    @Transactional(readOnly = true)
    public InquiryDetailResponse getInquiryDetail(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findByIdWithAllRelations(inquiryId)
                .orElseThrow(() -> new InquiryNotFoundException(inquiryId));
        return InquiryDetailResponse.from(inquiry);
    }
}
