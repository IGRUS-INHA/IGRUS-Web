package igrus.web.security.auth.approval.service.write;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.security.auth.approval.domain.AssociateDecisionType;
import igrus.web.security.auth.approval.exception.AdminRequiredException;
import igrus.web.security.auth.approval.exception.AssociateAlreadyDecidedException;
import igrus.web.security.auth.approval.exception.UserNotAssociateException;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RejectAssociateService 통합 테스트")
class RejectAssociateServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private RejectAssociateService rejectAssociateService;

    private User adminUser;
    private User associateUser;
    private User memberUser;

    @BeforeEach
    void setUp() {
        setUpBase();

        adminUser = createAndSaveUser("20200001", "admin@inha.edu", UserRole.ADMIN);
        associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);
        memberUser = createAndSaveUser("20220001", "member@inha.edu", UserRole.MEMBER);
    }

    @Nested
    @DisplayName("개별 거절")
    class IndividualRejectionTest {

        @Test
        @DisplayName("관리자 개별 거절 성공 - AssociateDecision에 REJECTED 기록 [REJ-010]")
        void rejectAssociate_WithAdminRole_CreatesRejectedDecision() {
            // when
            rejectAssociateService.rejectAssociate(associateUser.getId(), adminUser.getId(), "가입 동기 불충분");

            // then
            Optional<AssociateDecision> decision = associateDecisionRepository.findByUserIdAndActiveTrue(associateUser.getId());
            assertThat(decision).isPresent();
            assertThat(decision.get().getType()).isEqualTo(AssociateDecisionType.REJECTED);
            assertThat(decision.get().getReason()).isEqualTo("가입 동기 불충분");
            assertThat(decision.get().getDecidedBy()).isEqualTo(adminUser.getId());
            assertThat(decision.get().getDecidedAt()).isNotNull();
        }

        @Test
        @DisplayName("거절 후 역할은 ASSOCIATE 유지 [REJ-011]")
        void rejectAssociate_RoleRemainsAssociate() {
            // when
            rejectAssociateService.rejectAssociate(associateUser.getId(), adminUser.getId(), "거절 사유");

            // then
            User updatedUser = userRepository.findById(associateUser.getId()).orElseThrow();
            assertThat(updatedUser.getRole()).isEqualTo(UserRole.ASSOCIATE);
        }
    }

    @Nested
    @DisplayName("권한 검증")
    class AuthorizationTest {

        @Test
        @DisplayName("비관리자 거절 시도 시 AdminRequiredException 발생 [REJ-030]")
        void rejectAssociate_WithNonAdminRole_ThrowsAdminRequiredException() {
            assertThatThrownBy(() -> rejectAssociateService.rejectAssociate(associateUser.getId(), memberUser.getId(), "사유"))
                    .isInstanceOf(AdminRequiredException.class);
        }

        @Test
        @DisplayName("존재하지 않는 처리자 ID로 거절 시도 시 UserNotFoundException 발생 [REJ-031]")
        void rejectAssociate_WithNonExistentApprover_ThrowsUserNotFoundException() {
            assertThatThrownBy(() -> rejectAssociateService.rejectAssociate(associateUser.getId(), 999L, "사유"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCaseTest {

        @Test
        @DisplayName("존재하지 않는 사용자 거절 시 UserNotFoundException 발생")
        void rejectAssociate_NonExistentUser_ThrowsException() {
            assertThatThrownBy(() -> rejectAssociateService.rejectAssociate(999L, adminUser.getId(), "사유"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("MEMBER 거절 시도 시 UserNotAssociateException 발생")
        void rejectAssociate_MemberUser_ThrowsException() {
            assertThatThrownBy(() -> rejectAssociateService.rejectAssociate(memberUser.getId(), adminUser.getId(), "사유"))
                    .isInstanceOf(UserNotAssociateException.class);
        }

        @Test
        @DisplayName("강등된 준회원 거절 성공 - DEMOTED 결정이 비활성화되고 REJECTED 결정 생성")
        void rejectAssociate_DemotedUser_DeactivatesDemotedAndCreatesRejected() {
            // given
            AssociateDecision demotedDecision = AssociateDecision.demote(associateUser, adminUser.getId(), "관리자에 의한 역할 강등");
            associateDecisionRepository.save(demotedDecision);

            // when
            rejectAssociateService.rejectAssociate(associateUser.getId(), adminUser.getId(), "거절 사유");

            // then
            Optional<AssociateDecision> activeDecision = associateDecisionRepository.findByUserIdAndActiveTrue(associateUser.getId());
            assertThat(activeDecision).isPresent();
            assertThat(activeDecision.get().getType()).isEqualTo(AssociateDecisionType.REJECTED);
            assertThat(activeDecision.get().getReason()).isEqualTo("거절 사유");
        }

        @Test
        @DisplayName("이미 거절된 준회원 재거절 시도 시 AssociateAlreadyDecidedException 발생")
        void rejectAssociate_AlreadyRejected_ThrowsException() {
            // given
            rejectAssociateService.rejectAssociate(associateUser.getId(), adminUser.getId(), "첫 번째 거절");

            // when & then
            assertThatThrownBy(() -> rejectAssociateService.rejectAssociate(associateUser.getId(), adminUser.getId(), "두 번째 거절"))
                    .isInstanceOf(AssociateAlreadyDecidedException.class);
        }
    }
}
