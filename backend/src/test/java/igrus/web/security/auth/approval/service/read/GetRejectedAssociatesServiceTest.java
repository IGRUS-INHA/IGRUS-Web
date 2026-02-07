package igrus.web.security.auth.approval.service.read;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.security.auth.approval.dto.response.RejectedAssociateInfoResponse;
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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GetRejectedAssociatesService 통합 테스트")
class GetRejectedAssociatesServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetRejectedAssociatesService getRejectedAssociatesService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        setUpBase();

        adminUser = createAndSaveUser("20200001", "admin@inha.edu", UserRole.ADMIN);
    }

    @Nested
    @DisplayName("거절된 준회원 목록 조회")
    class GetRejectedListTest {

        @Test
        @DisplayName("거절된 준회원 목록 조회 성공 [REJ-040]")
        void getRejectedAssociates_ReturnsRejectedList() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);
            User associate2 = createAndSaveUser("20230011", "a11@inha.edu", UserRole.ASSOCIATE);

            associateDecisionRepository.save(AssociateDecision.reject(associate1, adminUser.getId(), "사유1"));
            associateDecisionRepository.save(AssociateDecision.reject(associate2, adminUser.getId(), "사유2"));

            // when
            Page<RejectedAssociateInfoResponse> result = getRejectedAssociatesService.getRejectedAssociates(
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "decidedAt")),
                    adminUser.getId()
            );

            // then
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).extracting(RejectedAssociateInfoResponse::rejectionReason)
                    .containsExactlyInAnyOrder("사유1", "사유2");
        }

        @Test
        @DisplayName("거절된 준회원이 없는 경우 빈 목록 반환 [REJ-041]")
        void getRejectedAssociates_NoRejected_ReturnsEmpty() {
            // when
            Page<RejectedAssociateInfoResponse> result = getRejectedAssociatesService.getRejectedAssociates(
                    PageRequest.of(0, 20),
                    adminUser.getId()
            );

            // then
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("승인된 사용자는 거절 목록에 포함되지 않음 [REJ-042]")
        void getRejectedAssociates_ExcludesApproved() {
            // given
            User associate1 = createAndSaveUser("20230010", "a10@inha.edu", UserRole.ASSOCIATE);
            User associate2 = createAndSaveUser("20230011", "a11@inha.edu", UserRole.ASSOCIATE);

            associateDecisionRepository.save(AssociateDecision.reject(associate1, adminUser.getId(), "거절 사유"));
            associateDecisionRepository.save(AssociateDecision.approve(associate2, adminUser.getId()));

            // when
            Page<RejectedAssociateInfoResponse> result = getRejectedAssociatesService.getRejectedAssociates(
                    PageRequest.of(0, 20),
                    adminUser.getId()
            );

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).userId()).isEqualTo(associate1.getId());
        }
    }
}
