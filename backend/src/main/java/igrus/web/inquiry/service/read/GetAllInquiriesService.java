package igrus.web.inquiry.service.read;

import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.response.InquiryListResponse;
import igrus.web.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전체 문의 목록 조회 서비스 (관리자).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetAllInquiriesService {

    private final InquiryRepository inquiryRepository;

    /**
     * 전체 문의 목록을 필터링하여 조회합니다.
     *
     * @param type 문의 유형 필터 (null 가능)
     * @param status 처리 상태 필터 (null 가능)
     * @param pageable 페이징 정보
     * @return 문의 목록 페이지
     */
    @Transactional(readOnly = true)
    public Page<InquiryListResponse> getAllInquiries(InquiryType type, InquiryStatus status, Pageable pageable) {
        return inquiryRepository.findByFilters(type, status, pageable)
                .map(InquiryListResponse::from);
    }
}
