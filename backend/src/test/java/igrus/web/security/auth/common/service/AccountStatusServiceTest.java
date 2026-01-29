package igrus.web.security.auth.common.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.exception.account.AccountSuspendedException;
import igrus.web.security.auth.common.exception.account.AccountWithdrawnException;
import igrus.web.security.auth.common.exception.email.EmailNotVerifiedException;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AccountStatusService 통합 테스트")
class AccountStatusServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private AccountStatusService accountStatusService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Nested
    @DisplayName("validateAccountStatus")
    class ValidateAccountStatusTest {

        @Test
        @DisplayName("ACTIVE 상태의 계정 - 예외 없이 정상 통과")
        void validateAccountStatus_ActiveAccount_PassesWithoutException() {
            // given
            User user = createActiveUser();
            Long userId = transactionTemplate.execute(status -> userRepository.save(user).getId());

            // when & then
            assertThatCode(() -> accountStatusService.validateAccountStatus(userId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("SUSPENDED 상태의 계정 - AccountSuspendedException 발생")
        void validateAccountStatus_SuspendedAccount_ThrowsAccountSuspendedException() {
            // given
            User user = createActiveUser();
            user.suspend();
            Long userId = transactionTemplate.execute(status -> userRepository.save(user).getId());

            // when & then
            assertThatThrownBy(() -> accountStatusService.validateAccountStatus(userId))
                    .isInstanceOf(AccountSuspendedException.class);
        }

        @Test
        @DisplayName("WITHDRAWN 상태의 계정 - AccountWithdrawnException 발생")
        void validateAccountStatus_WithdrawnAccount_ThrowsAccountWithdrawnException() {
            // given
            User user = createActiveUser();
            user.withdraw();
            Long userId = transactionTemplate.execute(status -> userRepository.save(user).getId());

            // when & then
            assertThatThrownBy(() -> accountStatusService.validateAccountStatus(userId))
                    .isInstanceOf(AccountWithdrawnException.class);
        }

        @Test
        @DisplayName("PENDING_VERIFICATION 상태의 계정 - EmailNotVerifiedException 발생")
        void validateAccountStatus_PendingVerificationAccount_ThrowsEmailNotVerifiedException() {
            // given
            User user = createPendingVerificationUser();
            Long userId = transactionTemplate.execute(status -> userRepository.save(user).getId());

            // when & then
            assertThatThrownBy(() -> accountStatusService.validateAccountStatus(userId))
                    .isInstanceOf(EmailNotVerifiedException.class);
        }

        @Test
        @DisplayName("존재하지 않는 userId - UserNotFoundException 발생")
        void validateAccountStatus_NonExistentUserId_ThrowsUserNotFoundException() {
            // given
            Long nonExistentUserId = 99999L;

            // when & then
            assertThatThrownBy(() -> accountStatusService.validateAccountStatus(nonExistentUserId))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("ACTIVE에서 SUSPENDED로 상태 변경 후 검증 - AccountSuspendedException 발생")
        void validateAccountStatus_StatusChangedToSuspended_ThrowsException() {
            // given
            User user = createActiveUser();
            Long userId = transactionTemplate.execute(status -> userRepository.save(user).getId());

            // 먼저 정상 검증 통과 확인
            assertThatCode(() -> accountStatusService.validateAccountStatus(userId))
                    .doesNotThrowAnyException();

            // 계정 상태를 SUSPENDED로 변경
            transactionTemplate.execute(status -> {
                User foundUser = userRepository.findById(userId).orElseThrow();
                foundUser.suspend();
                return null;
            });

            // when & then - 다음 검증에서 예외 발생 확인
            assertThatThrownBy(() -> accountStatusService.validateAccountStatus(userId))
                    .isInstanceOf(AccountSuspendedException.class);
        }
    }

    /**
     * ACTIVE 상태의 테스트용 사용자를 생성합니다.
     */
    private User createActiveUser() {
        User user = User.create(
                "20231234",
                "테스트유저",
                "test@inha.edu",
                "010-1234-5678",
                "컴퓨터공학과",
                "테스트 동기"
        );
        user.changeRole(UserRole.ASSOCIATE);
        user.verifyEmail(); // PENDING_VERIFICATION -> ACTIVE
        return user;
    }

    /**
     * PENDING_VERIFICATION 상태의 테스트용 사용자를 생성합니다.
     */
    private User createPendingVerificationUser() {
        User user = User.create(
                "20231235",
                "미인증유저",
                "pending@inha.edu",
                "010-1234-5679",
                "컴퓨터공학과",
                "테스트 동기"
        );
        user.changeRole(UserRole.ASSOCIATE);
        // verifyEmail() 호출하지 않아 PENDING_VERIFICATION 상태 유지
        return user;
    }
}
