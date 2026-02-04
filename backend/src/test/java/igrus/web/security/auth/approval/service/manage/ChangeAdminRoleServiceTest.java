package igrus.web.security.auth.approval.service.manage;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.approval.exception.LastAdminCannotChangeException;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserRoleHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChangeAdminRoleService 통합 테스트")
class ChangeAdminRoleServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private ChangeAdminRoleService changeAdminRoleService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        setUpBase();

        adminUser = createAndSaveUser("20200001", "admin@inha.edu", UserRole.ADMIN);
    }

    @Test
    @DisplayName("ADMIN 권한 변경 - 마지막 ADMIN이 아닌 경우 정상 변경 [APR-041-2]")
    void changeAdminRole_NotLastAdmin_Success() {
        // given
        User admin2 = createAndSaveUser("20200002", "admin2@inha.edu", UserRole.ADMIN);

        // when
        changeAdminRoleService.changeAdminRole(admin2.getId(), UserRole.MEMBER, adminUser.getId());

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
        assertThatThrownBy(() -> changeAdminRoleService.changeAdminRole(adminUser.getId(), UserRole.MEMBER, adminUser.getId()))
                .isInstanceOf(LastAdminCannotChangeException.class);
    }
}
