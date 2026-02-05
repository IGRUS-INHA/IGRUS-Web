package igrus.web.security.auth.common.service.login;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.LoginHistory;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecordLoginSuccessService 통합 테스트")
class RecordLoginSuccessServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private RecordLoginSuccessService recordLoginSuccessService;

    private static final String TEST_STUDENT_ID = "12345678";
    private static final String TEST_IP_ADDRESS = "192.168.1.100";
    private static final String TEST_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Test
    @DisplayName("로그인 성공 시 히스토리가 저장됨")
    void recordSuccess_SavesHistory() {
        // given
        User user = createAndSaveUser(TEST_STUDENT_ID, "test@inha.edu", UserRole.MEMBER);

        // when
        transactionTemplate.execute(status -> {
            recordLoginSuccessService.recordSuccess(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);
            return null;
        });

        // then
        List<LoginHistory> histories = loginHistoryRepository.findAll();
        assertThat(histories).hasSize(1);

        LoginHistory history = histories.get(0);
        assertThat(history.isSuccess()).isTrue();
        assertThat(history.getStudentId()).isEqualTo(TEST_STUDENT_ID);
        assertThat(history.getIpAddress()).isEqualTo(TEST_IP_ADDRESS);
        assertThat(history.getUserAgent()).isEqualTo(TEST_USER_AGENT);
        assertThat(history.getFailureReason()).isNull();
    }
}
