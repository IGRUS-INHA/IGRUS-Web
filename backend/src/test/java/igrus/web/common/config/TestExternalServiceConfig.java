package igrus.web.common.config;

import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.webhook.baebdungi.service.BaebdungiWebhookService;
import java.time.Clock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * 테스트 전용 외부 서비스 Mock 설정.
 *
 * <p>외부 의존성(이메일, 웹훅, 시계, S3)을 Mockito Mock으로 교체하여
 * 테스트에서 실제 외부 서비스를 호출하지 않도록 합니다.</p>
 *
 * <p>{@code @MockitoBean} 대신 {@code @TestConfiguration + @Primary}를 사용하여
 * 모든 테스트가 동일한 Spring ApplicationContext를 공유하도록 합니다.
 * {@code @MockitoBean}은 Mock 조합마다 별도의 Context를 생성하여
 * 테스트 실행 시간을 크게 증가시킵니다.</p>
 */
@TestConfiguration
public class TestExternalServiceConfig {

    @Bean
    @Primary
    public AuthEmailService testAuthEmailService() {
        return Mockito.mock(AuthEmailService.class);
    }

    @Bean
    @Primary
    public BaebdungiWebhookService testBaebdungiWebhookService() {
        return Mockito.mock(BaebdungiWebhookService.class);
    }

    @Bean
    @Primary
    public Clock testClock() {
        return Mockito.mock(Clock.class);
    }

    @Bean
    @Primary
    public S3Client testS3Client() {
        return Mockito.mock(S3Client.class);
    }

    @Bean
    @Primary
    public S3Presigner testS3Presigner() {
        return Mockito.mock(S3Presigner.class);
    }
}
