package igrus.web.inquiry.service.support;

import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.exception.InquiryNotFoundException;
import igrus.web.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문의 조회 공통 헬퍼.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class InquiryFinder {

    private final InquiryRepository inquiryRepository;

    /**
     * ID로 문의를 조회합니다.
     *
     * @param inquiryId 문의 ID
     * @return 문의 엔티티
     * @throws InquiryNotFoundException 문의를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public Inquiry findById(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InquiryNotFoundException(inquiryId));
    }
}
