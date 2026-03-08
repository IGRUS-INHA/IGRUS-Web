package igrus.web.event.service;

import igrus.web.event.domain.EventStatusChangeHistory;
import igrus.web.event.audit.EventStatusChanged;
import igrus.web.event.repository.EventStatusChangeHistoryRepository;
import igrus.web.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class RecordEventStatusChangeService {

    private final EventStatusChangeHistoryRepository eventStatusChangeHistoryRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    public RecordEventStatusChangeService(
            EventStatusChangeHistoryRepository eventStatusChangeHistoryRepository,
            UserRepository userRepository,
            PlatformTransactionManager transactionManager) {
        this.eventStatusChangeHistoryRepository = eventStatusChangeHistoryRepository;
        this.userRepository = userRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @EventListener
    public void handleEventStatusChange(EventStatusChanged event) {
        log.info("행사 상태 변경 이력 저장: eventId={}, changeType={}, {} -> {}",
                event.eventId(), event.changeType(), event.previousValue(), event.newValue());

        try {
            transactionTemplate.executeWithoutResult(status -> {
                String changedByStudentId = resolveStudentId(event.changedByUserId());

                EventStatusChangeHistory history = EventStatusChangeHistory.create(
                        event.eventId(),
                        event.changedByUserId(),
                        changedByStudentId,
                        event.changeType(),
                        event.previousValue(),
                        event.newValue(),
                        event.reason()
                );

                eventStatusChangeHistoryRepository.save(history);
            });
        } catch (Exception e) {
            log.error("행사 상태 변경 이력 저장 실패: eventId={}, changeType={}",
                    event.eventId(), event.changeType(), e);
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
