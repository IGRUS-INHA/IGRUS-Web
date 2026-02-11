package igrus.web.security.auth.approval.service.write;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.security.auth.approval.domain.AssociateDecisionType;
import igrus.web.security.auth.approval.exception.AdminRequiredException;
import igrus.web.security.auth.approval.exception.UserNotAssociateException;
import igrus.web.security.auth.common.exception.email.EmailNotVerifiedException;
import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserRoleHistory;
import igrus.web.user.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ApproveAssociateService 통합 테스트")
class ApproveAssociateServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private ApproveAssociateService approveAssociateService;

    private User adminUser;
    private User associateUser;
    private User memberUser;
    private User operatorUser;

    @BeforeEach
    void setUp() {
        setUpBase();

        adminUser = createAndSaveUser("20200001", "admin@inha.edu", UserRole.ADMIN);
        associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);
        memberUser = createAndSaveUser("20220001", "member@inha.edu", UserRole.MEMBER);
        operatorUser = createAndSaveUser("20210001", "operator@inha.edu", UserRole.OPERATOR);
    }

    @Nested
    @DisplayName("개별 승인")
    class IndividualApprovalTest {

        @Test
        @DisplayName("관리자 개별 승인 성공 - 역할이 MEMBER로 변경됨 [APR-010]")
        void approveAssociate_WithAdminRole_ChangesRoleToMember() {
            // when
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());

            // then
            User updatedUser = userRepository.findById(associateUser.getId()).orElseThrow();
            assertThat(updatedUser.getRole()).isEqualTo(UserRole.MEMBER);
            assertThat(updatedUser.isMember()).isTrue();
        }

        @Test
        @DisplayName("승인 후 역할 변경 확인 - ASSOCIATE에서 MEMBER로 [APR-011]")
        void approveAssociate_RoleChangedFromAssociateToMember() {
            // given
            assertThat(associateUser.getRole()).isEqualTo(UserRole.ASSOCIATE);

            // when
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());

            // then
            User updatedUser = userRepository.findById(associateUser.getId()).orElseThrow();
            assertThat(updatedUser.getRole()).isEqualTo(UserRole.MEMBER);
        }

        @Test
        @DisplayName("승인 결정 기록 - AssociateDecision에 APPROVED 타입으로 저장 [APR-012]")
        void approveAssociate_CreatesAssociateDecision() {
            // when
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());

            // then
            Optional<AssociateDecision> decision = associateDecisionRepository.findByUserIdAndActiveTrue(associateUser.getId());
            assertThat(decision).isPresent();
            assertThat(decision.get().getType()).isEqualTo(AssociateDecisionType.APPROVED);
            assertThat(decision.get().getDecidedBy()).isEqualTo(adminUser.getId());
            assertThat(decision.get().getDecidedAt()).isNotNull();
        }

        @Test
        @DisplayName("승인 시 리프레시 토큰 만료 - 기존 토큰이 revoked 처리됨 [APR-014]")
        void approveAssociate_RevokesRefreshTokens() {
            // given
            RefreshToken token = RefreshToken.createInitial(associateUser, "test-refresh-token", 86400000);
            refreshTokenRepository.save(token);

            // when
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());

            // then
            assertThat(refreshTokenRepository.findByUserIdAndRevokedFalse(associateUser.getId())).isEmpty();
        }

        @Test
        @DisplayName("역할 변경 감사 이력 기록 - UserRoleHistory에 ASSOCIATE -> MEMBER 기록 [APR-013]")
        void approveAssociate_RecordsRoleChangeHistory() {
            // when
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());

            // then
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
    @DisplayName("권한 검증")
    class AuthorizationTest {

        @Test
        @DisplayName("운영진 승인 시도 시 거부 - AdminRequiredException 발생 [APR-030]")
        void approveAssociate_WithOperatorRole_ThrowsAdminRequiredException() {
            // when & then
            assertThatThrownBy(() -> approveAssociateService.approveAssociate(associateUser.getId(), operatorUser.getId()))
                    .isInstanceOf(AdminRequiredException.class);
        }

        @Test
        @DisplayName("정회원 승인 시도 시 거부 - AdminRequiredException 발생 [APR-031]")
        void approveAssociate_WithMemberRole_ThrowsAdminRequiredException() {
            // when & then
            assertThatThrownBy(() -> approveAssociateService.approveAssociate(associateUser.getId(), memberUser.getId()))
                    .isInstanceOf(AdminRequiredException.class);
        }

        @Test
        @DisplayName("준회원 승인 시도 시 거부 - AdminRequiredException 발생 [APR-032]")
        void approveAssociate_WithAssociateRole_ThrowsAdminRequiredException() {
            // given
            User anotherAssociate = createAndSaveUser("20230100", "another@inha.edu", UserRole.ASSOCIATE);

            // when & then
            assertThatThrownBy(() -> approveAssociateService.approveAssociate(anotherAssociate.getId(), associateUser.getId()))
                    .isInstanceOf(AdminRequiredException.class);
        }

        @Test
        @DisplayName("비로그인 상태 승인 시도 시 거부 - UserNotFoundException 발생 [APR-033]")
        void approveAssociate_WithNonExistentUser_ThrowsUserNotFoundException() {
            // given
            Long nonExistentUserId = 999L;

            // when & then
            assertThatThrownBy(() -> approveAssociateService.approveAssociate(associateUser.getId(), nonExistentUserId))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCaseTest {

        @Test
        @DisplayName("강등된 준회원 재승인 성공 - DEMOTED 결정이 비활성화되고 APPROVED 결정 생성")
        void approveAssociate_DemotedUser_DeactivatesDemotedAndCreatesApproved() {
            // given
            AssociateDecision demotedDecision = AssociateDecision.demote(associateUser, adminUser.getId(), "관리자에 의한 역할 강등");
            associateDecisionRepository.save(demotedDecision);

            // when
            approveAssociateService.approveAssociate(associateUser.getId(), adminUser.getId());

            // then
            Optional<AssociateDecision> activeDecision = associateDecisionRepository.findByUserIdAndActiveTrue(associateUser.getId());
            assertThat(activeDecision).isPresent();
            assertThat(activeDecision.get().getType()).isEqualTo(AssociateDecisionType.APPROVED);

            User updatedUser = userRepository.findById(associateUser.getId()).orElseThrow();
            assertThat(updatedUser.getRole()).isEqualTo(UserRole.MEMBER);
        }

        @Test
        @DisplayName("이메일 미인증 사용자 승인 시도 시 EmailNotVerifiedException 발생")
        void approveAssociate_UnverifiedEmail_ThrowsException() {
            // given
            User unverifiedUser = createAndSaveUnverifiedUser("20230099", "unverified@inha.edu", UserRole.ASSOCIATE);

            // when & then
            assertThatThrownBy(() -> approveAssociateService.approveAssociate(unverifiedUser.getId(), adminUser.getId()))
                    .isInstanceOf(EmailNotVerifiedException.class);
        }

        @Test
        @DisplayName("이미 MEMBER인 사용자 승인 시도 시 UserNotAssociateException 발생")
        void approveAssociate_AlreadyMember_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> approveAssociateService.approveAssociate(memberUser.getId(), adminUser.getId()))
                    .isInstanceOf(UserNotAssociateException.class);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 승인 시도 시 UserNotFoundException 발생")
        void approveAssociate_NonExistentUser_ThrowsException() {
            // given
            Long nonExistentUserId = 999L;

            // when & then
            assertThatThrownBy(() -> approveAssociateService.approveAssociate(nonExistentUserId, adminUser.getId()))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}
