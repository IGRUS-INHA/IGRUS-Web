package igrus.web.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 뱁둥이봇 웹훅 관련 설정을 담는 Properties 클래스.
 * application-{profile}.yml의 app.webhook.baebdung-i 설정을 바인딩합니다.
 *
 * @param url     웹훅 엔드포인트 URL
 * @param secret  웹훅 인증 시크릿 (X-Webhook-Secret 헤더)
 * @param enabled 웹훅 활성화 여부 (로컬: false, 프로덕션: true)
 * @param timeout HTTP 요청 타임아웃 (밀리초)
 */
@ConfigurationProperties(prefix = "app.webhook.baebdung-i")
public record BaebdungiWebhookProperties(
        String url,
        String secret,
        boolean enabled,
        int timeout
) {
}
