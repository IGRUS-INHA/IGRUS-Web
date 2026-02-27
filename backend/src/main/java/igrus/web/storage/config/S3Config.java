package igrus.web.storage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3 Presigner Bean 설정.
 * S3Client는 spring-cloud-aws-starter-s3가 자동 등록하지만,
 * S3Presigner는 자동 등록되지 않으므로 수동으로 Bean을 등록한다.
 */
@Configuration
public class S3Config {

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.create();
    }
}
