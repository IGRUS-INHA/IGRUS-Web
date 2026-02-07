package igrus.web.user.service;

import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.dto.response.AccountStatusChangeHistoryResponse;
import igrus.web.user.repository.AccountStatusChangeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAccountStatusChangeHistoryService {

    private final AccountStatusChangeHistoryRepository accountStatusChangeHistoryRepository;

    public Page<AccountStatusChangeHistoryResponse> getHistories(
            Long userId,
            Long changedByUserId,
            AccountChangeType changeType,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    ) {
        return accountStatusChangeHistoryRepository
                .findByFilters(userId, changedByUserId, changeType, startDate, endDate, pageable)
                .map(AccountStatusChangeHistoryResponse::from);
    }
}
