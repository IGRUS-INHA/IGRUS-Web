package igrus.web.webhook.slack;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Slack Incoming Webhook 으로 운영 채널에 알림을 보낸다.
 * URL(app.webhook.slack.url)이 비어 있으면(로컬/테스트 등) no-op.
 */
@Slf4j
@Component
public class SlackNotifier {

    private final String url;
    private final Executor executor;
    private final RestClient restClient;

    public SlackNotifier(
            @Value("${app.webhook.slack.url:}") String url,
            @Qualifier("webhookTaskExecutor") Executor executor
    ) {
        this.url = url;
        this.executor = executor;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /**
     * 비동기 발송. 어떤 경우에도 예외를 던지지 않으며, 실패 시 로그만 남긴다.
     */
    public void send(String text) {
        if (url.isBlank()) {
            return;
        }
        try {
            executor.execute(() -> post(text));
        } catch (RejectedExecutionException e) {
            log.warn("슬랙 알림 누락: 웹훅 작업 큐가 가득 참");
        }
    }

    private void post(String text) {
        try {
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", escape(text)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("슬랙 알림 실패: status={}", e.getStatusCode());
        } catch (RestClientException e) {
            // 예외 메시지에 웹훅 URL(비밀값)이 포함되므로 타입만 기록
            log.warn("슬랙 알림 실패: {}", e.getClass().getSimpleName());
        }
    }

    /**
     * 사용자 입력(문의 제목 등)이 {@code <!channel>} 멘션이나 링크로 해석되지 않도록 이스케이프한다.
     */
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
