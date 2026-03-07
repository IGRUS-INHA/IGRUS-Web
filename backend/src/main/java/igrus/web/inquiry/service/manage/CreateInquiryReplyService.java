package igrus.web.inquiry.service.manage;

import igrus.web.inquiry.domain.Inquiry;
import igrus.web.inquiry.domain.InquiryChangeType;
import igrus.web.inquiry.domain.InquiryReply;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.dto.request.CreateInquiryReplyRequest;
import igrus.web.inquiry.dto.response.InquiryReplyResponse;
import igrus.web.inquiry.audit.InquiryStatusChanged;
import igrus.web.inquiry.exception.InquiryAlreadyRepliedException;
import igrus.web.inquiry.service.support.InquiryFinder;
import igrus.web.inquiry.service.support.InquiryNotificationService;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문의 답변 작성 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateInquiryReplyService {

    private final InquiryFinder inquiryFinder;
    private final UserRepository userRepository;
    private final InquiryNotificationService inquiryNotificationService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 문의에 답변을 작성합니다.
     *
     * @param inquiryId 문의 ID
     * @param request 답변 작성 요청
     * @param operatorId 운영자 ID
     * @return 답변 응답
     */
    public InquiryReplyResponse createReply(Long inquiryId, CreateInquiryReplyRequest request, Long operatorId) {
        Inquiry inquiry = inquiryFinder.findById(inquiryId);

        if (inquiry.hasReply()) {
            throw new InquiryAlreadyRepliedException(inquiryId);
        }

        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new UserNotFoundException(operatorId));

        InquiryReply reply = InquiryReply.create(request.getContent(), operator);
        inquiry.setReply(reply);

        InquiryStatus previousStatus = inquiry.getStatus();
        inquiry.complete();

        eventPublisher.publishEvent(new InquiryStatusChanged(
                inquiryId, operatorId, InquiryChangeType.REPLY_COMPLETED,
                previousStatus.name(), InquiryStatus.COMPLETED.name()));

        log.info("문의 답변 작성: inquiryId={}, operatorId={}", inquiryId, operatorId);

        try {
            inquiryNotificationService.sendReplyNotification(
                    inquiry.getAuthorEmail(),
                    inquiry.getInquiryNumber(),
                    inquiry.getTitle(),
                    request.getContent()
            );
        } catch (Exception e) {
            log.error("답변 알림 이메일 발송 실패: inquiryId={}, email={}", inquiryId, inquiry.getAuthorEmail(), e);
        }

        return InquiryReplyResponse.from(reply);
    }
}
