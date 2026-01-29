package igrus.web.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 쿠키 관련 설정을 담는 Properties 클래스
 * <p>
 * application-{profile}.yml의 app.cookie 설정을 바인딩합니다.
 *
 * @param secure   HTTPS에서만 쿠키 전송 여부 (프로덕션: true, 로컬: false)
 * @param sameSite SameSite 속성 (Strict, Lax, None)
 * @param domain   쿠키 도메인 (null이면 요청 도메인 사용)
 */
@ConfigurationProperties(prefix = "app.cookie")
public record CookieProperties(
        boolean secure,
        String sameSite,
        String domain
) {
}
