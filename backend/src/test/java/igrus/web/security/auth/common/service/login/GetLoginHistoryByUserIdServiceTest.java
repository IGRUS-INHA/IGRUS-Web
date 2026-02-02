package igrus.web.security.auth.common.service.login;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.LoginFailureReason;
import igrus.web.security.auth.common.domain.LoginHistory;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GetLoginHistoryByUserIdService 통합 테스트")
class GetLoginHistoryByUserIdServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetLoginHistoryByUserIdService getLoginHistoryByUserIdService;

    @Autowired
    private RecordLoginSuccessService recordLoginSuccessService;

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
    @DisplayName("사용자별 히스토리 조회")
    void getHistoryByUserId_ReturnsUserHistory() {
        // given
        User user = createAndSaveUser(TEST_STUDENT_ID, "test@inha.edu", UserRole.MEMBER);

        transactionTemplate.execute(status -> {
            recordLoginSuccessService.recordSuccess(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);
            recordLoginFailureService.recordFailure(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                    LoginFailureReason.INVALID_CREDENTIALS);
            return null;
        });

        // when
        Page<LoginHistory> result = transactionTemplate.execute(status ->
                getLoginHistoryByUserIdService.getHistoryByUserId(user.getId(), PageRequest.of(0, 10))
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
    }
}
