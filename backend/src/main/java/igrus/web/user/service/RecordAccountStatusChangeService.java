package igrus.web.user.service;

import igrus.web.user.domain.AccountStatusChangeHistory;
import igrus.web.user.event.AccountStatusChangeEvent;
import igrus.web.user.repository.AccountStatusChangeHistoryRepository;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordAccountStatusChangeService {

    private final AccountStatusChangeHistoryRepository accountStatusChangeHistoryRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAccountStatusChange(AccountStatusChangeEvent event) {
        log.info("계정 상태 변경 이력 저장: userId={}, changeType={}, {} -> {}",
                event.userId(), event.changeType(), event.previousValue(), event.newValue());

        try {
            transactionTemplate.executeWithoutResult(status -> {
                String userStudentId = resolveStudentId(event.userId());
                String changedByStudentId = resolveStudentId(event.changedByUserId());

                AccountStatusChangeHistory history = AccountStatusChangeHistory.create(
                        event.userId(), userStudentId,
                        event.changedByUserId(), changedByStudentId,
                        event.changeType(),
                        event.previousValue(),
                        event.newValue(),
                        event.reason()
                );

                accountStatusChangeHistoryRepository.save(history);
            });
        } catch (Exception e) {
            log.error("계정 상태 변경 이력 저장 실패: userId={}, changeType={}",
                    event.userId(), event.changeType(), e);
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
