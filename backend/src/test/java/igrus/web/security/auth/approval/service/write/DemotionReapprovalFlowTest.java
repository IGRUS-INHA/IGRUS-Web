package igrus.web.security.auth.approval.service.write;

import igrus.web.admin.user.service.ChangeUserRoleService;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.security.auth.approval.domain.AssociateDecisionType;
import igrus.web.security.auth.approval.dto.response.AssociateInfoResponse;
import igrus.web.security.auth.approval.dto.response.DemotedAssociateInfoResponse;
import igrus.web.security.auth.approval.exception.AssociateAlreadyDecidedException;
import igrus.web.security.auth.approval.service.read.GetDemotedAssociatesService;
import igrus.web.security.auth.approval.service.read.GetPendingAssociatesService;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("강등-재승인 플로우 통합 테스트")
class DemotionReapprovalFlowTest extends ServiceIntegrationTestBase {

    @Autowired
    private ApproveAssociateService approveAssociateService;

    @Autowired
    private RejectAssociateService rejectAssociateService;

    @Autowired
    private ChangeUserRoleService changeUserRoleService;

    @Autowired
    private GetPendingAssociatesService getPendingAssociatesService;

    @Autowired
    private GetDemotedAssociatesService getDemotedAssociatesService;

    @Autowired
    private BulkApproveAssociatesService bulkApproveAssociatesService;

    @Autowired
    private BulkRejectAssociatesService bulkRejectAssociatesService;

    private User adminUser;
    private User associateUser;

    @BeforeEach
    void setUp() {
        setUpBase();
        adminUser = createAndSaveUser("20200001", "admin@inha.edu", UserRole.ADMIN);
        associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);
    }

    @Nested
    @DisplayName("승인 → 강등 → 재승인")
    class ApprovalDemotionReapprovalTest {

        @Test
        @DisplayName("승인 후 강등하면 DEMOTED 기록이 active로 생성되고 기존 APPROVED는 비활성화됨")
        void approve_thenDemote_createsActiveDemotedRecord() {
            // given: 승인
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());

            // when: 강등
            changeUserRoleService.changeUserRole(associateUser.getId(), UserRole.ASSOCIATE, adminUser.getId());

            // then
            User updatedUser = userRepository.findById(associateUser.getId()).orElseThrow();
            assertThat(updatedUser.getRole()).isEqualTo(UserRole.ASSOCIATE);

            Optional<AssociateDecision> activeDecision = associateDecisionRepository.findByUserIdAndActiveTrue(associateUser.getId());
            assertThat(activeDecision).isPresent();
            assertThat(activeDecision.get().getType()).isEqualTo(AssociateDecisionType.DEMOTED);

            List<AssociateDecision> allDecisions = associateDecisionRepository.findAll();
            assertThat(allDecisions).hasSize(2);
            long activeCount = allDecisions.stream().filter(AssociateDecision::isActive).count();
            assertThat(activeCount).isEqualTo(1);
        }

        @Test
        @DisplayName("강등된 유저를 재승인하면 MEMBER로 변경되고 APPROVED 기록이 active로 생성됨")
        void demotedUser_reapproved_becomesMember() {
            // given: 승인 → 강등
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());
            changeUserRoleService.changeUserRole(associateUser.getId(), UserRole.ASSOCIATE, adminUser.getId());

            // when: 재승인
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());

            // then
            User updatedUser = userRepository.findById(associateUser.getId()).orElseThrow();
            assertThat(updatedUser.getRole()).isEqualTo(UserRole.MEMBER);

            Optional<AssociateDecision> activeDecision = associateDecisionRepository.findByUserIdAndActiveTrue(associateUser.getId());
            assertThat(activeDecision).isPresent();
            assertThat(activeDecision.get().getType()).isEqualTo(AssociateDecisionType.APPROVED);

            List<AssociateDecision> allDecisions = associateDecisionRepository.findAll();
            assertThat(allDecisions).hasSize(3);
            long activeCount = allDecisions.stream().filter(AssociateDecision::isActive).count();
            assertThat(activeCount).isEqualTo(1);
        }

        @Test
        @DisplayName("강등된 유저를 거절하려 하면 AssociateAlreadyDecidedException 발생")
        void demotedUser_rejected_throwsException() {
            // given: 승인 → 강등
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());
            changeUserRoleService.changeUserRole(associateUser.getId(), UserRole.ASSOCIATE, adminUser.getId());

            // when & then
            assertThatThrownBy(() -> rejectAssociateService.rejectAssociate(associateUser.getId(), adminUser.getId(), "재심사 결과 거절"))
                    .isInstanceOf(AssociateAlreadyDecidedException.class);
        }
    }

    @Nested
    @DisplayName("대기 목록 및 강등 목록 반영")
    class ListReflectionTest {

        @Test
        @DisplayName("강등된 유저가 대기 목록에 표시됨")
        void demotedUser_appearsInPendingList() {
            // given: 승인 → 강등
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());
            changeUserRoleService.changeUserRole(associateUser.getId(), UserRole.ASSOCIATE, adminUser.getId());

            // when
            Page<AssociateInfoResponse> pendingList = getPendingAssociatesService.getPendingAssociates(
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                    adminUser.getId()
            );

            // then
            assertThat(pendingList.getTotalElements()).isEqualTo(1);
            assertThat(pendingList.getContent().get(0).userId()).isEqualTo(associateUser.getId());
        }

        @Test
        @DisplayName("강등된 유저가 강등 목록에 표시됨")
        void demotedUser_appearsInDemotedList() {
            // given: 승인 → 강등
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());
            changeUserRoleService.changeUserRole(associateUser.getId(), UserRole.ASSOCIATE, adminUser.getId());

            // when
            Page<DemotedAssociateInfoResponse> demotedList = getDemotedAssociatesService.getDemotedAssociates(
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "decidedAt")),
                    adminUser.getId()
            );

            // then
            assertThat(demotedList.getTotalElements()).isEqualTo(1);
            assertThat(demotedList.getContent().get(0).userId()).isEqualTo(associateUser.getId());
        }

        @Test
        @DisplayName("재승인 후에는 대기 목록과 강등 목록에서 사라짐")
        void reapprovedUser_disappearsFromPendingAndDemotedList() {
            // given: 승인 → 강등 → 재승인
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());
            changeUserRoleService.changeUserRole(associateUser.getId(), UserRole.ASSOCIATE, adminUser.getId());
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());

            // when
            Page<AssociateInfoResponse> pendingList = getPendingAssociatesService.getPendingAssociates(
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                    adminUser.getId()
            );
            Page<DemotedAssociateInfoResponse> demotedList = getDemotedAssociatesService.getDemotedAssociates(
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "decidedAt")),
                    adminUser.getId()
            );

            // then
            assertThat(pendingList.getTotalElements()).isZero();
            assertThat(demotedList.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("다중 사이클")
    class MultipleCycleTest {

        @Test
        @DisplayName("승인 → 강등 → 재승인 → 재강등 시 이력이 모두 보존됨")
        void multipleCycles_allHistoryPreserved() {
            // 1차 승인
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());
            // 1차 강등
            changeUserRoleService.changeUserRole(associateUser.getId(), UserRole.ASSOCIATE, adminUser.getId());
            // 2차 승인
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());
            // 2차 강등
            changeUserRoleService.changeUserRole(associateUser.getId(), UserRole.ASSOCIATE, adminUser.getId());

            // then: 이력 4개 (APPROVED → DEMOTED → APPROVED → DEMOTED)
            List<AssociateDecision> allDecisions = associateDecisionRepository.findAll();
            assertThat(allDecisions).hasSize(4);

            long activeCount = allDecisions.stream().filter(AssociateDecision::isActive).count();
            assertThat(activeCount).isEqualTo(1);

            Optional<AssociateDecision> activeDecision = associateDecisionRepository.findByUserIdAndActiveTrue(associateUser.getId());
            assertThat(activeDecision).isPresent();
            assertThat(activeDecision.get().getType()).isEqualTo(AssociateDecisionType.DEMOTED);
        }
    }

    @Nested
    @DisplayName("OPERATOR/ADMIN → ASSOCIATE 강등")
    class NonMemberDemotionTest {

        @Test
        @DisplayName("OPERATOR를 ASSOCIATE로 강등해도 DEMOTED 기록이 생성됨")
        void operatorToAssociate_createsDemotedRecord() {
            // given: 승인 → OPERATOR 승격
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());
            changeUserRoleService.changeUserRole(associateUser.getId(), UserRole.OPERATOR, adminUser.getId());

            // when: OPERATOR → ASSOCIATE 강등
            changeUserRoleService.changeUserRole(associateUser.getId(), UserRole.ASSOCIATE, adminUser.getId());

            // then
            User updatedUser = userRepository.findById(associateUser.getId()).orElseThrow();
            assertThat(updatedUser.getRole()).isEqualTo(UserRole.ASSOCIATE);

            Optional<AssociateDecision> activeDecision = associateDecisionRepository.findByUserIdAndActiveTrue(associateUser.getId());
            assertThat(activeDecision).isPresent();
            assertThat(activeDecision.get().getType()).isEqualTo(AssociateDecisionType.DEMOTED);
        }

        @Test
        @DisplayName("ADMIN을 ASSOCIATE로 강등해도 DEMOTED 기록이 생성됨")
        void adminToAssociate_createsDemotedRecord() {
            // given: 승인 → ADMIN 승격 (adminUser2를 따로 만들어서 마지막 ADMIN 아니도록)
            User adminUser2 = createAndSaveUser("20200002", "admin2@inha.edu", UserRole.ADMIN);
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());
            changeUserRoleService.changeUserRole(associateUser.getId(), UserRole.ADMIN, adminUser.getId());

            // when: ADMIN → ASSOCIATE 강등
            changeUserRoleService.changeUserRole(associateUser.getId(), UserRole.ASSOCIATE, adminUser2.getId());

            // then
            User updatedUser = userRepository.findById(associateUser.getId()).orElseThrow();
            assertThat(updatedUser.getRole()).isEqualTo(UserRole.ASSOCIATE);

            Optional<AssociateDecision> activeDecision = associateDecisionRepository.findByUserIdAndActiveTrue(associateUser.getId());
            assertThat(activeDecision).isPresent();
            assertThat(activeDecision.get().getType()).isEqualTo(AssociateDecisionType.DEMOTED);
        }
    }

    @Nested
    @DisplayName("거절된 유저 재처리")
    class RejectedUserReprocessTest {

        @Test
        @DisplayName("거절된 ASSOCIATE를 재승인하면 MEMBER로 변경됨")
        void rejectedUser_reapproved_becomesMember() {
            // given: 거절
            rejectAssociateService.rejectAssociate(associateUser.getId(), adminUser.getId(), "거절 사유");

            // when: 재승인
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());

            // then
            User updatedUser = userRepository.findById(associateUser.getId()).orElseThrow();
            assertThat(updatedUser.getRole()).isEqualTo(UserRole.MEMBER);

            Optional<AssociateDecision> activeDecision = associateDecisionRepository.findByUserIdAndActiveTrue(associateUser.getId());
            assertThat(activeDecision).isPresent();
            assertThat(activeDecision.get().getType()).isEqualTo(AssociateDecisionType.APPROVED);

            List<AssociateDecision> allDecisions = associateDecisionRepository.findAll();
            assertThat(allDecisions).hasSize(2);
            long activeCount = allDecisions.stream().filter(AssociateDecision::isActive).count();
            assertThat(activeCount).isEqualTo(1);
        }

        @Test
        @DisplayName("거절된 ASSOCIATE를 재거절하려 하면 AssociateAlreadyDecidedException 발생")
        void rejectedUser_rerejected_throwsException() {
            // given: 거절
            rejectAssociateService.rejectAssociate(associateUser.getId(), adminUser.getId(), "첫 거절");

            // when & then
            assertThatThrownBy(() -> rejectAssociateService.rejectAssociate(associateUser.getId(), adminUser.getId(), "두번째 거절"))
                    .isInstanceOf(AssociateAlreadyDecidedException.class);
        }
    }

    @Nested
    @DisplayName("강등 유저 일괄 처리")
    class BulkProcessDemotedTest {

        @Test
        @DisplayName("강등된 유저가 포함된 일괄 승인 시 정상 처리됨")
        void bulkApprove_withDemotedUser_succeeds() {
            // given: 유저1 승인 → 강등, 유저2 신규 ASSOCIATE
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());
            changeUserRoleService.changeUserRole(associateUser.getId(), UserRole.ASSOCIATE, adminUser.getId());

            User newAssociate = createAndSaveUser("20230002", "new@inha.edu", UserRole.ASSOCIATE);

            // when
            int approvedCount = bulkApproveAssociatesService.approveBulk(
                    List.of(associateUser.getId(), newAssociate.getId()),
                    adminUser.getId()
            );

            // then
            assertThat(approvedCount).isEqualTo(2);

            User updatedUser1 = userRepository.findById(associateUser.getId()).orElseThrow();
            User updatedUser2 = userRepository.findById(newAssociate.getId()).orElseThrow();
            assertThat(updatedUser1.getRole()).isEqualTo(UserRole.MEMBER);
            assertThat(updatedUser2.getRole()).isEqualTo(UserRole.MEMBER);
        }

        @Test
        @DisplayName("강등된 유저가 포함된 일괄 거절 시 해당 유저는 skip됨")
        void bulkReject_withDemotedUser_skips() {
            // given: 승인 → 강등
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());
            changeUserRoleService.changeUserRole(associateUser.getId(), UserRole.ASSOCIATE, adminUser.getId());

            User newAssociate = createAndSaveUser("20230002", "new@inha.edu", UserRole.ASSOCIATE);

            // when
            int rejectedCount = bulkRejectAssociatesService.rejectBulk(
                    List.of(associateUser.getId(), newAssociate.getId()),
                    adminUser.getId(),
                    "일괄 거절 사유"
            );

            // then: 강등 유저는 skip, 신규만 거절
            assertThat(rejectedCount).isEqualTo(1);

            Optional<AssociateDecision> demotedDecision = associateDecisionRepository.findByUserIdAndActiveTrue(associateUser.getId());
            assertThat(demotedDecision).isPresent();
            assertThat(demotedDecision.get().getType()).isEqualTo(AssociateDecisionType.DEMOTED);
        }

        @Test
        @DisplayName("거절된 유저가 일괄 승인에 포함되면 정상 승인됨")
        void bulkApprove_withRejectedUser_approvesAll() {
            // given: 유저1 거절, 유저2 신규 ASSOCIATE
            rejectAssociateService.rejectAssociate(associateUser.getId(), adminUser.getId(), "거절 사유");
            User newAssociate = createAndSaveUser("20230002", "new@inha.edu", UserRole.ASSOCIATE);

            // when
            int approvedCount = bulkApproveAssociatesService.approveBulk(
                    List.of(associateUser.getId(), newAssociate.getId()),
                    adminUser.getId()
            );

            // then: 둘 다 승인
            assertThat(approvedCount).isEqualTo(2);

            User updatedUser1 = userRepository.findById(associateUser.getId()).orElseThrow();
            assertThat(updatedUser1.getRole()).isEqualTo(UserRole.MEMBER);

            User updatedUser2 = userRepository.findById(newAssociate.getId()).orElseThrow();
            assertThat(updatedUser2.getRole()).isEqualTo(UserRole.MEMBER);
        }
    }

}
