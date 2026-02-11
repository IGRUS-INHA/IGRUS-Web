package igrus.web.security.auth.password.service.auth;

import igrus.web.security.auth.common.domain.LoginFailureReason;
import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.common.dto.response.RecoveryEligibilityResponse;
import igrus.web.security.auth.common.exception.account.AccountLockedException;
import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import igrus.web.security.auth.common.exception.account.AccountRecoverableException;
import igrus.web.security.auth.common.exception.email.EmailNotVerifiedException;
import igrus.web.security.auth.common.service.account.CheckRecoveryEligibilityService;
import igrus.web.security.auth.common.service.login.CheckAccountLockedService;
import igrus.web.security.auth.common.service.login.RecordFailedAttemptService;
import igrus.web.security.auth.common.service.login.RecordLoginFailureService;
import igrus.web.security.auth.common.service.login.RecordLoginSuccessService;
import igrus.web.security.auth.common.service.login.ResetLoginAttemptsService;
import igrus.web.security.auth.password.dto.internal.LoginResult;
import igrus.web.security.auth.password.dto.request.PasswordLoginRequest;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.security.auth.password.service.signup.AutoResendVerificationService;
import igrus.web.security.auth.common.exception.account.AccountSuspendedException;
import igrus.web.security.auth.common.exception.account.AccountWithdrawnException;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CheckAccountLockedService checkAccountLockedService;
    private final RecordFailedAttemptService recordFailedAttemptService;
    private final RecordLoginFailureService recordLoginFailureService;
    private final RecordLoginSuccessService recordLoginSuccessService;
    private final ResetLoginAttemptsService resetLoginAttemptsService;
    private final CheckRecoveryEligibilityService checkRecoveryEligibilityService;
    private final AutoResendVerificationService autoResendVerificationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.access-token-validity}")
    private long accessTokenValidity;

    @Value("${app.jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    /**
     * 로그인을 수행합니다.
     *
     * @param request 로그인 요청 (학번, 비밀번호)
     * @param ipAddress 클라이언트 IP 주소
     * @param userAgent 클라이언트 User-Agent
     * @return 로그인 결과 (토큰 및 사용자 정보)
     * @throws AccountLockedException 계정이 잠금 상태인 경우
     * @throws InvalidCredentialsException 학번 또는 비밀번호가 올바르지 않은 경우
     * @throws AccountSuspendedException 계정이 정지된 경우
     * @throws AccountWithdrawnException 계정이 탈퇴 상태이고 복구 불가능한 경우
     * @throws AccountRecoverableException 계정이 탈퇴 상태이지만 복구 가능한 경우
     * @throws EmailNotVerifiedException 이메일 인증이 완료되지 않은 경우
     */
    public LoginResult login(PasswordLoginRequest request, String ipAddress, String userAgent) {
        log.info("로그인 시도: studentId={}, ip={}", request.studentId(), ipAddress);

        // 0. 계정 잠금 상태 확인
        try {
            checkAccountLockedService.checkAccountLocked(request.studentId());
        } catch (AccountLockedException e) {
            recordLoginFailureService.recordFailure(request.studentId(), ipAddress, userAgent,
                    LoginFailureReason.ACCOUNT_LOCKED);
            throw e;
        }

        // 1. 사용자 조회 (soft delete 포함하여 탈퇴 계정 복구 가능 여부 확인)
        User user = userRepository.findByStudentId(request.studentId())
                .orElseGet(() -> checkWithdrawnAccountRecovery(request.studentId(), ipAddress, userAgent));

        // 2. 비밀번호 조회 및 검증
        PasswordCredential credential = passwordCredentialRepository.findByUserId(user.getId())
                .or(() -> passwordCredentialRepository.findByUserIdIncludingDeleted(user.getId()))
                .orElseThrow(() -> {
                    log.warn("로그인 실패 - 비밀번호 정보 없음: userId={}", user.getId());
                    recordFailedAttemptService.recordFailedAttempt(request.studentId());
                    recordLoginFailureService.recordFailure(user, request.studentId(), ipAddress, userAgent,
                            LoginFailureReason.INVALID_CREDENTIALS);
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            log.warn("로그인 실패 - 비밀번호 불일치: studentId={}", request.studentId());
            recordFailedAttemptService.recordFailedAttempt(request.studentId());
            recordLoginFailureService.recordFailure(user, request.studentId(), ipAddress, userAgent,
                    LoginFailureReason.INVALID_CREDENTIALS);
            throw new InvalidCredentialsException();
        }

        // 3. 계정 상태 확인
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            log.warn("로그인 실패 - 이메일 미인증: studentId={}, email={}", request.studentId(), user.getEmail());
            recordFailedAttemptService.recordFailedAttempt(request.studentId());
            recordLoginFailureService.recordFailure(user, request.studentId(), ipAddress, userAgent,
                    LoginFailureReason.EMAIL_NOT_VERIFIED);
            autoResendVerificationService.autoResendIfPossible(user.getEmail());
            throw new EmailNotVerifiedException(user.getEmail());
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            log.warn("로그인 실패 - 계정 정지: studentId={}", request.studentId());
            recordFailedAttemptService.recordFailedAttempt(request.studentId());
            recordLoginFailureService.recordFailure(user, request.studentId(), ipAddress, userAgent,
                    LoginFailureReason.ACCOUNT_SUSPENDED);
            throw new AccountSuspendedException();
        }

        if (user.getStatus() == UserStatus.WITHDRAWN) {
            // 복구 가능 여부 확인
            RecoveryEligibilityResponse eligibility = checkRecoveryEligibilityService.checkRecoveryEligibility(request.studentId());
            if (eligibility.recoverable()) {
                log.info("복구 가능한 탈퇴 계정: studentId={}, recoveryDeadline={}", request.studentId(), eligibility.recoveryDeadline());
                recordLoginFailureService.recordFailure(user, request.studentId(), ipAddress, userAgent,
                        LoginFailureReason.ACCOUNT_RECOVERABLE);
                throw new AccountRecoverableException(request.studentId(), eligibility.recoveryDeadline());
            }
            log.warn("로그인 실패 - 계정 탈퇴 (복구 불가): studentId={}", request.studentId());
            recordFailedAttemptService.recordFailedAttempt(request.studentId());
            recordLoginFailureService.recordFailure(user, request.studentId(), ipAddress, userAgent,
                    LoginFailureReason.ACCOUNT_WITHDRAWN);
            throw new AccountWithdrawnException();
        }

        // 4. 로그인 성공 - 시도 기록 초기화
        resetLoginAttemptsService.resetAttempts(request.studentId());

        // 5. 로그인 히스토리 기록
        recordLoginSuccessService.recordSuccess(user, request.studentId(), ipAddress, userAgent);

        // 6. 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getStudentId(),
                user.getRole().name()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 7. RefreshToken 저장
        RefreshToken refreshTokenEntity = RefreshToken.createInitial(user, refreshToken, refreshTokenValidity);
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("로그인 성공: studentId={}, userId={}", request.studentId(), user.getId());

        return new LoginResult(
                accessToken,
                refreshToken,
                user.getId(),
                user.getStudentId(),
                user.getName(),
                user.getRole(),
                accessTokenValidity,
                refreshTokenValidity
        );
    }

    /**
     * 탈퇴 계정 복구 가능 여부를 확인하고, 복구 가능하면 해당 사용자를 반환합니다.
     * soft delete된 사용자를 조회하여 복구 가능 여부를 판단합니다.
     *
     * @param studentId 학번
     * @param ipAddress 클라이언트 IP 주소
     * @param userAgent 클라이언트 User-Agent
     * @return 탈퇴 상태의 사용자 (복구 가능한 경우)
     * @throws InvalidCredentialsException 사용자가 존재하지 않는 경우
     */
    private User checkWithdrawnAccountRecovery(String studentId, String ipAddress, String userAgent) {
        // soft delete 포함하여 사용자 조회
        User user = userRepository.findByStudentIdIncludingDeleted(studentId)
                .orElseThrow(() -> {
                    log.warn("로그인 실패 - 사용자 없음: studentId={}", studentId);
                    recordFailedAttemptService.recordFailedAttempt(studentId);
                    recordLoginFailureService.recordFailure(studentId, ipAddress, userAgent,
                            LoginFailureReason.INVALID_CREDENTIALS);
                    return new InvalidCredentialsException();
                });

        return user;
    }
}
