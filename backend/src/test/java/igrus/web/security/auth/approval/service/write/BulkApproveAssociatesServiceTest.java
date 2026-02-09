package igrus.web.security.auth.approval.service.write;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.security.auth.approval.domain.AssociateDecisionType;
import igrus.web.security.auth.approval.exception.BulkApprovalEmptyException;
import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserRoleHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BulkApproveAssociatesService 통합 테스트")
class BulkApproveAssociatesServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private BulkApproveAssociatesService bulkApproveAssociatesService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        setUpBase();

        adminUser = createAndSaveUser("20200001", "admin@inha.edu", UserRole.ADMIN);
    }

    @Nested
    @DisplayName("일괄 승인")
    class BulkApprovalTest {

        @Test
        @DisplayName("다수 준회원 일괄 승인 성공 - 5명 모두 MEMBER로 변경 [APR-020]")
        void approveBulk_AllAssociates_AllBecomeMember() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);
            User associate2 = createAndSaveUser("20230011", "a11@inha.edu", UserRole.ASSOCIATE);
            User associate3 = createAndSaveUser("20230012", "a12@inha.edu", UserRole.ASSOCIATE);
            User associate4 = createAndSaveUser("20230013", "a13@inha.edu", UserRole.ASSOCIATE);
            User associate5 = createAndSaveUser("20230014", "a14@inha.edu", UserRole.ASSOCIATE);

            List<Long> userIds = List.of(associate1.getId(), associate2.getId(), associate3.getId(), associate4.getId(), associate5.getId());

            // when
            int approvedCount = bulkApproveAssociatesService.approveBulk(userIds, adminUser.getId());

            // then
            assertThat(approvedCount).isEqualTo(5);

            assertThat(userRepository.findById(associate1.getId()).orElseThrow().getRole()).isEqualTo(UserRole.MEMBER);
            assertThat(userRepository.findById(associate2.getId()).orElseThrow().getRole()).isEqualTo(UserRole.MEMBER);
            assertThat(userRepository.findById(associate3.getId()).orElseThrow().getRole()).isEqualTo(UserRole.MEMBER);
            assertThat(userRepository.findById(associate4.getId()).orElseThrow().getRole()).isEqualTo(UserRole.MEMBER);
            assertThat(userRepository.findById(associate5.getId()).orElseThrow().getRole()).isEqualTo(UserRole.MEMBER);
        }

        @Test
        @DisplayName("일부 준회원 선택 후 일괄 승인 - 선택된 3명만 MEMBER로 변경 [APR-021]")
        void approveBulk_PartialSelection_OnlySelectedBecomesMember() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);
            User associate2 = createAndSaveUser("20230011", "a11@inha.edu", UserRole.ASSOCIATE);
            User associate3 = createAndSaveUser("20230012", "a12@inha.edu", UserRole.ASSOCIATE);
            User associate4 = createAndSaveUser("20230013", "a13@inha.edu", UserRole.ASSOCIATE);
            User associate5 = createAndSaveUser("20230014", "a14@inha.edu", UserRole.ASSOCIATE);

            List<Long> selectedUserIds = List.of(associate1.getId(), associate2.getId(), associate3.getId());

            // when
            int approvedCount = bulkApproveAssociatesService.approveBulk(selectedUserIds, adminUser.getId());

            // then
            assertThat(approvedCount).isEqualTo(3);

            assertThat(userRepository.findById(associate1.getId()).orElseThrow().getRole()).isEqualTo(UserRole.MEMBER);
            assertThat(userRepository.findById(associate2.getId()).orElseThrow().getRole()).isEqualTo(UserRole.MEMBER);
            assertThat(userRepository.findById(associate3.getId()).orElseThrow().getRole()).isEqualTo(UserRole.MEMBER);

            assertThat(userRepository.findById(associate4.getId()).orElseThrow().getRole()).isEqualTo(UserRole.ASSOCIATE);
            assertThat(userRepository.findById(associate5.getId()).orElseThrow().getRole()).isEqualTo(UserRole.ASSOCIATE);
        }

        @Test
        @DisplayName("일괄 승인 시 각각 AssociateDecision 기록 [APR-022]")
        void approveBulk_EachUserHasAssociateDecision() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);
            User associate2 = createAndSaveUser("20230011", "a11@inha.edu", UserRole.ASSOCIATE);

            List<Long> userIds = List.of(associate1.getId(), associate2.getId());

            // when
            bulkApproveAssociatesService.approveBulk(userIds, adminUser.getId());

            // then
            AssociateDecision decision1 = associateDecisionRepository.findByUserId(associate1.getId()).orElseThrow();
            AssociateDecision decision2 = associateDecisionRepository.findByUserId(associate2.getId()).orElseThrow();

            assertThat(decision1.getType()).isEqualTo(AssociateDecisionType.APPROVED);
            assertThat(decision1.getDecidedBy()).isEqualTo(adminUser.getId());
            assertThat(decision2.getType()).isEqualTo(AssociateDecisionType.APPROVED);
            assertThat(decision2.getDecidedBy()).isEqualTo(adminUser.getId());
        }

        @Test
        @DisplayName("일괄 승인 시 역할 변경 이력 개별 기록 [APR-023]")
        void approveBulk_EachUserHasRoleHistory() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);
            User associate2 = createAndSaveUser("20230011", "a11@inha.edu", UserRole.ASSOCIATE);

            List<Long> userIds = List.of(associate1.getId(), associate2.getId());

            // when
            bulkApproveAssociatesService.approveBulk(userIds, adminUser.getId());

            // then
            List<UserRoleHistory> histories = userRoleHistoryRepository.findAll();
            assertThat(histories).hasSize(2);
        }

        @Test
        @DisplayName("일괄 승인 시 각 사용자의 리프레시 토큰 만료 [APR-025]")
        void approveBulk_RevokesRefreshTokensForEachUser() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);
            User associate2 = createAndSaveUser("20230011", "a11@inha.edu", UserRole.ASSOCIATE);

            refreshTokenRepository.save(RefreshToken.createInitial(associate1, "token-1", 86400000));
            refreshTokenRepository.save(RefreshToken.createInitial(associate2, "token-2", 86400000));

            List<Long> userIds = List.of(associate1.getId(), associate2.getId());

            // when
            bulkApproveAssociatesService.approveBulk(userIds, adminUser.getId());

            // then
            assertThat(refreshTokenRepository.findByUserIdAndRevokedFalse(associate1.getId())).isEmpty();
            assertThat(refreshTokenRepository.findByUserIdAndRevokedFalse(associate2.getId())).isEmpty();
        }

        @Test
        @DisplayName("선택 없이 일괄 승인 시도 시 예외 발생 [APR-024]")
        void approveBulk_EmptyList_ThrowsException() {
            // given
            List<Long> emptyUserIds = Collections.emptyList();

            // when & then
            assertThatThrownBy(() -> bulkApproveAssociatesService.approveBulk(emptyUserIds, adminUser.getId()))
                    .isInstanceOf(BulkApprovalEmptyException.class);
        }

        @Test
        @DisplayName("null 목록으로 일괄 승인 시도 시 예외 발생 [APR-024-2]")
        void approveBulk_NullList_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> bulkApproveAssociatesService.approveBulk(null, adminUser.getId()))
                    .isInstanceOf(BulkApprovalEmptyException.class);
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCaseTest {

        @Test
        @DisplayName("일괄 승인 시 일부 사용자가 존재하지 않는 경우 나머지는 정상 처리")
        void approveBulk_SomeUsersNotFound_ProcessesOthers() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);
            Long nonExistentUserId = 999L;

            List<Long> userIds = List.of(associate1.getId(), nonExistentUserId);

            // when
            int approvedCount = bulkApproveAssociatesService.approveBulk(userIds, adminUser.getId());

            // then
            assertThat(approvedCount).isEqualTo(1);
            assertThat(userRepository.findById(associate1.getId()).orElseThrow().getRole()).isEqualTo(UserRole.MEMBER);
        }

        @Test
        @DisplayName("일괄 승인 시 일부 사용자가 ASSOCIATE가 아닌 경우 나머지는 정상 처리")
        void approveBulk_SomeUsersNotAssociate_ProcessesOthers() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);
            User member1 = createAndSaveUser("20230011", "m11@inha.edu", UserRole.MEMBER);

            List<Long> userIds = List.of(associate1.getId(), member1.getId());

            // when
            int approvedCount = bulkApproveAssociatesService.approveBulk(userIds, adminUser.getId());

            // then
            assertThat(approvedCount).isEqualTo(1);
            assertThat(userRepository.findById(associate1.getId()).orElseThrow().getRole()).isEqualTo(UserRole.MEMBER);
            assertThat(userRepository.findById(member1.getId()).orElseThrow().getRole()).isEqualTo(UserRole.MEMBER);
        }
    }
}
