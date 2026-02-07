package igrus.web.security.auth.common.service.login;

import igrus.web.security.auth.common.dto.response.LoginHistoryResponse;
import igrus.web.security.auth.common.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 관리자용 로그인 이력 조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetLoginHistoryForAdminService {

    private final LoginHistoryRepository loginHistoryRepository;

    /**
     * 복합 필터를 적용하여 로그인 이력을 조회합니다.
     *
     * @param adminUserId 조회 요청한 관리자 ID
     * @param studentId 학번 필터 (nullable)
     * @param success 성공 여부 필터 (nullable)
     * @param ipAddress IP 주소 필터 (nullable)
     * @param startDate 시작일 필터 (nullable)
     * @param endDate 종료일 필터 (nullable)
     * @param pageable 페이지 정보
     * @return 로그인 이력 페이지
     */
    @Transactional(readOnly = true)
    public Page<LoginHistoryResponse> getLoginHistories(Long adminUserId, String studentId,
                                                         Boolean success, String ipAddress,
                                                         Instant startDate, Instant endDate,
                                                         Pageable pageable) {
        log.info("관리자 로그인 이력 조회: adminUserId={}, filters=[studentId={}, success={}, ipAddress={}, startDate={}, endDate={}]",
                adminUserId, studentId, success, ipAddress, startDate, endDate);
        return loginHistoryRepository.findByFilters(studentId, success, ipAddress, startDate, endDate, pageable)
                .map(LoginHistoryResponse::from);
    }
}
