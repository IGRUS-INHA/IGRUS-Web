package igrus.web.security.auth.approval.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.approval.dto.response.AssociateInfoResponse;
import igrus.web.security.auth.approval.exception.AdminRequiredException;
import igrus.web.security.auth.approval.exception.BulkApprovalEmptyException;
import igrus.web.security.auth.approval.exception.LastAdminCannotChangeException;
import igrus.web.security.auth.approval.exception.UserNotAssociateException;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserRoleHistory;
import igrus.web.user.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MemberApprovalService 통합 테스트")
class MemberApprovalServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private MemberApprovalService memberApprovalService;

    private User adminUser;
    private User associateUser;
    private User memberUser;
    private User operatorUser;

    @BeforeEach
    void setUp() {
        setUpBase();

        // ADMIN 사용자 생성
        adminUser = createAndSaveUser("20200001", "admin@inha.edu", UserRole.ADMIN);

        // ASSOCIATE 사용자 생성
        associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);

        // MEMBER 사용자 생성
        memberUser = createAndSaveUser("20220001", "member@inha.edu", UserRole.MEMBER);

        // OPERATOR 사용자 생성
        operatorUser = createAndSaveUser("20210001", "operator@inha.edu", UserRole.OPERATOR);
    }

    @Nested
    @DisplayName("준회원 목록 조회")
    class AssociateListTest {

        @Test
        @DisplayName("관리자 준회원 목록 조회 성공 [APR-001]")
        void getPendingAssociates_WithAdminRole_ReturnsAssociateList() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<AssociateInfoResponse> result = memberApprovalService.getPendingAssociates(pageable, adminUser.getId());

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            AssociateInfoResponse response = result.getContent().get(0);
            assertThat(response.userId()).isEqualTo(associateUser.getId());
            assertThat(response.studentId()).isEqualTo(associateUser.getStudentId());
            assertThat(response.name()).isEqualTo(associateUser.getName());
            assertThat(response.department()).isEqualTo(associateUser.getDepartment());
            assertThat(response.motivation()).isEqualTo(associateUser.getMotivation());
        }

        @Test
        @DisplayName("준회원 상세 정보 표시 - 학번, 본명, 학과, 가입 동기, 가입일 포함 [APR-002]")
        void getPendingAssociates_ResponseContainsDetailedInfo() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<AssociateInfoResponse> result = memberApprovalService.getPendingAssociates(pageable, adminUser.getId());

            // then
            AssociateInfoResponse response = result.getContent().get(0);
            assertThat(response.studentId()).isNotNull();
            assertThat(response.name()).isNotNull();
            assertThat(response.department()).isNotNull();
            assertThat(response.motivation()).isNotNull();
        }

        @Test
        @DisplayName("준회원이 없는 경우 빈 목록 반환 [APR-003]")
        void getPendingAssociates_NoAssociates_ReturnsEmptyList() {
            // given - 기존 ASSOCIATE 삭제
            transactionTemplate.execute(status -> {
                userRepository.delete(associateUser);
                return null;
            });
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<AssociateInfoResponse> result = memberApprovalService.getPendingAssociates(pageable, adminUser.getId());

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("목록 페이지네이션 적용 확인 [APR-004]")
        void getPendingAssociates_WithPagination_ReturnsPagedResult() {
            // given - 추가 ASSOCIATE 사용자 생성
            User associate2 = createAndSaveUser("20230002", "associate2@inha.edu", UserRole.ASSOCIATE);
            User associate3 = createAndSaveUser("20230003", "associate3@inha.edu", UserRole.ASSOCIATE);
            User associate4 = createAndSaveUser("20230004", "associate4@inha.edu", UserRole.ASSOCIATE);
            User associate5 = createAndSaveUser("20230005", "associate5@inha.edu", UserRole.ASSOCIATE);

            Pageable pageable = PageRequest.of(0, 2);

            // when
            Page<AssociateInfoResponse> result = memberApprovalService.getPendingAssociates(pageable, adminUser.getId());

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.getNumber()).isZero();
        }
    }

    @Nested
    @DisplayName("개별 승인")
    class IndividualApprovalTest {

        @Test
        @DisplayName("관리자 개별 승인 성공 - 역할이 MEMBER로 변경됨 [APR-010]")
        void approveAssociate_WithAdminRole_ChangesRoleToMember() {
            // given
            PasswordCredential credential = PasswordCredential.create(associateUser, "hashedPassword");
            transactionTemplate.execute(status -> {
                passwordCredentialRepository.save(credential);
                return null;
            });

            // when
            memberApprovalService.approveAssociate(associateUser.getId(), adminUser.getId());

            // then - 상태 검증
            User updatedUser = userRepository.findById(associateUser.getId()).orElseThrow();
            assertThat(updatedUser.getRole()).isEqualTo(UserRole.MEMBER);
            assertThat(updatedUser.isMember()).isTrue();
        }

        @Test
        @DisplayName("승인 후 역할 변경 확인 - ASSOCIATE에서 MEMBER로 [APR-011]")
        void approveAssociate_RoleChangedFromAssociateToMember() {
            // given
            assertThat(associateUser.getRole()).isEqualTo(UserRole.ASSOCIATE);
            PasswordCredential credential = PasswordCredential.create(associateUser, "hashedPassword");
            transactionTemplate.execute(status -> {
                passwordCredentialRepository.save(credential);
                return null;
            });

            // when
            memberApprovalService.approveAssociate(associateUser.getId(), adminUser.getId());

            // then
            User updatedUser = userRepository.findById(associateUser.getId()).orElseThrow();
            assertThat(updatedUser.getRole()).isEqualTo(UserRole.MEMBER);
        }

        @Test
        @DisplayName("승인일 정확히 기록 - PasswordCredential에 approvedAt, approvedBy 설정 [APR-012]")
        void approveAssociate_SetsApprovalInfo() {
            // given
            PasswordCredential credential = PasswordCredential.create(associateUser, "hashedPassword");
            assertThat(credential.getApprovedAt()).isNull();
            assertThat(credential.getApprovedBy()).isNull();
            transactionTemplate.execute(status -> {
                passwordCredentialRepository.save(credential);
                return null;
            });

            // when
            memberApprovalService.approveAssociate(associateUser.getId(), adminUser.getId());

            // then
            PasswordCredential updatedCredential = passwordCredentialRepository.findByUserId(associateUser.getId()).orElseThrow();
            assertThat(updatedCredential.getApprovedAt()).isNotNull();
            assertThat(updatedCredential.getApprovedBy()).isEqualTo(adminUser.getId());
            assertThat(updatedCredential.isApproved()).isTrue();
        }

        @Test
        @DisplayName("역할 변경 감사 이력 기록 - UserRoleHistory에 ASSOCIATE -> MEMBER 기록 [APR-013]")
        void approveAssociate_RecordsRoleChangeHistory() {
            // given
            PasswordCredential credential = PasswordCredential.create(associateUser, "hashedPassword");
            transactionTemplate.execute(status -> {
                passwordCredentialRepository.save(credential);
                return null;
            });

            // when
            memberApprovalService.approveAssociate(associateUser.getId(), adminUser.getId());

            // then - 역할 변경 이력 검증
            List<UserRoleHistory> histories = userRoleHistoryRepository.findAll();
            assertThat(histories).hasSize(1);

            UserRoleHistory history = histories.get(0);
            assertThat(history.getUser().getId()).isEqualTo(associateUser.getId());
            assertThat(history.getPreviousRole()).isEqualTo(UserRole.ASSOCIATE);
            assertThat(history.getNewRole()).isEqualTo(UserRole.MEMBER);
            assertThat(history.getReason()).contains("정회원 전환");
        }
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
            int approvedCount = memberApprovalService.approveBulk(userIds, adminUser.getId());

            // then
            assertThat(approvedCount).isEqualTo(5);

            // 상태 검증
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
            int approvedCount = memberApprovalService.approveBulk(selectedUserIds, adminUser.getId());

            // then
            assertThat(approvedCount).isEqualTo(3);

            // 상태 검증 - 선택된 사용자
            assertThat(userRepository.findById(associate1.getId()).orElseThrow().getRole()).isEqualTo(UserRole.MEMBER);
            assertThat(userRepository.findById(associate2.getId()).orElseThrow().getRole()).isEqualTo(UserRole.MEMBER);
            assertThat(userRepository.findById(associate3.getId()).orElseThrow().getRole()).isEqualTo(UserRole.MEMBER);

            // 상태 검증 - 선택되지 않은 사용자
            assertThat(userRepository.findById(associate4.getId()).orElseThrow().getRole()).isEqualTo(UserRole.ASSOCIATE);
            assertThat(userRepository.findById(associate5.getId()).orElseThrow().getRole()).isEqualTo(UserRole.ASSOCIATE);
        }

        @Test
        @DisplayName("일괄 승인 시 각각 승인일 기록 [APR-022]")
        void approveBulk_EachUserHasApprovalDate() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);
            User associate2 = createAndSaveUser("20230011", "a11@inha.edu", UserRole.ASSOCIATE);
            PasswordCredential credential1 = PasswordCredential.create(associate1, "hash1");
            PasswordCredential credential2 = PasswordCredential.create(associate2, "hash2");
            transactionTemplate.execute(status -> {
                passwordCredentialRepository.save(credential1);
                passwordCredentialRepository.save(credential2);
                return null;
            });

            List<Long> userIds = List.of(associate1.getId(), associate2.getId());

            // when
            memberApprovalService.approveBulk(userIds, adminUser.getId());

            // then
            PasswordCredential updatedCredential1 = passwordCredentialRepository.findByUserId(associate1.getId()).orElseThrow();
            PasswordCredential updatedCredential2 = passwordCredentialRepository.findByUserId(associate2.getId()).orElseThrow();

            assertThat(updatedCredential1.getApprovedAt()).isNotNull();
            assertThat(updatedCredential1.getApprovedBy()).isEqualTo(adminUser.getId());
            assertThat(updatedCredential2.getApprovedAt()).isNotNull();
            assertThat(updatedCredential2.getApprovedBy()).isEqualTo(adminUser.getId());
        }

        @Test
        @DisplayName("일괄 승인 시 역할 변경 이력 개별 기록 [APR-023]")
        void approveBulk_EachUserHasRoleHistory() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);
            User associate2 = createAndSaveUser("20230011", "a11@inha.edu", UserRole.ASSOCIATE);

            List<Long> userIds = List.of(associate1.getId(), associate2.getId());

            // when
            memberApprovalService.approveBulk(userIds, adminUser.getId());

            // then
            List<UserRoleHistory> histories = userRoleHistoryRepository.findAll();
            assertThat(histories).hasSize(2);
        }

        @Test
        @DisplayName("선택 없이 일괄 승인 시도 시 예외 발생 [APR-024]")
        void approveBulk_EmptyList_ThrowsException() {
            // given
            List<Long> emptyUserIds = Collections.emptyList();

            // when & then
            assertThatThrownBy(() -> memberApprovalService.approveBulk(emptyUserIds, adminUser.getId()))
                    .isInstanceOf(BulkApprovalEmptyException.class);
        }

        @Test
        @DisplayName("null 목록으로 일괄 승인 시도 시 예외 발생 [APR-024-2]")
        void approveBulk_NullList_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> memberApprovalService.approveBulk(null, adminUser.getId()))
                    .isInstanceOf(BulkApprovalEmptyException.class);
        }
    }

    @Nested
    @DisplayName("권한 검증")
    class AuthorizationTest {

        @Test
        @DisplayName("운영진 승인 시도 시 거부 - AdminRequiredException 발생 [APR-030]")
        void approveAssociate_WithOperatorRole_ThrowsAdminRequiredException() {
            // when & then
            assertThatThrownBy(() -> memberApprovalService.approveAssociate(associateUser.getId(), operatorUser.getId()))
                    .isInstanceOf(AdminRequiredException.class);
        }

        @Test
        @DisplayName("정회원 승인 시도 시 거부 - AdminRequiredException 발생 [APR-031]")
        void approveAssociate_WithMemberRole_ThrowsAdminRequiredException() {
            // when & then
            assertThatThrownBy(() -> memberApprovalService.approveAssociate(associateUser.getId(), memberUser.getId()))
                    .isInstanceOf(AdminRequiredException.class);
        }

        @Test
        @DisplayName("준회원 승인 시도 시 거부 - AdminRequiredException 발생 [APR-032]")
        void approveAssociate_WithAssociateRole_ThrowsAdminRequiredException() {
            // given
            User anotherAssociate = createAndSaveUser("20230100", "another@inha.edu", UserRole.ASSOCIATE);

            // when & then
            assertThatThrownBy(() -> memberApprovalService.approveAssociate(anotherAssociate.getId(), associateUser.getId()))
                    .isInstanceOf(AdminRequiredException.class);
        }

        @Test
        @DisplayName("비로그인 상태 승인 시도 시 거부 - UserNotFoundException 발생 [APR-033]")
        void approveAssociate_WithNonExistentUser_ThrowsUserNotFoundException() {
            // given
            Long nonExistentUserId = 999L;

            // when & then
            assertThatThrownBy(() -> memberApprovalService.approveAssociate(associateUser.getId(), nonExistentUserId))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("운영진 목록 조회 시도 시 거부 - AdminRequiredException 발생 [APR-034]")
        void getPendingAssociates_WithOperatorRole_ThrowsAdminRequiredException() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when & then
            assertThatThrownBy(() -> memberApprovalService.getPendingAssociates(pageable, operatorUser.getId()))
                    .isInstanceOf(AdminRequiredException.class);
        }
    }

    @Nested
    @DisplayName("ADMIN 권한 보호")
    class AdminProtectionTest {

        @Test
        @DisplayName("마지막 ADMIN 권한 변경 시도 시 거부 - LastAdminCannotChangeException 발생 [APR-040]")
        void validateNotLastAdmin_LastAdmin_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> memberApprovalService.validateNotLastAdmin(adminUser.getId()))
                    .isInstanceOf(LastAdminCannotChangeException.class);
        }

        @Test
        @DisplayName("여러 ADMIN 존재 시 권한 변경 가능 [APR-041]")
        void validateNotLastAdmin_MultipleAdmins_NoException() {
            // given - 추가 ADMIN 생성
            createAndSaveUser("20200002", "admin2@inha.edu", UserRole.ADMIN);

            // when & then (예외 발생하지 않음)
            memberApprovalService.validateNotLastAdmin(adminUser.getId());
        }

        @Test
        @DisplayName("ADMIN 권한 변경 - 마지막 ADMIN이 아닌 경우 정상 변경 [APR-041-2]")
        void changeAdminRole_NotLastAdmin_Success() {
            // given
            User admin2 = createAndSaveUser("20200002", "admin2@inha.edu", UserRole.ADMIN);

            // when
            memberApprovalService.changeAdminRole(admin2.getId(), UserRole.MEMBER, adminUser.getId());

            // then
            User updatedAdmin2 = userRepository.findById(admin2.getId()).orElseThrow();
            assertThat(updatedAdmin2.getRole()).isEqualTo(UserRole.MEMBER);

            List<UserRoleHistory> histories = userRoleHistoryRepository.findAll();
            assertThat(histories).isNotEmpty();
        }

        @Test
        @DisplayName("ADMIN 권한 변경 - 마지막 ADMIN인 경우 예외 발생 [APR-040-2]")
        void changeAdminRole_LastAdmin_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> memberApprovalService.changeAdminRole(adminUser.getId(), UserRole.MEMBER, adminUser.getId()))
                    .isInstanceOf(LastAdminCannotChangeException.class);
        }
    }

    @Nested
    @DisplayName("추가 엣지 케이스")
    class EdgeCaseTest {

        @Test
        @DisplayName("이미 MEMBER인 사용자 승인 시도 시 UserNotAssociateException 발생")
        void approveAssociate_AlreadyMember_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> memberApprovalService.approveAssociate(memberUser.getId(), adminUser.getId()))
                    .isInstanceOf(UserNotAssociateException.class);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 승인 시도 시 UserNotFoundException 발생")
        void approveAssociate_NonExistentUser_ThrowsException() {
            // given
            Long nonExistentUserId = 999L;

            // when & then
            assertThatThrownBy(() -> memberApprovalService.approveAssociate(nonExistentUserId, adminUser.getId()))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("일괄 승인 시 일부 사용자가 존재하지 않는 경우 나머지는 정상 처리")
        void approveBulk_SomeUsersNotFound_ProcessesOthers() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);
            Long nonExistentUserId = 999L;

            List<Long> userIds = List.of(associate1.getId(), nonExistentUserId);

            // when
            int approvedCount = memberApprovalService.approveBulk(userIds, adminUser.getId());

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
            int approvedCount = memberApprovalService.approveBulk(userIds, adminUser.getId());

            // then
            assertThat(approvedCount).isEqualTo(1);
            assertThat(userRepository.findById(associate1.getId()).orElseThrow().getRole()).isEqualTo(UserRole.MEMBER);
            assertThat(userRepository.findById(member1.getId()).orElseThrow().getRole()).isEqualTo(UserRole.MEMBER);
        }
    }
}
