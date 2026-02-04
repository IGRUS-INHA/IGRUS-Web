package igrus.web.security.auth.common.service.login;

import igrus.web.security.auth.common.domain.LoginHistory;
import igrus.web.security.auth.common.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 ID 기반 로그인 히스토리 조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetLoginHistoryByUserIdService {

    private final LoginHistoryRepository loginHistoryRepository;

    /**
     * 특정 사용자의 로그인 히스토리를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param pageable 페이지 정보
     * @return 로그인 히스토리 페이지
     */
    @Transactional(readOnly = true)
    public Page<LoginHistory> getHistoryByUserId(Long userId, Pageable pageable) {
        return loginHistoryRepository.findByUserIdOrderByAttemptedAtDesc(userId, pageable);
    }
}
