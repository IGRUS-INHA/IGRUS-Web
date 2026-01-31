package igrus.web.security.auth.common.service;

import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.common.dto.internal.RecoveryResult;
import igrus.web.security.auth.common.dto.response.RecoveryEligibilityResponse;
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

import java.time.Duration;
import java.time.Instant;

/**
 * 탈퇴 계정 복구 서비스
 * <p>
 * 탈퇴 후 5일 이내에 계정을 복구할 수 있는 기능을 제공합니다.
 * 개인정보보호법 파기 기한(5일)과 일치시켜 법적 정합성을 확보합니다.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AccountRecoveryService {

    private static final Duration RECOVERY_PERIOD = Duration.ofDays(5);

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
     * 복구 가능 여부를 확인합니다.
     *
     * @param studentId 학번
     * @return 복구 가능 여부 응답
     */
    @Transactional(readOnly = true)
    public RecoveryEligibilityResponse checkRecoveryEligibility(String studentId) {
        log.info("복구 가능 여부 확인: studentId={}", studentId);

        // soft delete 포함하여 사용자 조회
        User user = userRepository.findByStudentIdIncludingDeleted(studentId)
                .orElse(null);

        if (user == null) {
            log.info("사용자 없음: studentId={}", studentId);
            return RecoveryEligibilityResponse.notRecoverable();
        }

        // 탈퇴 상태가 아닌 경우
        if (user.getStatus() != UserStatus.WITHDRAWN) {
            log.info("탈퇴 상태 아님: studentId={}, status={}", studentId, user.getStatus());
            return RecoveryEligibilityResponse.notWithdrawn();
        }

        // soft delete 되지 않은 경우 (논리적 오류)
        if (!user.isDeleted()) {
            log.warn("탈퇴 상태이나 soft delete가 아님: studentId={}", studentId);
            return RecoveryEligibilityResponse.notWithdrawn();
        }

        Instant recoveryDeadline = getRecoveryDeadlineInternal(user);
        Instant now = Instant.now();

        if (now.isAfter(recoveryDeadline)) {
            log.info("복구 기간 만료: studentId={}, deadline={}", studentId, recoveryDeadline);
            return RecoveryEligibilityResponse.notRecoverable();
        }

        log.info("복구 가능: studentId={}, deadline={}", studentId, recoveryDeadline);
        return RecoveryEligibilityResponse.recoverable(recoveryDeadline);
    }

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
        Instant recoveryDeadline = getRecoveryDeadlineInternal(user);
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

    /**
     * 복구 가능 기한을 조회합니다.
     *
     * @param studentId 학번
     * @return 복구 가능 기한 (탈퇴일 + 5일)
     * @throws InvalidCredentialsException    사용자가 존재하지 않는 경우
     * @throws AccountNotRecoverableException 복구 가능 상태가 아닌 경우
     */
    @Transactional(readOnly = true)
    public Instant getRecoveryDeadline(String studentId) {
        log.info("복구 기한 조회: studentId={}", studentId);

        User user = userRepository.findByStudentIdIncludingDeleted(studentId)
                .orElseThrow(() -> {
                    log.warn("복구 기한 조회 실패 - 사용자 없음: studentId={}", studentId);
                    return new InvalidCredentialsException();
                });

        if (user.getStatus() != UserStatus.WITHDRAWN || !user.isDeleted()) {
            log.warn("복구 기한 조회 실패 - 탈퇴 상태 아님: studentId={}", studentId);
            throw new AccountNotRecoverableException();
        }

        return getRecoveryDeadlineInternal(user);
    }

    /**
     * 주어진 학번으로 최근 탈퇴 이력이 있는지 확인합니다 (재가입 제한 체크용).
     *
     * @param studentId 학번
     * @return 재가입 가능 여부 및 재가입 가능 시점
     */
    @Transactional(readOnly = true)
    public ReRegistrationCheckResult checkReRegistrationEligibility(String studentId) {
        log.info("재가입 가능 여부 확인: studentId={}", studentId);

        User user = userRepository.findByStudentIdIncludingDeleted(studentId)
                .orElse(null);

        // 사용자가 존재하지 않으면 재가입 가능
        if (user == null) {
            return ReRegistrationCheckResult.eligible();
        }

        // 탈퇴 상태가 아니면 이미 가입된 상태
        if (user.getStatus() != UserStatus.WITHDRAWN) {
            return ReRegistrationCheckResult.alreadyRegistered();
        }

        // soft delete가 아니면 논리적 오류
        if (!user.isDeleted()) {
            return ReRegistrationCheckResult.alreadyRegistered();
        }

        Instant recoveryDeadline = getRecoveryDeadlineInternal(user);
        Instant now = Instant.now();

        // 복구 기간 내이면 재가입 불가
        if (now.isBefore(recoveryDeadline)) {
            log.info("재가입 불가 - 복구 기간 내: studentId={}, reRegistrationAvailableAt={}", studentId, recoveryDeadline);
            return ReRegistrationCheckResult.restricted(recoveryDeadline);
        }

        // 복구 기간 만료 후 재가입 가능
        return ReRegistrationCheckResult.eligible();
    }

    private Instant getRecoveryDeadlineInternal(User user) {
        Instant deletedAt = user.getDeletedAt();
        if (deletedAt == null) {
            // deletedAt이 null인 경우 (논리적 오류 방어)
            return Instant.MIN;
        }
        return deletedAt.plus(RECOVERY_PERIOD);
    }

    /**
     * 재가입 가능 여부 확인 결과
     */
    public record ReRegistrationCheckResult(
            boolean isEligible,
            boolean isAlreadyRegistered,
            Instant reRegistrationAvailableAt,
            String message
    ) {
        public static ReRegistrationCheckResult eligible() {
            return new ReRegistrationCheckResult(true, false, null, null);
        }

        public static ReRegistrationCheckResult alreadyRegistered() {
            return new ReRegistrationCheckResult(false, true, null, "이미 가입된 학번입니다");
        }

        public static ReRegistrationCheckResult restricted(Instant reRegistrationAvailableAt) {
            return new ReRegistrationCheckResult(
                    false,
                    false,
                    reRegistrationAvailableAt,
                    "탈퇴 후 5일이 지나야 재가입할 수 있습니다"
            );
        }
    }
}
