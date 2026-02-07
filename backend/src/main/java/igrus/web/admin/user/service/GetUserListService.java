package igrus.web.admin.user.service;

import igrus.web.admin.user.dto.UserListResponse;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자용 회원 목록 조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetUserListService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<UserListResponse> getUserList(String keyword, UserRole role, UserStatus status, Pageable pageable) {
        return userRepository.findByFilters(keyword, role, status, pageable)
                .map(UserListResponse::from);
    }
}
