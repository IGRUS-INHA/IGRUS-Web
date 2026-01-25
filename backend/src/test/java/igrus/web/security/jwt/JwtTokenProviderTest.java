package igrus.web.security.jwt;

import igrus.web.security.jwt.exception.AccessTokenExpiredException;
import igrus.web.security.jwt.exception.AccessTokenInvalidException;
import igrus.web.security.jwt.exception.InvalidTokenTypeException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String SECRET = "test-secret-key-for-testing-purposes-only-must-be-at-least-256-bits";
    private static final long ACCESS_TOKEN_VALIDITY = 3600000L; // 1시간
    private static final long REFRESH_TOKEN_VALIDITY = 604800000L; // 7일
    private static final String ISSUER = "igrus-api";
    private static final String AUDIENCE = "igrus-web";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, ACCESS_TOKEN_VALIDITY, REFRESH_TOKEN_VALIDITY, ISSUER, AUDIENCE);
    }

    @Nested
    @DisplayName("Access Token 생성")
    class CreateAccessToken {

        @Test
        @DisplayName("유효한 정보로 Access Token 생성 성공")
        void createAccessToken_WithValidInfo_ReturnsToken() {
            // given
            Long userId = 1L;
            String studentId = "12345678";
            String role = "USER";

            // when
            String token = jwtTokenProvider.createAccessToken(userId, studentId, role);

            // then
            assertThat(token).isNotNull();
            assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(userId);
            assertThat(jwtTokenProvider.getStudentId(token)).isEqualTo(studentId);
            assertThat(jwtTokenProvider.getRole(token)).isEqualTo(role);
            assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("access");
        }

        @Test
        @DisplayName("Access Token 생성 시 issuer, audience 클레임 포함")
        void createAccessToken_ContainsIssuerAndAudience() {
            // given
            Long userId = 1L;
            String studentId = "12345678";
            String role = "USER";

            // when
            String token = jwtTokenProvider.createAccessToken(userId, studentId, role);

            // then
            var claims = jwtTokenProvider.getClaims(token);
            assertThat(claims.getIssuer()).isEqualTo(ISSUER);
            assertThat(claims.getAudience()).contains(AUDIENCE);
        }
    }

    @Nested
    @DisplayName("Refresh Token 생성")
    class CreateRefreshToken {

        @Test
        @DisplayName("유효한 정보로 Refresh Token 생성 성공")
        void createRefreshToken_WithValidInfo_ReturnsToken() {
            // given
            Long userId = 1L;

            // when
            String token = jwtTokenProvider.createRefreshToken(userId);

            // then
            assertThat(token).isNotNull();
            assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(userId);
            assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("refresh");
        }

        @Test
        @DisplayName("Refresh Token 생성 시 issuer, audience 클레임 포함")
        void createRefreshToken_ContainsIssuerAndAudience() {
            // given
            Long userId = 1L;

            // when
            String token = jwtTokenProvider.createRefreshToken(userId);

            // then
            var claims = jwtTokenProvider.getClaims(token);
            assertThat(claims.getIssuer()).isEqualTo(ISSUER);
            assertThat(claims.getAudience()).contains(AUDIENCE);
        }
    }

    @Nested
    @DisplayName("토큰 유효성 검증")
    class ValidateToken {

        @Test
        @DisplayName("유효한 토큰 검증 성공")
        void validateToken_WithValidToken_ReturnsTrue() {
            // given
            String token = jwtTokenProvider.createAccessToken(1L, "12345678", "USER");

            // when
            boolean isValid = jwtTokenProvider.validateToken(token);

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("잘못된 토큰 검증 실패")
        void validateToken_WithInvalidToken_ReturnsFalse() {
            // given
            String invalidToken = "invalid.token.here";

            // when
            boolean isValid = jwtTokenProvider.validateToken(invalidToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("잘못된 issuer를 가진 토큰 검증 실패")
        void validateToken_WithWrongIssuer_ReturnsFalse() {
            // given
            SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            String tokenWithWrongIssuer = Jwts.builder()
                    .subject("1")
                    .issuer("wrong-issuer")
                    .audience().add(AUDIENCE).and()
                    .claim("type", "access")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
                    .signWith(key)
                    .compact();

            // when
            boolean isValid = jwtTokenProvider.validateToken(tokenWithWrongIssuer);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("잘못된 audience를 가진 토큰 검증 실패")
        void validateToken_WithWrongAudience_ReturnsFalse() {
            // given
            SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            String tokenWithWrongAudience = Jwts.builder()
                    .subject("1")
                    .issuer(ISSUER)
                    .audience().add("wrong-audience").and()
                    .claim("type", "access")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
                    .signWith(key)
                    .compact();

            // when
            boolean isValid = jwtTokenProvider.validateToken(tokenWithWrongAudience);

            // then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("validateTokenOrThrow")
    class ValidateTokenOrThrow {

        @Test
        @DisplayName("유효한 토큰 검증 시 예외 없음")
        void validateTokenOrThrow_WithValidToken_NoException() {
            // given
            String token = jwtTokenProvider.createAccessToken(1L, "12345678", "USER");

            // when & then
            jwtTokenProvider.validateTokenOrThrow(token);
        }

        @Test
        @DisplayName("잘못된 토큰 검증 시 AccessTokenInvalidException 발생")
        void validateTokenOrThrow_WithInvalidToken_ThrowsException() {
            // given
            String invalidToken = "invalid.token.here";

            // when & then
            assertThatThrownBy(() -> jwtTokenProvider.validateTokenOrThrow(invalidToken))
                    .isInstanceOf(AccessTokenInvalidException.class);
        }

        @Test
        @DisplayName("만료된 토큰 검증 시 AccessTokenExpiredException 발생")
        void validateTokenOrThrow_WithExpiredToken_ThrowsException() {
            // given
            SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            String expiredToken = Jwts.builder()
                    .subject("1")
                    .issuer(ISSUER)
                    .audience().add(AUDIENCE).and()
                    .claim("type", "access")
                    .issuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2시간 전
                    .expiration(new Date(System.currentTimeMillis() - 3600000)) // 1시간 전 만료
                    .signWith(key)
                    .compact();

            // when & then
            assertThatThrownBy(() -> jwtTokenProvider.validateTokenOrThrow(expiredToken))
                    .isInstanceOf(AccessTokenExpiredException.class);
        }
    }

    @Nested
    @DisplayName("validateAccessToken")
    class ValidateAccessToken {

        @Test
        @DisplayName("유효한 Access Token 검증 성공")
        void validateAccessToken_WithValidAccessToken_NoException() {
            // given
            String token = jwtTokenProvider.createAccessToken(1L, "12345678", "USER");

            // when & then
            jwtTokenProvider.validateAccessToken(token);
        }

        @Test
        @DisplayName("Refresh Token으로 Access Token 검증 시 InvalidTokenTypeException 발생")
        void validateAccessToken_WithRefreshToken_ThrowsException() {
            // given
            String refreshToken = jwtTokenProvider.createRefreshToken(1L);

            // when & then
            assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(refreshToken))
                    .isInstanceOf(InvalidTokenTypeException.class);
        }
    }

    @Nested
    @DisplayName("validateRefreshToken")
    class ValidateRefreshToken {

        @Test
        @DisplayName("유효한 Refresh Token 검증 성공")
        void validateRefreshToken_WithValidRefreshToken_NoException() {
            // given
            String token = jwtTokenProvider.createRefreshToken(1L);

            // when & then
            jwtTokenProvider.validateRefreshToken(token);
        }

        @Test
        @DisplayName("Access Token으로 Refresh Token 검증 시 InvalidTokenTypeException 발생")
        void validateRefreshToken_WithAccessToken_ThrowsException() {
            // given
            String accessToken = jwtTokenProvider.createAccessToken(1L, "12345678", "USER");

            // when & then
            assertThatThrownBy(() -> jwtTokenProvider.validateRefreshToken(accessToken))
                    .isInstanceOf(InvalidTokenTypeException.class);
        }
    }
}
