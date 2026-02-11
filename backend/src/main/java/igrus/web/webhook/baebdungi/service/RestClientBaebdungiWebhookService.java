package igrus.web.webhook.baebdungi.service;

import igrus.web.common.config.BaebdungiWebhookProperties;
import igrus.web.webhook.baebdungi.dto.BaebdungiSubmissionRequest;
import igrus.web.webhook.baebdungi.dto.BaebdungiSubmissionResponse;
import igrus.web.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * RestClient를 사용한 뱁둥이봇 웹훅 서비스 구현체.
 * 프로덕션 환경에서 사용됩니다.
 */
@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class RestClientBaebdungiWebhookService implements BaebdungiWebhookService {

    private final RestClient baebdungiRestClient;
    private final BaebdungiWebhookProperties properties;

    @Async("webhookTaskExecutor")
    @Retryable(
            retryFor = RestClientException.class,
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 2000,       // 2초
                    multiplier = 2,     // 2초 → 4초 → 8초
                    maxDelay = 10000    // 최대 10초
            )
    )
    @Override
    public void sendSubmission(User user) {
        if (!properties.enabled()) {
            log.debug("뱁둥이봇 웹훅 비활성화 상태: studentId={}", user.getStudentId());
            return;
        }

        BaebdungiSubmissionRequest request = BaebdungiSubmissionRequest.fromUser(user);
        log.debug("뱁둥이봇 웹훅 호출 시도: studentId={}", user.getStudentId());

        try {
            BaebdungiSubmissionResponse response = baebdungiRestClient.post()
                    .body(request)
                    .retrieve()
                    .body(BaebdungiSubmissionResponse.class);

            log.info("뱁둥이봇 웹훅 호출 성공: studentId={}, submissionId={}",
                    user.getStudentId(),
                    response != null ? response.submissionId() : "unknown");
        } catch (HttpClientErrorException e) {
            // 4xx 클라이언트 에러는 재시도하지 않고 로그만 기록
            log.error("뱁둥이봇 웹훅 클라이언트 에러 (재시도 불가): studentId={}, status={}, body={}",
                    user.getStudentId(), e.getStatusCode(), e.getResponseBodyAsString());
        }
        // 5xx 서버 에러, 타임아웃 등은 @Retryable이 재시도 처리
    }

    /**
     * 웹훅 호출 재시도 소진 시 복구 메서드.
     * 최종 실패해도 회원가입 프로세스에는 영향을 주지 않습니다.
     */
    @Recover
    public void recoverSendSubmission(RestClientException e, User user) {
        log.error("뱁둥이봇 웹훅 호출 최종 실패 (재시도 소진): studentId={}, error={}",
                user.getStudentId(), e.getMessage());
    }
}
