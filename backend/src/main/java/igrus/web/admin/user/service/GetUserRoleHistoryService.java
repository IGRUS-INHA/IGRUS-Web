package igrus.web.admin.user.service;

import igrus.web.admin.user.dto.UserRoleHistoryResponse;
import igrus.web.user.domain.UserRole;
import igrus.web.user.repository.UserRoleHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 관리자용 권한 변경 이력 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetUserRoleHistoryService {

    private final UserRoleHistoryRepository userRoleHistoryRepository;

    @Transactional(readOnly = true)
    public Page<UserRoleHistoryResponse> getUserRoleHistories(Long userId, UserRole previousRole, UserRole newRole,
                                                              Long changedBy, Instant startDate, Instant endDate,
                                                              Pageable pageable) {
        return userRoleHistoryRepository.findByFilters(userId, previousRole, newRole, changedBy, startDate, endDate, pageable)
                .map(UserRoleHistoryResponse::from);
    }
}
