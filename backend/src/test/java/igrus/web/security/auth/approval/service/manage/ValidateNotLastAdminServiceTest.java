package igrus.web.security.auth.approval.service.manage;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.approval.exception.LastAdminCannotChangeException;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ValidateNotLastAdminService 통합 테스트")
class ValidateNotLastAdminServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private ValidateNotLastAdminService validateNotLastAdminService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        setUpBase();

        adminUser = createAndSaveUser("20200001", "admin@inha.edu", UserRole.ADMIN);
    }

    @Test
    @DisplayName("마지막 ADMIN 권한 변경 시도 시 거부 - LastAdminCannotChangeException 발생 [APR-040]")
    void validateNotLastAdmin_LastAdmin_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> validateNotLastAdminService.validateNotLastAdmin(adminUser.getId()))
                .isInstanceOf(LastAdminCannotChangeException.class);
    }

    @Test
    @DisplayName("여러 ADMIN 존재 시 권한 변경 가능 [APR-041]")
    void validateNotLastAdmin_MultipleAdmins_NoException() {
        // given - 추가 ADMIN 생성
        createAndSaveUser("20200002", "admin2@inha.edu", UserRole.ADMIN);

        // when & then (예외 발생하지 않음)
        validateNotLastAdminService.validateNotLastAdmin(adminUser.getId());
    }
}
