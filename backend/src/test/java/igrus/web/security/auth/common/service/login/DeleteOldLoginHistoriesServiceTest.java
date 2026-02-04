package igrus.web.security.auth.common.service.login;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.LoginHistory;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeleteOldLoginHistoriesService 통합 테스트")
class DeleteOldLoginHistoriesServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private DeleteOldLoginHistoriesService deleteOldLoginHistoriesService;

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
    @DisplayName("지정 시각 이전 히스토리 삭제")
    void deleteOldHistories_DeletesOldRecords() {
        // given
        User user = createAndSaveUser(TEST_STUDENT_ID, "test@inha.edu", UserRole.MEMBER);

        // 현재 시각의 히스토리 생성
        transactionTemplate.execute(status -> {
            recordLoginSuccessService.recordSuccess(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);
            return null;
        });

        // 오래된 히스토리 직접 생성 (attemptedAt을 과거로 설정)
        transactionTemplate.execute(status -> {
            LoginHistory oldHistory = LoginHistory.success(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);
            setField(oldHistory, "attemptedAt", Instant.now().minus(400, ChronoUnit.DAYS));
            loginHistoryRepository.save(oldHistory);
            return null;
        });

        assertThat(loginHistoryRepository.findAll()).hasSize(2);

        // when
        Instant cutoffDate = Instant.now().minus(365, ChronoUnit.DAYS);
        int deletedCount = transactionTemplate.execute(status ->
                deleteOldLoginHistoriesService.deleteOldHistories(cutoffDate)
        );

        // then
        assertThat(deletedCount).isEqualTo(1);
        assertThat(loginHistoryRepository.findAll()).hasSize(1);
    }
}
