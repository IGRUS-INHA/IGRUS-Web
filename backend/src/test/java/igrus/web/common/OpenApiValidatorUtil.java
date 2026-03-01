package igrus.web.common;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import igrus.web.common.config.OpenApiValidatorFactory;
import org.springframework.test.web.servlet.ResultMatcher;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;

/**
 * MockMvc 통합 테스트에서 OpenAPI 응답 스키마 검증을 위한 유틸리티 클래스.
 *
 * <p>모든 컨트롤러 통합 테스트에서 공통으로 사용할 {@link OpenApiInteractionValidator}
 * 인스턴스를 싱글턴으로 관리한다. validator 초기화 비용이 높으므로(~3초),
 * 스펙 파일 파싱은 한 번만 수행하고 모든 테스트에서 재사용한다.</p>
 *
 * <p>Validator 및 LevelResolver 생성은 {@link OpenApiValidatorFactory}에 위임하여,
 * 런타임 Filter({@link igrus.web.common.config.OpenApiValidationConfig})와
 * 동일한 검증 기준을 사용한다.</p>
 *
 * <p>사용 예시:
 * <pre>{@code
 * mockMvc.perform(get("/api/v1/boards"))
 *     .andExpect(status().isOk())
 *     .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
 * }</pre>
 * </p>
 *
 * @see OpenApiValidatorFactory
 * @see com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers
 */
public final class OpenApiValidatorUtil {

    private static volatile OpenApiInteractionValidator validatorInstance;

    private OpenApiValidatorUtil() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    /**
     * 싱글턴 {@link OpenApiInteractionValidator} 인스턴스를 반환한다.
     *
     * <p>double-checked locking 패턴으로 thread-safe 초기화를 보장한다.
     * 스펙 파일 파싱은 최초 호출 시 한 번만 수행된다.
     * Validator 생성은 {@link OpenApiValidatorFactory#createValidator()}에 위임한다.</p>
     *
     * @return OpenApiInteractionValidator 싱글턴 인스턴스
     */
    public static OpenApiInteractionValidator getValidator() {
        if (validatorInstance == null) {
            synchronized (OpenApiValidatorUtil.class) {
                if (validatorInstance == null) {
                    validatorInstance = OpenApiValidatorFactory.createValidator();
                }
            }
        }
        return validatorInstance;
    }

    /**
     * OpenAPI 응답 스키마 검증을 수행하는 {@link ResultMatcher}를 반환한다.
     *
     * <p>MockMvc의 {@code andExpect()} 체인에서 간결하게 사용할 수 있도록
     * 정적 메서드로 제공한다.</p>
     *
     * <p>사용 예시:
     * <pre>{@code
     * mockMvc.perform(get("/api/v1/boards"))
     *     .andExpect(status().isOk())
     *     .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
     * }</pre>
     * </p>
     *
     * @return OpenAPI 스키마 검증 ResultMatcher
     */
    public static ResultMatcher matchesOpenApiSpec() {
        return openApi().isValid(getValidator());
    }
}
