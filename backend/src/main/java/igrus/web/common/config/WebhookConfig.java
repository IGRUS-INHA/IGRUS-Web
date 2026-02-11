package igrus.web.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 웹훅 관련 RestClient 빈 설정.
 * 프로덕션 환경에서만 활성화됩니다.
 */
@Configuration
@Profile("prod")
public class WebhookConfig {

    @Bean(name = "baebdungiRestClient")
    public RestClient baebdungiRestClient(BaebdungiWebhookProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(properties.timeout());

        return RestClient.builder()
                .baseUrl(properties.url())
                .defaultHeader("X-Webhook-Secret", properties.secret())
                .requestFactory(requestFactory)
                .build();
    }
}
