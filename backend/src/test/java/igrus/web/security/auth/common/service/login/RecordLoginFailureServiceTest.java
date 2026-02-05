package igrus.web.security.auth.common.service.login;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.LoginFailureReason;
import igrus.web.security.auth.common.domain.LoginHistory;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecordLoginFailureService 통합 테스트")
class RecordLoginFailureServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private RecordLoginFailureService recordLoginFailureService;

    private static final String TEST_STUDENT_ID = "12345678";
    private static final String TEST_IP_ADDRESS = "192.168.1.100";
    private static final String TEST_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Nested
    @DisplayName("recordFailure")
    class RecordFailureTest {

        @Test
        @DisplayName("사용자 없이 로그인 실패 시 히스토리가 저장됨")
        void recordFailure_WithoutUser_SavesHistory() {
            // when
            transactionTemplate.execute(status -> {
                recordLoginFailureService.recordFailure(TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                        LoginFailureReason.INVALID_CREDENTIALS);
                return null;
            });

            // then
            List<LoginHistory> histories = loginHistoryRepository.findAll();
            assertThat(histories).hasSize(1);

            LoginHistory history = histories.get(0);
            assertThat(history.isSuccess()).isFalse();
            assertThat(history.getUser()).isNull();
            assertThat(history.getFailureReason()).isEqualTo(LoginFailureReason.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("사용자와 함께 로그인 실패 시 히스토리가 저장됨")
        void recordFailure_WithUser_SavesHistory() {
            // given
            User user = createAndSaveUser(TEST_STUDENT_ID, "test@inha.edu", UserRole.MEMBER);

            // when
            transactionTemplate.execute(status -> {
                recordLoginFailureService.recordFailure(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                        LoginFailureReason.ACCOUNT_SUSPENDED);
                return null;
            });

            // then
            List<LoginHistory> histories = loginHistoryRepository.findAll();
            assertThat(histories).hasSize(1);

            LoginHistory history = histories.get(0);
            assertThat(history.isSuccess()).isFalse();
            assertThat(history.getUser()).isNotNull();
            assertThat(history.getFailureReason()).isEqualTo(LoginFailureReason.ACCOUNT_SUSPENDED);
        }
    }
}
