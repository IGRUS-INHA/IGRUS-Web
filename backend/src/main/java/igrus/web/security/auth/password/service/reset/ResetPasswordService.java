package igrus.web.security.auth.password.service.reset;

import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.domain.PasswordResetToken;
import igrus.web.security.auth.password.exception.PasswordResetTokenExpiredException;
import igrus.web.security.auth.password.exception.PasswordResetTokenInvalidException;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.security.auth.password.repository.PasswordResetTokenRepository;
import igrus.web.security.auth.password.service.support.ValidatePasswordFormatService;
import igrus.web.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비밀번호 재설정 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ResetPasswordService {

    private final PasswordCredentialRepository passwordCredentialRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ValidatePasswordFormatService validatePasswordFormatService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 비밀번호를 재설정합니다.
     *
     * @param token 재설정 토큰
     * @param newPassword 새 비밀번호
     * @throws PasswordResetTokenInvalidException 토큰이 유효하지 않은 경우
     * @throws PasswordResetTokenExpiredException 토큰이 만료된 경우
     * @throws igrus.web.security.auth.password.exception.InvalidPasswordFormatException 비밀번호 형식이 올바르지 않은 경우
     */
    public void resetPassword(String token, String newPassword) {
        log.info("비밀번호 재설정 시도");

        // 1. 토큰 유효성 검증
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> {
                    log.warn("비밀번호 재설정 실패 - 유효하지 않은 토큰");
                    return new PasswordResetTokenInvalidException();
                });

        if (resetToken.isExpired()) {
            log.warn("비밀번호 재설정 실패 - 만료된 토큰: userId={}", resetToken.getUser().getId());
            throw new PasswordResetTokenExpiredException();
        }

        // 2. 비밀번호 복잡도 검증
        validatePasswordFormatService.validatePasswordFormat(newPassword);

        // 3. 비밀번호 변경
        User user = resetToken.getUser();
        PasswordCredential credential = passwordCredentialRepository.findByUserId(user.getId())
                .orElseThrow(() -> {
                    log.error("비밀번호 재설정 실패 - 자격 증명 없음: userId={}", user.getId());
                    return new PasswordResetTokenInvalidException();
                });

        String newPasswordHash = passwordEncoder.encode(newPassword);
        credential.changePassword(newPasswordHash);

        // 4. 토큰 사용 처리
        resetToken.markAsUsed();

        // 5. 모든 기존 토큰 무효화 (비밀번호 재설정 토큰 + 리프레시 토큰)
        passwordResetTokenRepository.invalidateAllByUserId(user.getId());
        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("비밀번호 재설정 완료: userId={}", user.getId());
    }
}
