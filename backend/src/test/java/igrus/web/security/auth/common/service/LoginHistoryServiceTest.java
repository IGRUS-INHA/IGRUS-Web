package igrus.web.security.auth.common.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoginHistoryService 통합 테스트")
class LoginHistoryServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private LoginHistoryService loginHistoryService;

    private static final String TEST_STUDENT_ID = "12345678";
    private static final String TEST_IP_ADDRESS = "192.168.1.100";
    private static final String TEST_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Nested
    @DisplayName("recordSuccess")
    class RecordSuccessTest {

        @Test
        @DisplayName("로그인 성공 시 히스토리가 저장됨")
        void recordSuccess_SavesHistory() {
            // given
            User user = createAndSaveUser(TEST_STUDENT_ID, "test@inha.edu", UserRole.MEMBER);

            // when
            transactionTemplate.execute(status -> {
                loginHistoryService.recordSuccess(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);
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

    @Nested
    @DisplayName("recordFailure")
    class RecordFailureTest {

        @Test
        @DisplayName("사용자 없이 로그인 실패 시 히스토리가 저장됨")
        void recordFailure_WithoutUser_SavesHistory() {
            // when
            transactionTemplate.execute(status -> {
                loginHistoryService.recordFailure(TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
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
                loginHistoryService.recordFailure(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
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

    @Nested
    @DisplayName("getHistoryByUserId")
    class GetHistoryByUserIdTest {

        @Test
        @DisplayName("사용자별 히스토리 조회")
        void getHistoryByUserId_ReturnsUserHistory() {
            // given
            User user = createAndSaveUser(TEST_STUDENT_ID, "test@inha.edu", UserRole.MEMBER);

            transactionTemplate.execute(status -> {
                loginHistoryService.recordSuccess(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);
                loginHistoryService.recordFailure(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                        LoginFailureReason.INVALID_CREDENTIALS);
                return null;
            });

            // when
            Page<LoginHistory> result = transactionTemplate.execute(status ->
                    loginHistoryService.getHistoryByUserId(user.getId(), PageRequest.of(0, 10))
            );

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getHistoryByStudentId")
    class GetHistoryByStudentIdTest {

        @Test
        @DisplayName("학번별 히스토리 조회")
        void getHistoryByStudentId_ReturnsStudentHistory() {
            // given
            transactionTemplate.execute(status -> {
                loginHistoryService.recordFailure(TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                        LoginFailureReason.INVALID_CREDENTIALS);
                loginHistoryService.recordFailure(TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                        LoginFailureReason.ACCOUNT_LOCKED);
                return null;
            });

            // when
            Page<LoginHistory> result = transactionTemplate.execute(status ->
                    loginHistoryService.getHistoryByStudentId(TEST_STUDENT_ID, PageRequest.of(0, 10))
            );

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getRecentSuccessfulLogins")
    class GetRecentSuccessfulLoginsTest {

        @Test
        @DisplayName("최근 로그인 성공 기록 조회 (최대 10건)")
        void getRecentSuccessfulLogins_ReturnsRecentLogins() {
            // given
            User user = createAndSaveUser(TEST_STUDENT_ID, "test@inha.edu", UserRole.MEMBER);

            transactionTemplate.execute(status -> {
                for (int i = 0; i < 15; i++) {
                    loginHistoryService.recordSuccess(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);
                }
                // 실패 기록도 추가
                loginHistoryService.recordFailure(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT,
                        LoginFailureReason.INVALID_CREDENTIALS);
                return null;
            });

            // when
            List<LoginHistory> result = transactionTemplate.execute(status ->
                    loginHistoryService.getRecentSuccessfulLogins(user.getId())
            );

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(10);
            assertThat(result).allMatch(LoginHistory::isSuccess);
        }
    }

    @Nested
    @DisplayName("deleteOldHistories")
    class DeleteOldHistoriesTest {

        @Test
        @DisplayName("지정 시각 이전 히스토리 삭제")
        void deleteOldHistories_DeletesOldRecords() {
            // given
            User user = createAndSaveUser(TEST_STUDENT_ID, "test@inha.edu", UserRole.MEMBER);

            // 현재 시각의 히스토리 생성
            transactionTemplate.execute(status -> {
                loginHistoryService.recordSuccess(user, TEST_STUDENT_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);
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
                    loginHistoryService.deleteOldHistories(cutoffDate)
            );

            // then
            assertThat(deletedCount).isEqualTo(1);
            assertThat(loginHistoryRepository.findAll()).hasSize(1);
        }
    }
}
