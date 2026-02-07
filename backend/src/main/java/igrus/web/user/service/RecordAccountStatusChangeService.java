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

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordAccountStatusChangeService {

    private final AccountStatusChangeHistoryRepository accountStatusChangeHistoryRepository;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleAccountStatusChange(AccountStatusChangeEvent event) {
        log.info("계정 상태 변경 이력 저장: userId={}, changeType={}, {} -> {}",
                event.userId(), event.changeType(), event.previousValue(), event.newValue());

        String userStudentId = event.userId() != null
                ? userRepository.findStudentIdByIdIncludingDeleted(event.userId()).orElse(null)
                : null;

        String changedByStudentId = event.changedByUserId() != null
                ? userRepository.findStudentIdByIdIncludingDeleted(event.changedByUserId()).orElse(null)
                : null;

        AccountStatusChangeHistory history = AccountStatusChangeHistory.create(
                event.userId(), userStudentId,
                event.changedByUserId(), changedByStudentId,
                event.changeType(),
                event.previousValue(),
                event.newValue(),
                event.reason()
        );

        accountStatusChangeHistoryRepository.save(history);
    }
}
