package igrus.web.security.auth.common.service.login;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.LoginFailureReason;
import igrus.web.security.auth.common.domain.LoginHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GetLoginHistoryByStudentIdService 통합 테스트")
class GetLoginHistoryByStudentIdServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetLoginHistoryByStudentIdService getLoginHistoryByStudentIdService;

    @Autowired
    private RecordLoginFailureService recordLoginFailureService;

    private static final String TEST_STUDENT_ID = "12345678";
    private static final String TEST_IP_ADDRESS = "192.168.1.100";
    private static final String TEST_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Test
    @DisplayName("학번별 히스토리 조회")
    void getHistoryByStudentId_ReturnsStudentHistory() {
        // given
        transactionTemplate.execute(status -> {
            recordLoginFailureService.recordFailure(TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                    LoginFailureReason.INVALID_CREDENTIALS);
            recordLoginFailureService.recordFailure(TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                    LoginFailureReason.ACCOUNT_LOCKED);
            return null;
        });

        // when
        Page<LoginHistory> result = transactionTemplate.execute(status ->
                getLoginHistoryByStudentIdService.getHistoryByStudentId(TEST_STUDENT_ID, PageRequest.of(0, 10))
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
    }
}
