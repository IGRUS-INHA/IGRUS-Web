package igrus.web.user.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.domain.AccountStatusChangeHistory;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.dto.response.AccountStatusChangeHistoryResponse;
import igrus.web.user.repository.AccountStatusChangeHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GetAccountStatusChangeHistoryService 통합 테스트")
class GetAccountStatusChangeHistoryServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetAccountStatusChangeHistoryService getAccountStatusChangeHistoryService;

    @Autowired
    private AccountStatusChangeHistoryRepository accountStatusChangeHistoryRepository;

    private User targetUser;
    private User adminUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        setUpBase();
        transactionTemplate.execute(status -> {
            targetUser = createAndSaveUser("20220001", "target@inha.edu", UserRole.MEMBER);
            adminUser = createAndSaveUser("20200001", "admin@inha.edu", UserRole.ADMIN);
            anotherUser = createAndSaveUser("20230001", "another@inha.edu", UserRole.MEMBER);
            return null;
        });
    }

    private void createHistory(User user, User changedBy, AccountChangeType changeType,
                               String previousValue, String newValue, String reason) {
        transactionTemplate.execute(status -> {
            AccountStatusChangeHistory history = AccountStatusChangeHistory.create(
                    user.getId(), user.getStudentId(),
                    changedBy.getId(), changedBy.getStudentId(),
                    changeType, previousValue, newValue, reason
            );
            accountStatusChangeHistoryRepository.save(history);
            return null;
        });
    }

    @Nested
    @DisplayName("필터 조회")
    class FilterTests {

        @Test
        @DisplayName("필터 없이 전체 이력 조회")
        void getHistories_NoFilter_ReturnsAll() {
            createHistory(targetUser, adminUser, AccountChangeType.APPROVAL, "ASSOCIATE", "MEMBER", "승인");
            createHistory(anotherUser, adminUser, AccountChangeType.ROLE_CHANGE, "MEMBER", "OPERATOR", "변경");

            Page<AccountStatusChangeHistoryResponse> result = getAccountStatusChangeHistoryService.getHistories(
                    null, null, null, null, null,
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            );

            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("userId로 필터링 조회")
        void getHistories_ByUserId_ReturnsFiltered() {
            createHistory(targetUser, adminUser, AccountChangeType.APPROVAL, "ASSOCIATE", "MEMBER", "승인");
            createHistory(anotherUser, adminUser, AccountChangeType.ROLE_CHANGE, "MEMBER", "OPERATOR", "변경");

            Page<AccountStatusChangeHistoryResponse> result = getAccountStatusChangeHistoryService.getHistories(
                    targetUser.getId(), null, null, null, null,
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            );

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).userId()).isEqualTo(targetUser.getId());
        }

        @Test
        @DisplayName("changedByUserId로 필터링 조회")
        void getHistories_ByChangedByUserId_ReturnsFiltered() {
            createHistory(targetUser, adminUser, AccountChangeType.APPROVAL, "ASSOCIATE", "MEMBER", "승인");
            createHistory(anotherUser, anotherUser, AccountChangeType.WITHDRAWAL, "ACTIVE", "WITHDRAWN", "탈퇴");

            Page<AccountStatusChangeHistoryResponse> result = getAccountStatusChangeHistoryService.getHistories(
                    null, adminUser.getId(), null, null, null,
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            );

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).changedByUserId()).isEqualTo(adminUser.getId());
        }

        @Test
        @DisplayName("changeType으로 필터링 조회")
        void getHistories_ByChangeType_ReturnsFiltered() {
            createHistory(targetUser, adminUser, AccountChangeType.APPROVAL, "ASSOCIATE", "MEMBER", "승인");
            createHistory(targetUser, adminUser, AccountChangeType.SUSPENSION, "ACTIVE", "SUSPENDED", "정지");

            Page<AccountStatusChangeHistoryResponse> result = getAccountStatusChangeHistoryService.getHistories(
                    null, null, AccountChangeType.SUSPENSION, null, null,
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            );

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).changeType()).isEqualTo(AccountChangeType.SUSPENSION);
        }

        @Test
        @DisplayName("복합 필터 조회")
        void getHistories_MultipleFilters_ReturnsFiltered() {
            createHistory(targetUser, adminUser, AccountChangeType.APPROVAL, "ASSOCIATE", "MEMBER", "승인");
            createHistory(targetUser, adminUser, AccountChangeType.SUSPENSION, "ACTIVE", "SUSPENDED", "정지");
            createHistory(anotherUser, adminUser, AccountChangeType.APPROVAL, "ASSOCIATE", "MEMBER", "승인");

            Page<AccountStatusChangeHistoryResponse> result = getAccountStatusChangeHistoryService.getHistories(
                    targetUser.getId(), adminUser.getId(), AccountChangeType.APPROVAL, null, null,
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            );

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("기간으로 필터링 조회")
        void getHistories_ByDateRange_ReturnsFiltered() {
            createHistory(targetUser, adminUser, AccountChangeType.APPROVAL, "ASSOCIATE", "MEMBER", "승인");

            Instant startDate = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant endDate = Instant.now().plus(1, ChronoUnit.HOURS);

            Page<AccountStatusChangeHistoryResponse> result = getAccountStatusChangeHistoryService.getHistories(
                    null, null, null, startDate, endDate,
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            );

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("페이지네이션")
    class PaginationTests {

        @Test
        @DisplayName("페이지네이션이 올바르게 동작한다")
        void getHistories_Pagination_ReturnsPagedResults() {
            for (int i = 0; i < 5; i++) {
                createHistory(targetUser, adminUser, AccountChangeType.ROLE_CHANGE,
                        "MEMBER", "OPERATOR", "변경 " + i);
            }

            Page<AccountStatusChangeHistoryResponse> firstPage = getAccountStatusChangeHistoryService.getHistories(
                    null, null, null, null, null,
                    PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"))
            );

            assertThat(firstPage.getTotalElements()).isEqualTo(5);
            assertThat(firstPage.getContent()).hasSize(2);
            assertThat(firstPage.getTotalPages()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("빈 결과")
    class EmptyResultTests {

        @Test
        @DisplayName("조건에 맞는 이력이 없으면 빈 페이지를 반환한다")
        void getHistories_NoMatch_ReturnsEmpty() {
            Page<AccountStatusChangeHistoryResponse> result = getAccountStatusChangeHistoryService.getHistories(
                    Long.MAX_VALUE, null, null, null, null,
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            );

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }
}
