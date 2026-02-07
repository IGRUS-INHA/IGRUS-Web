package igrus.web.user.withdrawal.service;

import igrus.web.security.auth.common.exception.account.AccountSuspendedException;
import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.event.AccountStatusChangeEvent;
import igrus.web.user.withdrawal.domain.WithdrawalLog;
import igrus.web.user.withdrawal.dto.request.WithdrawRequest;
import igrus.web.user.withdrawal.repository.WithdrawalLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static igrus.web.common.fixture.UserTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * WithdrawService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>WD-001: 회원 탈퇴 성공 - 상태 변경, soft delete, 토큰 무효화, 로그 저장</li>
 *     <li>WD-002: 비밀번호 불일치 시 InvalidCredentialsException</li>
 *     <li>WD-003: User 조회 실패 → UserNotFoundException</li>
 *     <li>WD-004: PasswordCredential 없는 사용자 → UserNotFoundException</li>
 *     <li>WD-005: 탈퇴 로그에 사유가 정상 저장되는지 확인</li>
 *     <li>WD-006: 정지 상태 사용자 탈퇴 시 AccountSuspendedException</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WithdrawService 단위 테스트")
class WithdrawServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordCredentialRepository passwordCredentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private WithdrawalLogRepository withdrawalLogRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WithdrawService withdrawService;

    private User memberUser;

    @BeforeEach
    void setUp() {
        memberUser = createMemberWithId();
    }

    @Nested
    @DisplayName("회원 탈퇴 테스트")
    class WithdrawTest {

        @DisplayName("WD-001: 회원 탈퇴 성공 - 상태 변경, soft delete, 토큰 무효화, 로그 저장")
        @Test
        void withdraw_WithValidRequest_Success() {
            // given
            Long userId = memberUser.getId();
            WithdrawRequest request = new WithdrawRequest("Password1!", "더 이상 사용하지 않습니다");

            PasswordCredential credential = PasswordCredential.create(memberUser, "hashedPw");
            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("Password1!", "hashedPw")).willReturn(true);

            // when
            withdrawService.withdraw(userId, request);

            // then
            assertThat(memberUser.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
            assertThat(memberUser.isDeleted()).isTrue();
            assertThat(credential.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
            assertThat(credential.isDeleted()).isTrue();
            verify(refreshTokenRepository).revokeAllByUserId(userId);
            verify(withdrawalLogRepository).save(any(WithdrawalLog.class));
            verify(eventPublisher).publishEvent(any(AccountStatusChangeEvent.class));
        }

        @DisplayName("WD-002: 비밀번호 불일치 시 InvalidCredentialsException 발생")
        @Test
        void withdraw_WithWrongPassword_ThrowsInvalidCredentialsException() {
            // given
            Long userId = memberUser.getId();
            WithdrawRequest request = new WithdrawRequest("wrongPassword!", "탈퇴합니다");

            PasswordCredential credential = PasswordCredential.create(memberUser, "hashedPw");
            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("wrongPassword!", "hashedPw")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> withdrawService.withdraw(userId, request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @DisplayName("WD-003: User 조회 실패 시 UserNotFoundException 발생")
        @Test
        void withdraw_WhenUserNotFound_ThrowsUserNotFoundException() {
            // given
            Long nonExistentUserId = 999L;
            WithdrawRequest request = new WithdrawRequest("Password1!", "탈퇴합니다");

            given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> withdrawService.withdraw(nonExistentUserId, request))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @DisplayName("WD-004: PasswordCredential 없는 사용자 탈퇴 시 UserNotFoundException 발생")
        @Test
        void withdraw_WhenCredentialNotFound_ThrowsUserNotFoundException() {
            // given
            Long userId = memberUser.getId();
            WithdrawRequest request = new WithdrawRequest("Password1!", "탈퇴합니다");

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> withdrawService.withdraw(userId, request))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @DisplayName("WD-005: 탈퇴 로그에 사유가 정상 저장되는지 확인")
        @Test
        void withdraw_SavesWithdrawalLogWithReason() {
            // given
            Long userId = memberUser.getId();
            String reason = "동아리 활동이 맞지 않아서 탈퇴합니다";
            WithdrawRequest request = new WithdrawRequest("Password1!", reason);

            PasswordCredential credential = PasswordCredential.create(memberUser, "hashedPw");
            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(passwordCredentialRepository.findByUserId(userId)).willReturn(Optional.of(credential));
            given(passwordEncoder.matches("Password1!", "hashedPw")).willReturn(true);

            // when
            withdrawService.withdraw(userId, request);

            // then
            ArgumentCaptor<WithdrawalLog> captor = ArgumentCaptor.forClass(WithdrawalLog.class);
            verify(withdrawalLogRepository).save(captor.capture());

            WithdrawalLog savedLog = captor.getValue();
            assertThat(savedLog.getUser()).isEqualTo(memberUser);
            assertThat(savedLog.getReason()).isEqualTo(reason);
        }

        @DisplayName("WD-006: 정지 상태 사용자 탈퇴 시 AccountSuspendedException 발생")
        @Test
        void withdraw_WhenUserSuspended_ThrowsAccountSuspendedException() {
            // given
            Long userId = memberUser.getId();
            WithdrawRequest request = new WithdrawRequest("Password1!", "탈퇴합니다");

            memberUser.suspend();
            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));

            // when & then
            assertThatThrownBy(() -> withdrawService.withdraw(userId, request))
                    .isInstanceOf(AccountSuspendedException.class);

            // 비밀번호 검증, 탈퇴 처리가 수행되지 않았는지 확인
            verify(passwordCredentialRepository, never()).findByUserId(userId);
            verify(withdrawalLogRepository, never()).save(any());
            verify(refreshTokenRepository, never()).revokeAllByUserId(userId);
        }
    }
}
