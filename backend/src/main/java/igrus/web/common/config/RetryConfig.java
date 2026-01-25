package igrus.web.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring Retry 설정 클래스.
 * 이메일 발송 등의 일시적 실패에 대한 재시도 로직을 활성화합니다.
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
