package igrus.web.security.auth.common.util;

import igrus.web.common.config.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * 쿠키 생성, 삭제, 추출을 담당하는 유틸리티 클래스
 */
@Component
@RequiredArgsConstructor
public class CookieUtil {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final CookieProperties cookieProperties;

    /**
     * 리프레시 토큰 쿠키를 생성합니다.
     *
     * @param refreshToken 리프레시 토큰 값
     * @param maxAge       쿠키 유효 기간
     * @return ResponseCookie 객체
     */
    public ResponseCookie createRefreshTokenCookie(String refreshToken, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(COOKIE_PATH)
                .maxAge(maxAge);

        if (cookieProperties.domain() != null && !cookieProperties.domain().isBlank()) {
            builder.domain(cookieProperties.domain());
        }

        return builder.build();
    }

    /**
     * 리프레시 토큰 쿠키를 삭제합니다 (로그아웃용).
     * maxAge를 0으로 설정하여 브라우저가 쿠키를 삭제하도록 합니다.
     *
     * @return 삭제용 ResponseCookie 객체
     */
    public ResponseCookie deleteRefreshTokenCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(COOKIE_PATH)
                .maxAge(0);

        if (cookieProperties.domain() != null && !cookieProperties.domain().isBlank()) {
            builder.domain(cookieProperties.domain());
        }

        return builder.build();
    }

    /**
     * 요청에서 리프레시 토큰 쿠키를 추출합니다.
     *
     * @param request HTTP 요청
     * @return 리프레시 토큰 값 (Optional)
     */
    public Optional<String> getRefreshTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                String value = cookie.getValue();
                if (value != null && !value.isBlank()) {
                    return Optional.of(value);
                }
            }
        }
        return Optional.empty();
    }
}
