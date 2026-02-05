package igrus.web.security.auth.common.service.login;

import igrus.web.security.auth.common.domain.LoginHistory;
import igrus.web.security.auth.common.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 최근 로그인 성공 기록 조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetRecentSuccessfulLoginsService {

    private final LoginHistoryRepository loginHistoryRepository;

    /**
     * 특정 사용자의 최근 로그인 성공 기록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 최근 로그인 성공 기록 목록 (최대 10건)
     */
    @Transactional(readOnly = true)
    public List<LoginHistory> getRecentSuccessfulLogins(Long userId) {
        return loginHistoryRepository.findTop10ByUserIdAndSuccessTrueOrderByAttemptedAtDesc(userId);
    }
}
