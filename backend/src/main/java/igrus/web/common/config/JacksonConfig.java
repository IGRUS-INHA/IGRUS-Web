package igrus.web.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson 3.x JsonMapper 설정.
 *
 * <p>Jackson 3.x에서는 JSR-310 (Java Time API) 지원이
 * jackson-databind에 내장되어 있습니다.</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder().build();
    }
}
