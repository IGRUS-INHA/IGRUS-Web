package igrus.web.security.auth.common.service.account;

import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.common.dto.internal.RecoveryResult;
import igrus.web.security.auth.common.exception.account.AccountNotRecoverableException;
import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 계정 복구 서비스.
 * <p>
 * 탈퇴 후 5일 이내에 계정을 복구할 수 있는 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecoverAccountService {

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.access-token-validity}")
    private long accessTokenValidity;

    @Value("${app.jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    /**
     * 계정을 복구합니다.
     *
     * @param studentId 학번
     * @param password  비밀번호
     * @return 계정 복구 결과 (토큰 및 사용자 정보)
     * @throws InvalidCredentialsException    학번 또는 비밀번호가 올바르지 않은 경우
     * @throws AccountNotRecoverableException 복구 기간이 만료된 경우
     */
    public RecoveryResult recoverAccount(String studentId, String password) {
        log.info("계정 복구 시도: studentId={}", studentId);

        // 1. 사용자 조회 (soft delete 포함)
        User user = userRepository.findByStudentIdIncludingDeleted(studentId)
                .orElseThrow(() -> {
                    log.warn("계정 복구 실패 - 사용자 없음: studentId={}", studentId);
                    return new InvalidCredentialsException();
                });

        // 2. 탈퇴 상태 확인
        if (user.getStatus() != UserStatus.WITHDRAWN || !user.isDeleted()) {
            log.warn("계정 복구 실패 - 탈퇴 상태 아님: studentId={}, status={}", studentId, user.getStatus());
            throw new InvalidCredentialsException();
        }

        // 3. 비밀번호 조회 및 검증 (soft delete 포함)
        PasswordCredential credential = passwordCredentialRepository.findByUserIdIncludingDeleted(user.getId())
                .orElseThrow(() -> {
                    log.warn("계정 복구 실패 - 비밀번호 정보 없음: userId={}", user.getId());
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(password, credential.getPasswordHash())) {
            log.warn("계정 복구 실패 - 비밀번호 불일치: studentId={}", studentId);
            throw new InvalidCredentialsException();
        }

        // 4. 복구 기간 확인
        Instant recoveryDeadline = RecoveryPeriodConstants.getRecoveryDeadline(user);
        if (Instant.now().isAfter(recoveryDeadline)) {
            log.warn("계정 복구 실패 - 복구 기간 만료: studentId={}, deadline={}", studentId, recoveryDeadline);
            throw new AccountNotRecoverableException();
        }

        // 5. User 상태를 ACTIVE로 변경 및 soft delete 복구
        user.activate();
        user.restore();

        // 6. PasswordCredential 상태를 ACTIVE로 변경 및 soft delete 복구
        credential.activate();
        credential.restore();

        // 7. 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getStudentId(),
                user.getRole().name()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 8. RefreshToken 저장
        RefreshToken refreshTokenEntity = RefreshToken.create(user, refreshToken, refreshTokenValidity);
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("계정 복구 성공: studentId={}, userId={}, role={}", studentId, user.getId(), user.getRole());

        return new RecoveryResult(
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
}
