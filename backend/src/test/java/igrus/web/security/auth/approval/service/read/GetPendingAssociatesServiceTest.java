package igrus.web.security.auth.approval.service.read;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.approval.dto.response.AssociateInfoResponse;
import igrus.web.security.auth.approval.exception.AdminRequiredException;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GetPendingAssociatesService 통합 테스트")
class GetPendingAssociatesServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetPendingAssociatesService getPendingAssociatesService;

    private User adminUser;
    private User associateUser;
    private User operatorUser;

    @BeforeEach
    void setUp() {
        setUpBase();

        adminUser = createAndSaveUser("20200001", "admin@inha.edu", UserRole.ADMIN);
        associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);
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
            Page<AssociateInfoResponse> result = getPendingAssociatesService.getPendingAssociates(pageable, adminUser.getId());

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
            Page<AssociateInfoResponse> result = getPendingAssociatesService.getPendingAssociates(pageable, adminUser.getId());

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
            Page<AssociateInfoResponse> result = getPendingAssociatesService.getPendingAssociates(pageable, adminUser.getId());

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("이메일 미인증 사용자는 목록에서 제외 [APR-005]")
        void getPendingAssociates_ExcludesUnverifiedUsers() {
            // given
            createAndSaveUnverifiedUser("20230099", "unverified@inha.edu", UserRole.ASSOCIATE);
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<AssociateInfoResponse> result = getPendingAssociatesService.getPendingAssociates(pageable, adminUser.getId());

            // then - 인증된 associateUser만 포함, 미인증 사용자 제외
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).userId()).isEqualTo(associateUser.getId());
        }

        @Test
        @DisplayName("목록 페이지네이션 적용 확인 [APR-004]")
        void getPendingAssociates_WithPagination_ReturnsPagedResult() {
            // given - 추가 ASSOCIATE 사용자 생성
            createAndSaveUser("20230002", "associate2@inha.edu", UserRole.ASSOCIATE);
            createAndSaveUser("20230003", "associate3@inha.edu", UserRole.ASSOCIATE);
            createAndSaveUser("20230004", "associate4@inha.edu", UserRole.ASSOCIATE);
            createAndSaveUser("20230005", "associate5@inha.edu", UserRole.ASSOCIATE);

            Pageable pageable = PageRequest.of(0, 2);

            // when
            Page<AssociateInfoResponse> result = getPendingAssociatesService.getPendingAssociates(pageable, adminUser.getId());

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.getNumber()).isZero();
        }
    }

    @Nested
    @DisplayName("권한 검증")
    class AuthorizationTest {

        @Test
        @DisplayName("운영진 목록 조회 시도 시 거부 - AdminRequiredException 발생 [APR-034]")
        void getPendingAssociates_WithOperatorRole_ThrowsAdminRequiredException() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when & then
            assertThatThrownBy(() -> getPendingAssociatesService.getPendingAssociates(pageable, operatorUser.getId()))
                    .isInstanceOf(AdminRequiredException.class);
        }
    }
}
