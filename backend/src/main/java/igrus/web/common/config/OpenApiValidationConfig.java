package igrus.web.common.config;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.springmvc.OpenApiValidationFilter;
import com.atlassian.oai.validator.springmvc.OpenApiValidationInterceptor;
import com.atlassian.oai.validator.springmvc.ValidationReportHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * dev/test 프로필에서만 활성화되는 OpenAPI 응답 스키마 검증 설정.
 *
 * <p>모든 API 응답이 OpenAPI 스펙에 정의된 스키마와 일치하는지 런타임에 검증한다.
 * 검증 실패 시 예외를 던지지 않고, WARN 로그로 스키마 불일치 상세 메시지를 출력한다.</p>
 *
 * <p>프로덕션(prod) 프로필에서는 {@code @Profile({"dev", "test"})} 조건에 의해
 * Bean이 등록되지 않으므로 성능에 영향이 없다.</p>
 *
 * <h3>Filter 활성화/비활성화 스위치</h3>
 * <p>{@code @ConditionalOnProperty}를 통해 {@code openapi.validation.filter.enabled=true}일 때만
 * Filter/Interceptor가 활성화된다. 마이그레이션 진행 중에는 {@code application-test.yml}에서
 * {@code false}로 설정하여 Filter를 비활성화하고, MockMvc ResultMatcher 방식만 사용한다.
 * 마이그레이션 완료 후 {@code true}로 변경하면 Filter가 활성화된다.</p>
 *
 * <h3>Validator 생성 로직</h3>
 * <p>Validator 및 LevelResolver 생성은 {@link OpenApiValidatorFactory}에 위임하여,
 * 테스트 유틸리티(OpenApiValidatorUtil)와 동일한 검증 기준을 사용한다.</p>
 *
 * @see OpenApiValidatorFactory
 * @see com.atlassian.oai.validator.springmvc.OpenApiValidationFilter
 * @see com.atlassian.oai.validator.springmvc.OpenApiValidationInterceptor
 */
@Slf4j
@Configuration
@Profile({"dev", "test"})
@ConditionalOnProperty(name = "openapi.validation.filter.enabled", havingValue = "true", matchIfMissing = false)
public class OpenApiValidationConfig implements WebMvcConfigurer {

    private final OpenApiValidationInterceptor validationInterceptor;

    /**
     * OpenAPI 스펙 파일을 파싱하여 Validator와 Interceptor를 초기화한다.
     *
     * <p>스펙 파일 파싱에 ~3초가 소요될 수 있으며, 파일 미존재 시에도
     * ApplicationContext 로딩이 실패하지 않도록 graceful degradation을 적용한다.
     * 초기화 실패 시 검증이 비활성화된 상태로 동작한다.</p>
     */
    public OpenApiValidationConfig() {
        OpenApiValidationInterceptor interceptor = null;
        try {
            OpenApiInteractionValidator validator = OpenApiValidatorFactory.createValidator();
            interceptor = new OpenApiValidationInterceptor(validator, new LoggingValidationReportHandler());
            log.info("OpenAPI 응답 검증 Filter/Interceptor 활성화 (dev/test 프로필)");
        } catch (Exception e) {
            log.warn("OpenAPI 응답 검증 초기화 실패. 검증이 비활성화됩니다: {}", e.getMessage());
        }
        this.validationInterceptor = interceptor;
    }

    /**
     * 요청/응답 검증을 수행하는 Servlet Filter를 등록한다.
     *
     * <p>Filter는 요청 본문을 캐싱하여 Interceptor에서 검증할 수 있도록 한다.
     * 첫 번째 인자(validateRequest)와 두 번째 인자(validateResponse) 모두 true로 설정하여
     * 요청과 응답 모두를 검증한다.</p>
     */
    @Bean
    public OpenApiValidationFilter openApiValidationFilter() {
        boolean enabled = (validationInterceptor != null);
        return new OpenApiValidationFilter(enabled, enabled);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (validationInterceptor != null) {
            registry.addInterceptor(validationInterceptor);
        }
    }

    /**
     * 검증 실패 시 예외 대신 WARN 로그를 출력하는 ValidationReportHandler.
     *
     * <p>{@link com.atlassian.oai.validator.springmvc.DefaultValidationReportHandler}는
     * 검증 실패 시 {@link com.atlassian.oai.validator.springmvc.InvalidRequestException} 또는
     * {@link com.atlassian.oai.validator.springmvc.InvalidResponseException}을 던지지만,
     * 이 구현은 로그만 남기고 응답을 정상 반환한다.</p>
     *
     * <p>ERROR 메시지뿐 아니라 WARN 메시지도 로그에 출력한다.
     * {@code hasErrors()}는 ERROR 레벨만 확인하므로, WARN만 존재하는 경우도
     * 별도로 처리하여 스키마 불일치 정보를 놓치지 않도록 한다.</p>
     */
    static class LoggingValidationReportHandler implements ValidationReportHandler {

        @Override
        public void handleRequestReport(String uri, ValidationReport report) {
            if (report.hasErrors()) {
                log.warn("[OpenAPI 요청 검증 실패] URI={}, 메시지={}", uri, formatMessages(report));
            } else if (hasWarnings(report)) {
                log.warn("[OpenAPI 요청 검증 경고] URI={}, 메시지={}", uri, formatMessages(report));
            }
        }

        @Override
        public void handleResponseReport(String uri, ValidationReport report) {
            if (report.hasErrors()) {
                log.warn("[OpenAPI 응답 검증 실패] URI={}, 메시지={}", uri, formatMessages(report));
            } else if (hasWarnings(report)) {
                log.warn("[OpenAPI 응답 검증 경고] URI={}, 메시지={}", uri, formatMessages(report));
            }
        }

        private boolean hasWarnings(ValidationReport report) {
            return report.getMessages().stream()
                    .anyMatch(msg -> msg.getLevel() == ValidationReport.Level.WARN);
        }

        private String formatMessages(ValidationReport report) {
            StringBuilder sb = new StringBuilder();
            report.getMessages().forEach(msg -> {
                if (msg.getLevel() == ValidationReport.Level.ERROR
                        || msg.getLevel() == ValidationReport.Level.WARN) {
                    sb.append("\n  - [").append(msg.getLevel()).append("] ").append(msg.getMessage());
                }
            });
            return sb.toString();
        }
    }
}
