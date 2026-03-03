package igrus.web.common.config;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * OpenAPI 응답 스키마 검증에 사용되는 공통 팩토리.
 *
 * <p>{@link OpenApiInteractionValidator} 생성과 {@link LevelResolver} 구성 로직을
 * 단일 진실점(Single Source of Truth)으로 관리한다.
 * 런타임 Filter({@link OpenApiValidationConfig})와 테스트 유틸리티(OpenApiValidatorUtil)
 * 양쪽에서 이 팩토리를 참조하여 검증 기준의 일관성을 보장한다.</p>
 *
 * <p>PoC(TASK-200)에서 확정된 LevelResolver 설정:
 * <ul>
 *     <li>CSRF 파라미터(_csrf): IGNORE -- MockMvc의 {@code .with(csrf())}가 추가하는 파라미터</li>
 *     <li>Security 검증: IGNORE -- Spring Security가 인증/인가를 담당</li>
 *     <li>additionalProperties: WARN -- 스키마 미명시 시 추가 필드 경고만 (향후 ERROR 전환 가능)</li>
 * </ul>
 * </p>
 *
 * @see OpenApiValidationConfig
 */
@Slf4j
public final class OpenApiValidatorFactory {

    /**
     * 프로젝트 루트 기준 OpenAPI 스펙 파일 상대 경로.
     * 백엔드 모듈(backend/)에서 실행되므로 "../openapi/openapi.yaml"로 접근한다.
     * 멀티파일 구조(openapi.yaml -> paths, schemas)를 $ref로 자동 해석한다.
     */
    static final String SPEC_FILE_PATH = "../openapi/openapi.yaml";

    private OpenApiValidatorFactory() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    /**
     * 프로젝트에 적합한 LevelResolver를 생성한다.
     *
     * <p>PoC(TASK-200)에서 확정된 설정으로, 런타임 Filter와 MockMvc 테스트에서
     * 동일한 검증 레벨을 사용하여 환경 간 검증 기준 불일치를 방지한다.</p>
     *
     * @return 프로젝트 기본 LevelResolver
     */
    public static LevelResolver createProjectLevelResolver() {
        return LevelResolver.create()
                .withLevel("validation.request.parameter.query.unexpected", ValidationReport.Level.IGNORE)
                .withLevel("validation.request.security.missing", ValidationReport.Level.IGNORE)
                .withLevel("validation.response.body.schema.additionalProperties", ValidationReport.Level.WARN)
                .build();
    }

    /**
     * 프로젝트 OpenAPI 스펙 파일로부터 {@link OpenApiInteractionValidator}를 생성한다.
     *
     * <p>스펙 파일 경로는 {@link #SPEC_FILE_PATH}이며, $ref를 재귀적으로 해석하여
     * 멀티파일 스펙을 지원한다. LevelResolver는 {@link #createProjectLevelResolver()}로 생성된다.</p>
     *
     * @return OpenApiInteractionValidator 인스턴스
     * @throws IllegalStateException 스펙 파일이 존재하지 않는 경우
     */
    public static OpenApiInteractionValidator createValidator() {
        File specFile = new File(SPEC_FILE_PATH);
        if (!specFile.exists()) {
            throw new IllegalStateException(
                    "OpenAPI 스펙 파일을 찾을 수 없습니다: " + specFile.getAbsolutePath()
                            + " (프로젝트 루트의 openapi/openapi.yaml 파일이 존재하는지 확인하세요)"
            );
        }
        log.debug("OpenAPI 스펙 파일 로딩: {}", specFile.getAbsolutePath());
        return OpenApiInteractionValidator
                .createForSpecificationUrl(specFile.toURI().toString())
                .withLevelResolver(createProjectLevelResolver())
                .build();
    }
}
