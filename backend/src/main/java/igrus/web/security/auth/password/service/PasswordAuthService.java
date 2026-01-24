package igrus.web.security.auth.password.service;

import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import igrus.web.security.auth.common.exception.email.EmailNotVerifiedException;
import igrus.web.security.auth.common.exception.token.RefreshTokenExpiredException;
import igrus.web.security.auth.common.exception.token.RefreshTokenInvalidException;
import igrus.web.security.auth.password.dto.request.PasswordLoginRequest;
import igrus.web.security.auth.password.dto.request.PasswordLogoutRequest;
import igrus.web.security.auth.password.dto.request.TokenRefreshRequest;
import igrus.web.security.auth.password.dto.response.PasswordLoginResponse;
import igrus.web.security.auth.password.dto.response.TokenRefreshResponse;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.security.auth.common.exception.account.AccountSuspendedException;
import igrus.web.security.auth.common.exception.account.AccountWithdrawnException;
import igrus.web.security.auth.common.service.LoginAttemptService;
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
@Transactional
@RequiredArgsConstructor
public class PasswordAuthService {

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptService loginAttemptService;
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
     * @return 로그인 응답 (토큰 및 사용자 정보)
     * @throws igrus.web.security.auth.common.exception.account.AccountLockedException 계정이 잠금 상태인 경우
     * @throws InvalidCredentialsException 학번 또는 비밀번호가 올바르지 않은 경우
     * @throws AccountSuspendedException 계정이 정지된 경우
     * @throws AccountWithdrawnException 계정이 탈퇴 상태인 경우
     * @throws EmailNotVerifiedException 이메일 인증이 완료되지 않은 경우
     */
    public PasswordLoginResponse login(PasswordLoginRequest request) {
        log.info("로그인 시도: studentId={}", request.studentId());

        // 0. 계정 잠금 상태 확인
        loginAttemptService.checkAccountLocked(request.studentId());

        // 1. 사용자 조회
        User user = userRepository.findByStudentId(request.studentId())
                .orElseThrow(() -> {
                    log.warn("로그인 실패 - 사용자 없음: studentId={}", request.studentId());
                    loginAttemptService.recordFailedAttempt(request.studentId());
                    return new InvalidCredentialsException();
                });

        // 2. 비밀번호 조회 및 검증
        PasswordCredential credential = passwordCredentialRepository.findByUserId(user.getId())
                .orElseThrow(() -> {
                    log.warn("로그인 실패 - 비밀번호 정보 없음: userId={}", user.getId());
                    loginAttemptService.recordFailedAttempt(request.studentId());
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            log.warn("로그인 실패 - 비밀번호 불일치: studentId={}", request.studentId());
            loginAttemptService.recordFailedAttempt(request.studentId());
            throw new InvalidCredentialsException();
        }

        // 3. 계정 상태 확인
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            log.warn("로그인 실패 - 이메일 미인증: studentId={}, email={}", request.studentId(), user.getEmail());
            loginAttemptService.recordFailedAttempt(request.studentId());
            throw new EmailNotVerifiedException();
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            log.warn("로그인 실패 - 계정 정지: studentId={}", request.studentId());
            loginAttemptService.recordFailedAttempt(request.studentId());
            throw new AccountSuspendedException();
        }

        if (user.getStatus() == UserStatus.WITHDRAWN) {
            log.warn("로그인 실패 - 계정 탈퇴: studentId={}", request.studentId());
            loginAttemptService.recordFailedAttempt(request.studentId());
            throw new AccountWithdrawnException();
        }

        // 5. 로그인 성공 - 시도 기록 초기화
        loginAttemptService.resetAttempts(request.studentId());

        // 6. 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getStudentId(),
                user.getRole().name()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 7. RefreshToken 저장
        RefreshToken refreshTokenEntity = RefreshToken.create(user, refreshToken, refreshTokenValidity);
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("로그인 성공: studentId={}, userId={}", request.studentId(), user.getId());

        return PasswordLoginResponse.of(
                accessToken,
                refreshToken,
                user.getId(),
                user.getStudentId(),
                user.getName(),
                user.getRole(),
                accessTokenValidity
        );
    }

    /**
     * 로그아웃을 수행합니다.
     *
     * @param request 로그아웃 요청 (리프레시 토큰)
     * @throws RefreshTokenInvalidException 리프레시 토큰이 유효하지 않은 경우
     */
    public void logout(PasswordLogoutRequest request) {
        log.info("로그아웃 시도");

        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.refreshToken())
                .orElseThrow(() -> {
                    log.warn("로그아웃 실패 - 유효하지 않은 리프레시 토큰");
                    return new RefreshTokenInvalidException();
                });

        refreshToken.revoke();

        log.info("로그아웃 성공: userId={}", refreshToken.getUser().getId());
    }

    /**
     * 리프레시 토큰으로 새로운 액세스 토큰을 발급합니다.
     *
     * @param request 토큰 갱신 요청 (리프레시 토큰)
     * @return 새로운 액세스 토큰 응답
     * @throws RefreshTokenInvalidException 리프레시 토큰이 유효하지 않은 경우
     * @throws RefreshTokenExpiredException 리프레시 토큰이 만료된 경우
     */
    @Transactional(readOnly = true)
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        log.info("토큰 갱신 시도");

        // 1. DB에서 Refresh Token 조회 (revoked가 아닌 토큰)
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByTokenAndRevokedFalse(request.refreshToken())
                .orElseThrow(() -> {
                    log.warn("토큰 갱신 실패 - 유효하지 않은 리프레시 토큰");
                    return new RefreshTokenInvalidException();
                });

        // 2. 만료 여부 확인
        if (refreshTokenEntity.isExpired()) {
            log.warn("토큰 갱신 실패 - 리프레시 토큰 만료: userId={}", refreshTokenEntity.getUser().getId());
            throw new RefreshTokenExpiredException();
        }

        // 3. 새 Access Token 발급
        User user = refreshTokenEntity.getUser();
        String newAccessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getStudentId(),
                user.getRole().name()
        );

        log.info("토큰 갱신 성공: userId={}", user.getId());

        return TokenRefreshResponse.of(newAccessToken, accessTokenValidity);
    }
}
