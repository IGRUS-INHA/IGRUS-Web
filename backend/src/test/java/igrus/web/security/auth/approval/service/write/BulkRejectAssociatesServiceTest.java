package igrus.web.security.auth.approval.service.write;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.security.auth.approval.domain.AssociateDecisionType;
import igrus.web.security.auth.approval.exception.BulkRejectionEmptyException;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BulkRejectAssociatesService 통합 테스트")
class BulkRejectAssociatesServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private BulkRejectAssociatesService bulkRejectAssociatesService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        setUpBase();

        adminUser = createAndSaveUser("20200001", "admin@inha.edu", UserRole.ADMIN);
    }

    @Nested
    @DisplayName("일괄 거절")
    class BulkRejectionTest {

        @Test
        @DisplayName("다수 준회원 일괄 거절 성공 [REJ-020]")
        void rejectBulk_AllAssociates_AllRejected() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);
            User associate2 = createAndSaveUser("20230011", "a11@inha.edu", UserRole.ASSOCIATE);
            User associate3 = createAndSaveUser("20230012", "a12@inha.edu", UserRole.ASSOCIATE);

            List<Long> userIds = List.of(associate1.getId(), associate2.getId(), associate3.getId());

            // when
            int rejectedCount = bulkRejectAssociatesService.rejectBulk(userIds, adminUser.getId(), "일괄 거절 사유");

            // then
            assertThat(rejectedCount).isEqualTo(3);

            AssociateDecision d1 = associateDecisionRepository.findByUserIdAndActiveTrue(associate1.getId()).orElseThrow();
            AssociateDecision d2 = associateDecisionRepository.findByUserIdAndActiveTrue(associate2.getId()).orElseThrow();
            AssociateDecision d3 = associateDecisionRepository.findByUserIdAndActiveTrue(associate3.getId()).orElseThrow();

            assertThat(d1.getType()).isEqualTo(AssociateDecisionType.REJECTED);
            assertThat(d2.getType()).isEqualTo(AssociateDecisionType.REJECTED);
            assertThat(d3.getType()).isEqualTo(AssociateDecisionType.REJECTED);
            assertThat(d1.getReason()).isEqualTo("일괄 거절 사유");
        }

        @Test
        @DisplayName("거절 후 역할은 ASSOCIATE 유지 [REJ-021]")
        void rejectBulk_RolesRemainAssociate() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);
            User associate2 = createAndSaveUser("20230011", "a11@inha.edu", UserRole.ASSOCIATE);

            List<Long> userIds = List.of(associate1.getId(), associate2.getId());

            // when
            bulkRejectAssociatesService.rejectBulk(userIds, adminUser.getId(), "거절 사유");

            // then
            assertThat(userRepository.findById(associate1.getId()).orElseThrow().getRole()).isEqualTo(UserRole.ASSOCIATE);
            assertThat(userRepository.findById(associate2.getId()).orElseThrow().getRole()).isEqualTo(UserRole.ASSOCIATE);
        }

        @Test
        @DisplayName("빈 목록으로 일괄 거절 시도 시 예외 발생 [REJ-024]")
        void rejectBulk_EmptyList_ThrowsException() {
            assertThatThrownBy(() -> bulkRejectAssociatesService.rejectBulk(Collections.emptyList(), adminUser.getId(), "사유"))
                    .isInstanceOf(BulkRejectionEmptyException.class);
        }

        @Test
        @DisplayName("null 목록으로 일괄 거절 시도 시 예외 발생 [REJ-024-2]")
        void rejectBulk_NullList_ThrowsException() {
            assertThatThrownBy(() -> bulkRejectAssociatesService.rejectBulk(null, adminUser.getId(), "사유"))
                    .isInstanceOf(BulkRejectionEmptyException.class);
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCaseTest {

        @Test
        @DisplayName("일부 사용자가 존재하지 않는 경우 나머지는 정상 처리")
        void rejectBulk_SomeUsersNotFound_ProcessesOthers() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);

            List<Long> userIds = List.of(associate1.getId(), Long.MAX_VALUE);

            // when
            int rejectedCount = bulkRejectAssociatesService.rejectBulk(userIds, adminUser.getId(), "거절 사유");

            // then
            assertThat(rejectedCount).isEqualTo(1);
            assertThat(associateDecisionRepository.findByUserIdAndActiveTrue(associate1.getId())).isPresent();
        }

        @Test
        @DisplayName("이메일 미인증 사용자는 skip되고 나머지는 정상 처리")
        void rejectBulk_SomeUsersUnverified_ProcessesOthers() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);
            User unverifiedUser = createAndSaveUnverifiedUser("20230011", "unverified@inha.edu", UserRole.ASSOCIATE);

            List<Long> userIds = List.of(associate1.getId(), unverifiedUser.getId());

            // when
            int rejectedCount = bulkRejectAssociatesService.rejectBulk(userIds, adminUser.getId(), "거절 사유");

            // then
            assertThat(rejectedCount).isEqualTo(1);
            assertThat(associateDecisionRepository.findByUserIdAndActiveTrue(associate1.getId())).isPresent();
            assertThat(associateDecisionRepository.findByUserIdAndActiveTrue(unverifiedUser.getId())).isEmpty();
        }

        @Test
        @DisplayName("일부 사용자가 ASSOCIATE가 아닌 경우 나머지는 정상 처리")
        void rejectBulk_SomeUsersNotAssociate_ProcessesOthers() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);
            User member1 = createAndSaveUser("20230011", "m11@inha.edu", UserRole.MEMBER);

            List<Long> userIds = List.of(associate1.getId(), member1.getId());

            // when
            int rejectedCount = bulkRejectAssociatesService.rejectBulk(userIds, adminUser.getId(), "거절 사유");

            // then
            assertThat(rejectedCount).isEqualTo(1);
            assertThat(associateDecisionRepository.findByUserIdAndActiveTrue(associate1.getId())).isPresent();
        }

        @Test
        @DisplayName("이미 거절된 사용자가 포함된 경우 해당 유저는 skip되고 나머지는 정상 처리")
        void rejectBulk_SomeUsersAlreadyRejected_SkipsRejected() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);
            User associate2 = createAndSaveUser("20230011", "a11@inha.edu", UserRole.ASSOCIATE);

            AssociateDecision existingDecision = AssociateDecision.reject(associate1, adminUser.getId(), "이전 거절");
            associateDecisionRepository.save(existingDecision);

            List<Long> userIds = List.of(associate1.getId(), associate2.getId());

            // when
            int rejectedCount = bulkRejectAssociatesService.rejectBulk(userIds, adminUser.getId(), "새 거절 사유");

            // then
            assertThat(rejectedCount).isEqualTo(1);
            assertThat(associateDecisionRepository.findByUserIdAndActiveTrue(associate2.getId())).isPresent();
        }
    }
}
