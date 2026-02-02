package igrus.web.security.auth.password.service.reset;

import igrus.web.security.auth.password.domain.PasswordResetToken;
import igrus.web.security.auth.password.exception.PasswordResetTokenExpiredException;
import igrus.web.security.auth.password.exception.PasswordResetTokenInvalidException;
import igrus.web.security.auth.password.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비밀번호 재설정 토큰 검증 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ValidateResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    /**
     * 재설정 토큰의 유효성을 검증합니다.
     *
     * @param token 검증할 토큰
     * @return 토큰이 유효하면 true
     * @throws PasswordResetTokenInvalidException 토큰이 유효하지 않은 경우
     * @throws PasswordResetTokenExpiredException 토큰이 만료된 경우
     */
    @Transactional(readOnly = true)
    public boolean validateResetToken(String token) {
        log.info("비밀번호 재설정 토큰 검증");

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> {
                    log.warn("토큰 검증 실패 - 유효하지 않은 토큰");
                    return new PasswordResetTokenInvalidException();
                });

        if (resetToken.isExpired()) {
            log.warn("토큰 검증 실패 - 만료된 토큰");
            throw new PasswordResetTokenExpiredException();
        }

        log.info("토큰 검증 성공");
        return true;
    }
}
