package igrus.web.security.auth.sso;

import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.common.repository.RefreshTokenRepository;
import igrus.web.security.auth.sso.exception.SsoCodeInvalidException;
import igrus.web.security.auth.sso.exception.SsoRedirectUriNotAllowedException;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * 다른 도메인 서비스(play 등)와의 세션 연동(SSO)을 담당합니다.
 *
 * <p>흐름: 브라우저가 refresh 쿠키를 들고 authorize로 top-level 리다이렉트
 * → 일회용 코드 발급 → 서비스 백엔드가 코드를 토큰으로 교환(새 리프레시 토큰 패밀리).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SsoService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final SsoCodeStore codeStore;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.access-token-validity}")
    private long accessTokenValidity;

    @Value("${app.jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    @Value("${app.sso.allowed-redirect-origins:}")
    private List<String> allowedRedirectOrigins;

    /**
     * redirect_uri가 허용된 origin인지 검증합니다.
     *
     * @throws SsoRedirectUriNotAllowedException 절대 URI가 아니거나 허용 목록에 없는 origin인 경우
     */
    public void validateRedirectUri(String redirectUri) {
        URI uri;
        try {
            uri = new URI(redirectUri);
        } catch (URISyntaxException e) {
            throw new SsoRedirectUriNotAllowedException();
        }
        if (!uri.isAbsolute() || uri.getHost() == null) {
            throw new SsoRedirectUriNotAllowedException();
        }
        String origin = uri.getScheme() + "://" + uri.getRawAuthority();
        if (!allowedRedirectOrigins.contains(origin)) {
            log.warn("SSO 허용되지 않은 redirect_uri: {}", redirectUri);
            throw new SsoRedirectUriNotAllowedException();
        }
    }

    /**
     * 유효한 refresh 토큰이면 일회용 코드를 발급합니다.
     * 비로그인(무효/만료/폐기 토큰)이면 empty — 예외를 던지지 않습니다 (리다이렉트 흐름).
     */
    @Transactional(readOnly = true)
    public Optional<String> issueCodeForRefreshToken(String refreshTokenValue) {
        return refreshTokenRepository.findByToken(refreshTokenValue)
                .filter(token -> !token.isRevoked() && !token.isExpired())
                .map(token -> {
                    log.info("SSO 코드 발급: userId={}", token.getUser().getId());
                    return codeStore.issue(token.getUser().getId());
                });
    }

    /**
     * 일회용 코드를 액세스 토큰 + 새 리프레시 토큰(새 토큰 패밀리)으로 교환합니다.
     *
     * @throws SsoCodeInvalidException 무효/만료/재사용 코드이거나 사용자가 활성 상태가 아닌 경우
     */
    public SsoTokenResult exchangeCode(String code) {
        Long userId = codeStore.consume(code).orElseThrow(SsoCodeInvalidException::new);

        User user = userRepository.findById(userId).orElseThrow(SsoCodeInvalidException::new);
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("SSO 코드 교환 거부 - 비활성 사용자: userId={}, status={}", userId, user.getStatus());
            throw new SsoCodeInvalidException();
        }

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getStudentId(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        refreshTokenRepository.save(RefreshToken.createInitial(user, newRefreshToken, refreshTokenValidity));

        log.info("SSO 토큰 발급 성공: userId={}", user.getId());
        return new SsoTokenResult(accessToken, newRefreshToken, accessTokenValidity, refreshTokenValidity);
    }
}
