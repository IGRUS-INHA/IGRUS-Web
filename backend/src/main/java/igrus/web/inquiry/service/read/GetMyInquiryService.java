package igrus.web.inquiry.service.read;

import igrus.web.inquiry.domain.MemberInquiry;
import igrus.web.inquiry.dto.response.InquiryResponse;
import igrus.web.inquiry.exception.InquiryAccessDeniedException;
import igrus.web.inquiry.repository.MemberInquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 문의 상세 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetMyInquiryService {

    private final MemberInquiryRepository memberInquiryRepository;

    /**
     * 회원의 특정 문의를 상세 조회합니다.
     *
     * @param inquiryId 문의 ID
     * @param userId 사용자 ID
     * @return 문의 응답
     */
    @Transactional(readOnly = true)
    public InquiryResponse getMyInquiry(Long inquiryId, Long userId) {
        MemberInquiry inquiry = memberInquiryRepository.findByIdAndUserId(inquiryId, userId)
                .orElseThrow(InquiryAccessDeniedException::new);

        return InquiryResponse.from(inquiry);
    }
}
