package igrus.web.inquiry.service.manage;

import igrus.web.inquiry.domain.InquiryStatusChangeHistory;
import igrus.web.inquiry.audit.InquiryStatusChanged;
import igrus.web.inquiry.repository.InquiryStatusChangeHistoryRepository;
import igrus.web.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class RecordInquiryStatusChangeService {

    private final InquiryStatusChangeHistoryRepository inquiryStatusChangeHistoryRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    public RecordInquiryStatusChangeService(
            InquiryStatusChangeHistoryRepository inquiryStatusChangeHistoryRepository,
            UserRepository userRepository,
            PlatformTransactionManager transactionManager) {
        this.inquiryStatusChangeHistoryRepository = inquiryStatusChangeHistoryRepository;
        this.userRepository = userRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @EventListener
    public void handleInquiryStatusChange(InquiryStatusChanged event) {
        log.info("문의 상태 변경 이력 저장: inquiryId={}, changeType={}, {} -> {}",
                event.inquiryId(), event.changeType(), event.previousValue(), event.newValue());

        try {
            transactionTemplate.executeWithoutResult(status -> {
                String changedByStudentId = resolveStudentId(event.changedByUserId());

                InquiryStatusChangeHistory history = InquiryStatusChangeHistory.create(
                        event.inquiryId(),
                        event.changedByUserId(),
                        changedByStudentId,
                        event.changeType(),
                        event.previousValue(),
                        event.newValue()
                );

                inquiryStatusChangeHistoryRepository.save(history);
            });
        } catch (Exception e) {
            log.error("문의 상태 변경 이력 저장 실패: inquiryId={}, changeType={}",
                    event.inquiryId(), event.changeType(), e);
        }
    }

    private String resolveStudentId(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findStudentIdByIdIncludingDeleted(userId)
                .orElseGet(() -> {
                    log.warn("학번 조회 실패: userId={}", userId);
                    return null;
                });
    }
}
