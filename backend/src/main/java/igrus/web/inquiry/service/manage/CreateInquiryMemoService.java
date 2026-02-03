package igrus.web.inquiry.service.manage;

import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.domain.InquiryMemo;
import igrus.web.inquiry.dto.request.CreateInquiryMemoRequest;
import igrus.web.inquiry.dto.response.InquiryMemoResponse;
import igrus.web.inquiry.service.support.InquiryFinder;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문의 내부 메모 작성 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateInquiryMemoService {

    private final InquiryFinder inquiryFinder;
    private final UserRepository userRepository;

    /**
     * 문의에 내부 메모를 작성합니다.
     *
     * @param inquiryId 문의 ID
     * @param request 메모 작성 요청
     * @param operatorId 운영자 ID
     * @return 메모 응답
     */
    public InquiryMemoResponse createMemo(Long inquiryId, CreateInquiryMemoRequest request, Long operatorId) {
        Inquiry inquiry = inquiryFinder.findById(inquiryId);

        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new UserNotFoundException(operatorId));

        InquiryMemo memo = InquiryMemo.create(request.getContent(), operator);
        inquiry.addMemo(memo);

        log.info("문의 내부 메모 작성: inquiryId={}, operatorId={}", inquiryId, operatorId);

        return InquiryMemoResponse.from(memo);
    }
}
