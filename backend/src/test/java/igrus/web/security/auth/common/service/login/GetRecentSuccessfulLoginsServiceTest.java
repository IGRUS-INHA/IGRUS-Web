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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GetRecentSuccessfulLoginsService 통합 테스트")
class GetRecentSuccessfulLoginsServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetRecentSuccessfulLoginsService getRecentSuccessfulLoginsService;

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
    @DisplayName("최근 로그인 성공 기록 조회 (최대 10건)")
    void getRecentSuccessfulLogins_ReturnsRecentLogins() {
        // given
        User user = createAndSaveUser(TEST_STUDENT_ID, "test@inha.edu", UserRole.MEMBER);

        transactionTemplate.execute(status -> {
            for (int i = 0; i < 15; i++) {
                recordLoginSuccessService.recordSuccess(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);
            }
            // 실패 기록도 추가
            recordLoginFailureService.recordFailure(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                    LoginFailureReason.INVALID_CREDENTIALS);
            return null;
        });

        // when
        List<LoginHistory> result = transactionTemplate.execute(status ->
                getRecentSuccessfulLoginsService.getRecentSuccessfulLogins(user.getId())
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(10);
        assertThat(result).allMatch(LoginHistory::isSuccess);
    }
}
