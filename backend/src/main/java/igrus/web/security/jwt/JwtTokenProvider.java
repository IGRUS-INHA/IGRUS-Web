package igrus.web.security.jwt;

import igrus.web.security.jwt.exception.AccessTokenExpiredException;
import igrus.web.security.jwt.exception.AccessTokenInvalidException;
import igrus.web.security.jwt.exception.InvalidTokenTypeException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final int MINIMUM_SECRET_KEY_LENGTH = 32;

    private final SecretKey secretKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;
    private final String issuer;
    private final String audience;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-validity}") long accessTokenValidity,
            @Value("${app.jwt.refresh-token-validity}") long refreshTokenValidity,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience
    ) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MINIMUM_SECRET_KEY_LENGTH) {
            throw new IllegalArgumentException("JWT 비밀키는 최소 " + MINIMUM_SECRET_KEY_LENGTH + "바이트 필요");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
        this.issuer = issuer;
        this.audience = audience;
    }

    // Access Token 생성
    public String createAccessToken(Long userId, String studentId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValidity);

        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // 각 토큰을 고유하게 식별하기 위한 jti 클레임
                .subject(userId.toString())
                .issuer(issuer)
                .audience().add(audience).and()
                .claim("studentId", studentId)
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    // Refresh Token 생성
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenValidity);

        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // 각 토큰을 고유하게 식별하기 위한 jti 클레임
                .subject(userId.toString())
                .issuer(issuer)
                .audience().add(audience).and()
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰 유효성 검증 (하위 호환 유지)
     *
     * @param token JWT 토큰
     * @return 유효하면 true, 아니면 false
     * @deprecated 1.0부터 사용 중단 예정. {@link #validateTokenOrThrow(String)} 또는
     *             {@link #validateAndGetClaims(String)} 사용 권장
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 토큰 유효성 검증 및 예외 던지기
     *
     * @param token JWT 토큰
     * @throws AccessTokenExpiredException 토큰이 만료된 경우
     * @throws AccessTokenInvalidException 토큰이 유효하지 않은 경우
     */
    public void validateTokenOrThrow(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            throw new AccessTokenExpiredException();
        } catch (JwtException | IllegalArgumentException e) {
            throw new AccessTokenInvalidException();
        }
    }

    /**
     * Access Token 유효성 검증 (타입 검증 포함)
     *
     * @param token JWT 토큰
     * @throws AccessTokenExpiredException 토큰이 만료된 경우
     * @throws AccessTokenInvalidException 토큰이 유효하지 않은 경우
     * @throws InvalidTokenTypeException 토큰 타입이 access가 아닌 경우
     */
    public void validateAccessToken(String token) {
        validateTokenOrThrow(token);

        String tokenType = getTokenType(token);
        if (!"access".equals(tokenType)) {
            throw new InvalidTokenTypeException();
        }
    }

    /**
     * Refresh Token 유효성 검증 (타입 검증 포함)
     *
     * @param token JWT 토큰
     * @throws AccessTokenExpiredException 토큰이 만료된 경우
     * @throws AccessTokenInvalidException 토큰이 유효하지 않은 경우
     * @throws InvalidTokenTypeException 토큰 타입이 refresh가 아닌 경우
     */
    public void validateRefreshToken(String token) {
        validateTokenOrThrow(token);

        String tokenType = getTokenType(token);
        if (!"refresh".equals(tokenType)) {
            throw new InvalidTokenTypeException();
        }
    }

    // 토큰에서 Claims 추출
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 토큰 유효성 검증 및 Claims 반환 (검증 + Claims 반환을 한 번에 수행)
     *
     * @param token JWT 토큰
     * @return Claims 객체
     * @throws AccessTokenExpiredException 토큰이 만료된 경우
     * @throws AccessTokenInvalidException 토큰이 유효하지 않은 경우
     */
    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new AccessTokenExpiredException();
        } catch (JwtException | IllegalArgumentException e) {
            throw new AccessTokenInvalidException();
        }
    }

    /**
     * Access Token 유효성 검증 및 Claims 반환 (타입 검증 포함)
     *
     * @param token JWT 토큰
     * @return Claims 객체
     * @throws AccessTokenExpiredException 토큰이 만료된 경우
     * @throws AccessTokenInvalidException 토큰이 유효하지 않은 경우
     * @throws InvalidTokenTypeException 토큰 타입이 access가 아닌 경우
     */
    public Claims validateAccessTokenAndGetClaims(String token) {
        Claims claims = validateAndGetClaims(token);

        String tokenType = getTokenTypeFromClaims(claims);
        if (!"access".equals(tokenType)) {
            throw new InvalidTokenTypeException();
        }

        return claims;
    }

    // Claims에서 유저 ID 추출
    public Long getUserIdFromClaims(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    // Claims에서 학번 추출
    public String getStudentIdFromClaims(Claims claims) {
        return claims.get("studentId", String.class);
    }

    // Claims에서 역할 추출
    public String getRoleFromClaims(Claims claims) {
        return claims.get("role", String.class);
    }

    // Claims에서 토큰 타입 추출
    public String getTokenTypeFromClaims(Claims claims) {
        return claims.get("type", String.class);
    }

    /**
     * 토큰에서 유저 ID 추출
     *
     * @param token JWT 토큰
     * @return 유저 ID
     * @deprecated 1.0부터 사용 중단 예정. {@link #validateAndGetClaims(String)}로 Claims를 얻은 후
     *             {@link #getUserIdFromClaims(Claims)} 사용 권장
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    /**
     * 토큰에서 학번 추출
     *
     * @param token JWT 토큰
     * @return 학번
     * @deprecated 1.0부터 사용 중단 예정. {@link #validateAndGetClaims(String)}로 Claims를 얻은 후
     *             {@link #getStudentIdFromClaims(Claims)} 사용 권장
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public String getStudentId(String token) {
        return getClaims(token).get("studentId", String.class);
    }

    /**
     * 토큰에서 역할 추출
     *
     * @param token JWT 토큰
     * @return 역할
     * @deprecated 1.0부터 사용 중단 예정. {@link #validateAndGetClaims(String)}로 Claims를 얻은 후
     *             {@link #getRoleFromClaims(Claims)} 사용 권장
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * 토큰 타입 확인 (access / refresh)
     *
     * @param token JWT 토큰
     * @return 토큰 타입 ("access" 또는 "refresh")
     * @deprecated 1.0부터 사용 중단 예정. {@link #validateAndGetClaims(String)}로 Claims를 얻은 후
     *             {@link #getTokenTypeFromClaims(Claims)} 사용 권장
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public String getTokenType(String token) {
        return getClaims(token).get("type", String.class);
    }

    /**
     * 토큰 만료 여부 확인
     *
     * @param token JWT 토큰
     * @return 만료되었으면 true, 아니면 false
     * @deprecated 1.0부터 사용 중단 예정. {@link #validateTokenOrThrow(String)} 또는
     *             {@link #validateAndGetClaims(String)} 사용 시 만료된 경우
     *             {@link AccessTokenExpiredException}이 발생하므로 이를 활용 권장
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
}
