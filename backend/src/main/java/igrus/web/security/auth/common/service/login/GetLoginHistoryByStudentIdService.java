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
 * 학번 기반 로그인 히스토리 조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetLoginHistoryByStudentIdService {

    private final LoginHistoryRepository loginHistoryRepository;

    /**
     * 특정 학번의 로그인 히스토리를 조회합니다.
     *
     * @param studentId 학번
     * @param pageable 페이지 정보
     * @return 로그인 히스토리 페이지
     */
    @Transactional(readOnly = true)
    public Page<LoginHistory> getHistoryByStudentId(String studentId, Pageable pageable) {
        return loginHistoryRepository.findByStudentIdOrderByAttemptedAtDesc(studentId, pageable);
    }
}
