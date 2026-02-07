package igrus.web.admin.user.service;

import igrus.web.admin.user.dto.UserRoleHistoryResponse;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserRoleHistory;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import igrus.web.admin.user.exception.InvalidDateRangeException;

@DisplayName("GetUserRoleHistoryService 통합 테스트")
class GetUserRoleHistoryServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetUserRoleHistoryService getUserRoleHistoryService;

    private User userA;
    private User userB;

    private static final PageRequest DEFAULT_PAGE = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

    @BeforeEach
    void setUp() {
        setUpBase();
        userA = createAndSaveUser("20200001", "userA@inha.edu", UserRole.MEMBER);
        userB = createAndSaveUser("20200002", "userB@inha.edu", UserRole.OPERATOR);
    }

    private UserRoleHistory saveHistory(User user, UserRole previousRole, UserRole newRole, String reason) {
        UserRoleHistory history = UserRoleHistory.create(user, previousRole, newRole, reason);
        return userRoleHistoryRepository.save(history);
    }

    @Test
    @DisplayName("전체 이력 조회 (필터 없음)")
    void getUserRoleHistories_NoFilter_ReturnsAll() {
        saveHistory(userA, UserRole.ASSOCIATE, UserRole.MEMBER, null);
        saveHistory(userB, UserRole.MEMBER, UserRole.OPERATOR, null);

        Page<UserRoleHistoryResponse> result = getUserRoleHistoryService.getUserRoleHistories(
                null, null, null, null, null, null, DEFAULT_PAGE);

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("userId 필터 조회")
    void getUserRoleHistories_WithUserIdFilter_ReturnsFiltered() {
        saveHistory(userA, UserRole.ASSOCIATE, UserRole.MEMBER, null);
        saveHistory(userB, UserRole.MEMBER, UserRole.OPERATOR, null);

        Page<UserRoleHistoryResponse> result = getUserRoleHistoryService.getUserRoleHistories(
                userA.getId(), null, null, null, null, null, DEFAULT_PAGE);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().userId()).isEqualTo(userA.getId());
    }

    @Test
    @DisplayName("previousRole 필터 조회")
    void getUserRoleHistories_WithPreviousRoleFilter_ReturnsFiltered() {
        saveHistory(userA, UserRole.ASSOCIATE, UserRole.MEMBER, null);
        saveHistory(userB, UserRole.MEMBER, UserRole.OPERATOR, null);

        Page<UserRoleHistoryResponse> result = getUserRoleHistoryService.getUserRoleHistories(
                null, UserRole.ASSOCIATE, null, null, null, null, DEFAULT_PAGE);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().previousRole()).isEqualTo(UserRole.ASSOCIATE);
    }

    @Test
    @DisplayName("newRole 필터 조회")
    void getUserRoleHistories_WithNewRoleFilter_ReturnsFiltered() {
        saveHistory(userA, UserRole.ASSOCIATE, UserRole.MEMBER, null);
        saveHistory(userB, UserRole.MEMBER, UserRole.OPERATOR, null);

        Page<UserRoleHistoryResponse> result = getUserRoleHistoryService.getUserRoleHistories(
                null, null, UserRole.OPERATOR, null, null, null, DEFAULT_PAGE);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().newRole()).isEqualTo(UserRole.OPERATOR);
    }

    @Test
    @DisplayName("changedBy 필터 조회")
    void getUserRoleHistories_WithChangedByFilter_ReturnsFiltered() {
        UserRoleHistory h1 = saveHistory(userA, UserRole.ASSOCIATE, UserRole.MEMBER, null);
        UserRoleHistory h2 = saveHistory(userB, UserRole.MEMBER, UserRole.OPERATOR, null);

        transactionTemplate.execute(status -> {
            entityManager.createNativeQuery(
                    "UPDATE user_role_histories SET user_role_histories_created_by = ? WHERE user_role_histories_id = ?")
                    .setParameter(1, 100L).setParameter(2, h1.getId()).executeUpdate();
            entityManager.createNativeQuery(
                    "UPDATE user_role_histories SET user_role_histories_created_by = ? WHERE user_role_histories_id = ?")
                    .setParameter(1, 200L).setParameter(2, h2.getId()).executeUpdate();
            return null;
        });

        Page<UserRoleHistoryResponse> result = getUserRoleHistoryService.getUserRoleHistories(
                null, null, null, 100L, null, null, DEFAULT_PAGE);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().changedBy()).isEqualTo(100L);
    }

    @Test
    @DisplayName("날짜 범위 필터 조회")
    void getUserRoleHistories_WithDateFilter_ReturnsFiltered() {
        saveHistory(userA, UserRole.ASSOCIATE, UserRole.MEMBER, null);

        Instant startDate = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant endDate = Instant.now().plus(1, ChronoUnit.HOURS);

        Page<UserRoleHistoryResponse> result = getUserRoleHistoryService.getUserRoleHistories(
                null, null, null, null, startDate, endDate, DEFAULT_PAGE);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("날짜 범위 밖 데이터 필터링")
    void getUserRoleHistories_WithOutOfRangeDateFilter_ReturnsEmpty() {
        saveHistory(userA, UserRole.ASSOCIATE, UserRole.MEMBER, null);

        Instant futureStart = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant futureEnd = Instant.now().plus(2, ChronoUnit.DAYS);

        Page<UserRoleHistoryResponse> result = getUserRoleHistoryService.getUserRoleHistories(
                null, null, null, null, futureStart, futureEnd, DEFAULT_PAGE);

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("복합 필터 조합")
    void getUserRoleHistories_WithMultipleFilters_ReturnsFiltered() {
        saveHistory(userA, UserRole.ASSOCIATE, UserRole.MEMBER, null);
        saveHistory(userA, UserRole.MEMBER, UserRole.OPERATOR, null);
        saveHistory(userB, UserRole.ASSOCIATE, UserRole.MEMBER, null);

        Page<UserRoleHistoryResponse> result = getUserRoleHistoryService.getUserRoleHistories(
                userA.getId(), UserRole.ASSOCIATE, UserRole.MEMBER, null, null, null, DEFAULT_PAGE);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().userId()).isEqualTo(userA.getId());
        assertThat(result.getContent().getFirst().previousRole()).isEqualTo(UserRole.ASSOCIATE);
    }

    @Test
    @DisplayName("빈 결과 처리")
    void getUserRoleHistories_NoData_ReturnsEmptyPage() {
        Page<UserRoleHistoryResponse> result = getUserRoleHistoryService.getUserRoleHistories(
                null, null, null, null, null, null, DEFAULT_PAGE);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("페이지네이션 동작")
    void getUserRoleHistories_WithPagination_ReturnsPaginated() {
        saveHistory(userA, UserRole.ASSOCIATE, UserRole.MEMBER, null);
        saveHistory(userA, UserRole.MEMBER, UserRole.OPERATOR, null);
        saveHistory(userA, UserRole.OPERATOR, UserRole.ADMIN, null);

        PageRequest smallPage = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserRoleHistoryResponse> result = getUserRoleHistoryService.getUserRoleHistories(
                null, null, null, null, null, null, smallPage);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("DTO 매핑 정확성")
    void getUserRoleHistories_DtoMapping_IsCorrect() {
        saveHistory(userA, UserRole.ASSOCIATE, UserRole.MEMBER, "승급 사유");

        Page<UserRoleHistoryResponse> result = getUserRoleHistoryService.getUserRoleHistories(
                null, null, null, null, null, null, DEFAULT_PAGE);

        UserRoleHistoryResponse response = result.getContent().getFirst();
        assertThat(response.id()).isNotNull();
        assertThat(response.userId()).isEqualTo(userA.getId());
        assertThat(response.userName()).isEqualTo("테스트유저");
        assertThat(response.studentId()).isEqualTo("20200001");
        assertThat(response.previousRole()).isEqualTo(UserRole.ASSOCIATE);
        assertThat(response.newRole()).isEqualTo(UserRole.MEMBER);
        assertThat(response.reason()).isEqualTo("승급 사유");
        assertThat(response.changedAt()).isNotNull();
    }

    @Test
    @DisplayName("탈퇴한 사용자의 이력 조회 시 사용자 정보가 '탈퇴한 사용자'로 표시된다")
    void getUserRoleHistories_WithWithdrawnUser_ReturnsWithdrawnDisplayName() {
        saveHistory(userA, UserRole.ASSOCIATE, UserRole.MEMBER, "승급");

        transactionTemplate.execute(status -> {
            entityManager.createNativeQuery(
                    "UPDATE users SET users_status = 'WITHDRAWN', users_deleted = true, users_deleted_at = NOW() " +
                            "WHERE users_id = :userId")
                    .setParameter("userId", userA.getId())
                    .executeUpdate();
            entityManager.flush();
            entityManager.clear();
            return null;
        });

        Page<UserRoleHistoryResponse> result = getUserRoleHistoryService.getUserRoleHistories(
                null, null, null, null, null, null, DEFAULT_PAGE);

        assertThat(result.getTotalElements()).isEqualTo(1);
        UserRoleHistoryResponse response = result.getContent().getFirst();
        assertThat(response.userId()).isEqualTo(userA.getId());
        assertThat(response.userName()).isEqualTo("탈퇴한 사용자");
        assertThat(response.studentId()).isNull();
        assertThat(response.previousRole()).isEqualTo(UserRole.ASSOCIATE);
        assertThat(response.newRole()).isEqualTo(UserRole.MEMBER);
    }

    @Test
    @DisplayName("탈퇴한 사용자의 userId로 필터링 시 이력이 조회된다")
    void getUserRoleHistories_FilterByWithdrawnUserId_ReturnsResults() {
        saveHistory(userA, UserRole.ASSOCIATE, UserRole.MEMBER, "승급");

        Long savedUserId = userA.getId();

        transactionTemplate.execute(status -> {
            entityManager.createNativeQuery(
                    "UPDATE users SET users_status = 'WITHDRAWN', users_deleted = true, users_deleted_at = NOW() " +
                            "WHERE users_id = :userId")
                    .setParameter("userId", savedUserId)
                    .executeUpdate();
            entityManager.flush();
            entityManager.clear();
            return null;
        });

        Page<UserRoleHistoryResponse> result = getUserRoleHistoryService.getUserRoleHistories(
                savedUserId, null, null, null, null, null, DEFAULT_PAGE);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().userId()).isEqualTo(savedUserId);
        assertThat(result.getContent().getFirst().userName()).isEqualTo("탈퇴한 사용자");
    }

    @Test
    @DisplayName("시작 일시가 종료 일시보다 이후일 때 예외 발생")
    void getUserRoleHistories_WithInvertedDateRange_ThrowsException() {
        Instant startDate = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant endDate = Instant.now();

        assertThatThrownBy(() -> getUserRoleHistoryService.getUserRoleHistories(
                null, null, null, null, startDate, endDate, DEFAULT_PAGE))
                .isInstanceOf(InvalidDateRangeException.class);
    }
}
