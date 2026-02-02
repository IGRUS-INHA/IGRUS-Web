package igrus.web.inquiry.service.read;

import igrus.web.inquiry.dto.response.InquiryListResponse;
import igrus.web.inquiry.repository.MemberInquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 문의 목록 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetMyInquiriesService {

    private final MemberInquiryRepository memberInquiryRepository;

    /**
     * 회원의 문의 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 문의 목록 페이지
     */
    @Transactional(readOnly = true)
    public Page<InquiryListResponse> getMyInquiries(Long userId, Pageable pageable) {
        return memberInquiryRepository.findByUserId(userId, pageable)
                .map(InquiryListResponse::from);
    }
}
