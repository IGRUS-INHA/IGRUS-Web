package igrus.web.user.controller;

import igrus.web.common.util.EnumUtils;
import igrus.web.common.util.PageResponseMapper;
import igrus.web.common.util.PageableUtils;
import igrus.web.generated.api.AccountStatusChangeHistoryApi;
import igrus.web.generated.model.GetLoginHistories200Response;
import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.dto.response.AccountStatusChangeHistoryResponse;
import igrus.web.user.service.GetAccountStatusChangeHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountStatusChangeHistoryController implements AccountStatusChangeHistoryApi {

    private final GetAccountStatusChangeHistoryService getAccountStatusChangeHistoryService;

    @Override
    public ResponseEntity<GetLoginHistories200Response> getHistories(
            Long userId, Long changedByUserId, String changeType,
            Instant startDate, Instant endDate,
            Integer page, Integer size, List<String> sort) {
        Pageable pageable = PageableUtils.of(page, size, sort);
        AccountChangeType changeTypeEnum = EnumUtils.fromStringOrNull(AccountChangeType.class, changeType);

        Page<AccountStatusChangeHistoryResponse> histories =
                getAccountStatusChangeHistoryService.getHistories(
                        userId, changedByUserId, changeTypeEnum, startDate, endDate, pageable
                );

        GetLoginHistories200Response response = PageResponseMapper.toSpringPageResponse(
                histories,
                item -> item,
                GetLoginHistories200Response::new,
                (r, content, meta) -> r
                        .content(content.stream().map(c -> (Object) c).toList())
                        .totalElements(meta.totalElements())
                        .totalPages(meta.totalPages())
                        .number(meta.number())
                        .size(meta.size())
                        .numberOfElements(meta.numberOfElements())
                        .first(meta.first())
                        .last(meta.last())
                        .empty(meta.empty())
                        .pageable(meta.pageable())
                        .sort(meta.sort())
        );

        return ResponseEntity.ok(response);
    }
}
