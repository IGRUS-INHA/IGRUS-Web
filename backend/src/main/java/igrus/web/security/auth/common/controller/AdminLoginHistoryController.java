package igrus.web.security.auth.common.controller;

import igrus.web.common.util.PageResponseMapper;
import igrus.web.common.util.PageableUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.generated.api.AdminLoginHistoryApi;
import igrus.web.generated.model.ApiPage;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.security.auth.common.dto.response.LoginHistoryResponse;
import igrus.web.security.auth.common.service.login.GetLoginHistoryForAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLoginHistoryController implements AdminLoginHistoryApi {

    private final GetLoginHistoryForAdminService getLoginHistoryForAdminService;

    @Override
    public ResponseEntity<ApiPage> getLoginHistories(
            String studentId, Boolean success, String ipAddress,
            Instant startDate, Instant endDate,
            Integer page, Integer size, List<String> sort) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageableUtils.of(page, size, sort);

        org.springframework.data.domain.Page<LoginHistoryResponse> loginHistories = getLoginHistoryForAdminService.getLoginHistories(
                user.userId(), studentId, success, ipAddress, startDate, endDate, pageable
        );

        ApiPage response = PageResponseMapper.toSpringPageResponse(
                loginHistories,
                item -> item,
                ApiPage::new,
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
