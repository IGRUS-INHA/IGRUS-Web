package igrus.web.security.auth.common.service.login;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.LoginFailureReason;
import igrus.web.security.auth.common.domain.LoginHistory;
import igrus.web.security.auth.common.dto.response.LoginHistoryResponse;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GetLoginHistoryForAdminService 통합 테스트")
class GetLoginHistoryForAdminServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetLoginHistoryForAdminService getLoginHistoryForAdminService;

    @Autowired
    private RecordLoginSuccessService recordLoginSuccessService;

    @Autowired
    private RecordLoginFailureService recordLoginFailureService;

    private static final String TEST_IP = "192.168.1.100";
    private static final String TEST_IP_2 = "10.0.0.1";
    private static final String TEST_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    private User testUser;
    private PageRequest defaultPageRequest;

    @BeforeEach
    void setUp() {
        setUpBase();
        testUser = createAndSaveUser("20200001", "test@inha.edu", UserRole.MEMBER);
        defaultPageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "attemptedAt"));
    }

    @Test
    @DisplayName("필터 없이 전체 조회")
    void getLoginHistories_NoFilter_ReturnsAll() {
        // given
        transactionTemplate.execute(status -> {
            recordLoginSuccessService.recordSuccess(testUser, "20200001", TEST_IP, TEST_USER_AGENT);
            return null;
        });
        transactionTemplate.execute(status -> {
            recordLoginFailureService.recordFailure("20220001", TEST_IP, TEST_USER_AGENT, LoginFailureReason.INVALID_CREDENTIALS);
            return null;
        });

        // when
        Page<LoginHistoryResponse> result = getLoginHistoryForAdminService.getLoginHistories(
                1L, null, null, null, null, null, defaultPageRequest
        );

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("studentId 필터 조회")
    void getLoginHistories_FilterByStudentId_ReturnsFiltered() {
        // given
        transactionTemplate.execute(status -> {
            recordLoginSuccessService.recordSuccess(testUser, "20200001", TEST_IP, TEST_USER_AGENT);
            return null;
        });
        transactionTemplate.execute(status -> {
            recordLoginFailureService.recordFailure("20220001", TEST_IP, TEST_USER_AGENT, LoginFailureReason.INVALID_CREDENTIALS);
            return null;
        });

        // when
        Page<LoginHistoryResponse> result = getLoginHistoryForAdminService.getLoginHistories(
                1L, "20200001", null, null, null, null, defaultPageRequest
        );

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().studentId()).isEqualTo("20200001");
    }

    @Test
    @DisplayName("success 필터 조회")
    void getLoginHistories_FilterBySuccess_ReturnsFiltered() {
        // given
        transactionTemplate.execute(status -> {
            recordLoginSuccessService.recordSuccess(testUser, "20200001", TEST_IP, TEST_USER_AGENT);
            return null;
        });
        transactionTemplate.execute(status -> {
            recordLoginFailureService.recordFailure("20220001", TEST_IP, TEST_USER_AGENT, LoginFailureReason.INVALID_CREDENTIALS);
            return null;
        });

        // when
        Page<LoginHistoryResponse> result = getLoginHistoryForAdminService.getLoginHistories(
                1L, null, false, null, null, null, defaultPageRequest
        );

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().success()).isFalse();
    }

    @Test
    @DisplayName("ipAddress 필터 조회")
    void getLoginHistories_FilterByIpAddress_ReturnsFiltered() {
        // given
        transactionTemplate.execute(status -> {
            recordLoginSuccessService.recordSuccess(testUser, "20200001", TEST_IP, TEST_USER_AGENT);
            return null;
        });
        transactionTemplate.execute(status -> {
            recordLoginFailureService.recordFailure("20220001", TEST_IP_2, TEST_USER_AGENT, LoginFailureReason.INVALID_CREDENTIALS);
            return null;
        });

        // when
        Page<LoginHistoryResponse> result = getLoginHistoryForAdminService.getLoginHistories(
                1L, null, null, TEST_IP_2, null, null, defaultPageRequest
        );

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().ipAddress()).isEqualTo(TEST_IP_2);
    }

    @Test
    @DisplayName("날짜 범위 필터 조회")
    void getLoginHistories_FilterByDateRange_ReturnsFiltered() {
        // given
        transactionTemplate.execute(status -> {
            recordLoginSuccessService.recordSuccess(testUser, "20200001", TEST_IP, TEST_USER_AGENT);
            return null;
        });

        // 오래된 이력
        transactionTemplate.execute(status -> {
            LoginHistory oldHistory = LoginHistory.success(testUser, "20200001", TEST_IP, TEST_USER_AGENT);
            setField(oldHistory, "attemptedAt", Instant.now().minus(30, ChronoUnit.DAYS));
            loginHistoryRepository.save(oldHistory);
            return null;
        });

        Instant startDate = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant endDate = Instant.now().plus(1, ChronoUnit.DAYS);

        // when
        Page<LoginHistoryResponse> result = getLoginHistoryForAdminService.getLoginHistories(
                1L, null, null, null, startDate, endDate, defaultPageRequest
        );

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("복합 필터 조회")
    void getLoginHistories_CombinedFilters_ReturnsFiltered() {
        // given
        transactionTemplate.execute(status -> {
            recordLoginSuccessService.recordSuccess(testUser, "20200001", TEST_IP, TEST_USER_AGENT);
            return null;
        });
        transactionTemplate.execute(status -> {
            recordLoginFailureService.recordFailure("20200001", TEST_IP, TEST_USER_AGENT, LoginFailureReason.INVALID_CREDENTIALS);
            return null;
        });
        transactionTemplate.execute(status -> {
            recordLoginFailureService.recordFailure("20220001", TEST_IP_2, TEST_USER_AGENT, LoginFailureReason.ACCOUNT_LOCKED);
            return null;
        });

        // when - studentId + success 복합 필터
        Page<LoginHistoryResponse> result = getLoginHistoryForAdminService.getLoginHistories(
                1L, "20200001", false, null, null, null, defaultPageRequest
        );

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().studentId()).isEqualTo("20200001");
        assertThat(result.getContent().getFirst().success()).isFalse();
    }

    @Test
    @DisplayName("빈 결과 조회")
    void getLoginHistories_NoData_ReturnsEmptyPage() {
        // when
        Page<LoginHistoryResponse> result = getLoginHistoryForAdminService.getLoginHistories(
                1L, null, null, null, null, null, defaultPageRequest
        );

        // then
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }
}
