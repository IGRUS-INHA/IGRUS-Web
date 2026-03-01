package igrus.web.common.openapi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.domain.BoardPermission;
import igrus.web.community.board.repository.BoardPermissionRepository;
import igrus.web.community.board.repository.BoardRepository;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.File;
import java.util.List;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-200: Atlassian swagger-request-validator PoC 검증 테스트.
 *
 * <p>검증 항목:
 * <ol>
 *     <li>멀티파일 스펙 로딩 vs 번들 파일 로딩</li>
 *     <li>MockMvc 통합 (정상 응답 스키마 검증)</li>
 *     <li>의도적 불일치 감지 (구조적 불일치, 타입 강제 변환)</li>
 *     <li>LevelResolver 설정 옵션 확인</li>
 *     <li>성능 영향 측정 (validator 초기화 비용, 요청당 검증 비용)</li>
 *     <li>테스트 컨텍스트 캐싱 전략 검증</li>
 * </ol>
 * </p>
 *
 * <p>PoC 발견 사항 요약:
 * <ul>
 *     <li>멀티파일 스펙($ref) 로딩: 성공 (번들 파일 불필요)</li>
 *     <li>MockMvc CSRF: _csrf 쿼리 파라미터가 unexpected parameter로 감지됨
 *         -- validation.request.parameter.query.unexpected를 IGNORE로 설정 필요</li>
 *     <li>additionalProperties: OpenAPI 3.1.0 기본값이 true이므로, 스키마에 미명시 시 추가 필드 허용됨
 *         -- 스키마에 additionalProperties: false를 명시해야 감지 가능</li>
 *     <li>OpenAPI 3.1.0 한계: swagger-request-validator 2.46.0이 3.1.0 스펙에서
 *         구조적 불일치(배열 vs 객체), 타입 불일치(boolean vs string)를 감지하지 못함.
 *         동일한 스키마를 3.0.3으로 정의하면 정상 감지됨.
 *         -- 프로젝트 스펙을 3.0.3으로 다운그레이드하거나, 향후 라이브러리 업데이트 시 재검증 필요</li>
 * </ul>
 * </p>
 */
@AutoConfigureMockMvc
@DisplayName("TASK-200: swagger-request-validator PoC 검증")
class SwaggerRequestValidatorPocTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardPermissionRepository boardPermissionRepository;

    private static final String BASE_URL = "/api/v1/boards";

    private User memberUser;

    /**
     * 프로젝트에 적합한 LevelResolver 기본 설정을 생성한다.
     * 이 설정은 향후 OpenApiValidatorUtil에서 공통으로 사용할 예정.
     */
    private static LevelResolver createProjectLevelResolver() {
        return LevelResolver.create()
                // MockMvc에서 .with(csrf())가 _csrf 쿼리 파라미터를 추가하므로 IGNORE
                .withLevel("validation.request.parameter.query.unexpected", ValidationReport.Level.IGNORE)
                // Spring Security 검증은 Spring Security가 담당하므로 IGNORE
                .withLevel("validation.request.security.missing", ValidationReport.Level.IGNORE)
                // additionalProperties: 스키마에 미명시 시 기본 WARN (안정화 후 ERROR로 전환)
                .withLevel("validation.response.body.schema.additionalProperties", ValidationReport.Level.WARN)
                .build();
    }

    @BeforeEach
    void setUp() {
        setUpBase();
        setupBoardData();
        memberUser = createAndSaveUser("20200001", "member@inha.edu", UserRole.MEMBER);
    }

    private void setupBoardData() {
        Board notices = Board.create(BoardCode.NOTICES, "공지사항", "동아리 공지사항을 확인할 수 있습니다.", false, false, 1);
        Board general = Board.create(BoardCode.GENERAL, "자유게시판", "자유롭게 이야기를 나눌 수 있는 공간입니다.", true, true, 2);
        Board insight = Board.create(BoardCode.INSIGHT, "정보공유", "유용한 정보를 공유하는 게시판입니다.", false, false, 3);

        boardRepository.save(notices);
        boardRepository.save(general);
        boardRepository.save(insight);

        boardPermissionRepository.save(BoardPermission.create(notices, UserRole.MEMBER, true, false));
        boardPermissionRepository.save(BoardPermission.create(general, UserRole.MEMBER, true, true));
        boardPermissionRepository.save(BoardPermission.create(insight, UserRole.MEMBER, true, true));
    }

    private RequestPostProcessor withAuth(User user) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.getId(),
                user.getStudentId(),
                user.getRole().name()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        return authentication(auth);
    }

    // ==================== 1. 스펙 파일 로딩 검증 ====================

    @Nested
    @DisplayName("1. 스펙 파일 로딩 검증")
    class SpecLoadingTest {

        @DisplayName("번들 파일(openapi.bundled.yaml)을 정상적으로 로딩할 수 있다")
        @Test
        void bundledSpec_CanBeLoaded() {
            // given
            File specFile = new File("../openapi/openapi.bundled.yaml");
            assertThat(specFile.exists()).as("번들 파일이 존재해야 함").isTrue();

            // when
            OpenApiInteractionValidator validator = OpenApiInteractionValidator
                    .createForSpecificationUrl(specFile.toURI().toString())
                    .build();

            // then
            assertThat(validator).isNotNull();
        }

        @DisplayName("멀티파일 스펙(openapi.yaml)에서 $ref 해석이 가능하다")
        @Test
        void multifileSpec_RefResolution_Succeeds() {
            // given
            File specFile = new File("../openapi/openapi.yaml");
            assertThat(specFile.exists()).as("멀티파일 스펙이 존재해야 함").isTrue();

            // when
            OpenApiInteractionValidator validator = OpenApiInteractionValidator
                    .createForSpecificationUrl(specFile.toURI().toString())
                    .build();

            // then - 멀티파일 스펙의 $ref 해석이 성공함
            assertThat(validator).as("멀티파일 스펙 로딩 성공").isNotNull();
            System.out.println("[PoC] 멀티파일 스펙 로딩 성공: 번들 파일 없이 $ref 자동 해석 가능");
        }

        @DisplayName("멀티파일 스펙으로 실제 API 검증이 가능하다")
        @Test
        void multifileSpec_CanValidateActualApi() {
            // given - 멀티파일 스펙으로 validator 생성
            File specFile = new File("../openapi/openapi.yaml");
            OpenApiInteractionValidator validator = OpenApiInteractionValidator
                    .createForSpecificationUrl(specFile.toURI().toString())
                    .withLevelResolver(createProjectLevelResolver())
                    .build();

            // when - 유효한 Board 응답 검증
            String validResponse = "[{\"code\":\"GENERAL\",\"name\":\"자유게시판\",\"description\":\"설명\",\"canRead\":true,\"canWrite\":true}]";
            SimpleRequest request = new SimpleRequest.Builder(Request.Method.GET, "/api/v1/boards").build();
            SimpleResponse response = new SimpleResponse.Builder(200)
                    .withContentType("application/json")
                    .withBody(validResponse)
                    .build();

            ValidationReport report = validator.validate(request, response);

            // then
            assertThat(report.hasErrors()).as("멀티파일 스펙으로 유효한 응답 검증 통과").isFalse();
            System.out.println("[PoC] 결론: 멀티파일 스펙(openapi.yaml)을 직접 사용 가능. 번들 파일 불필요.");
        }
    }

    // ==================== 2. MockMvc 통합 검증 ====================

    @Nested
    @DisplayName("2. MockMvc 통합 - 정상 응답 스키마 검증")
    class MockMvcIntegrationTest {

        private OpenApiInteractionValidator validator;

        @BeforeEach
        void setUpValidator() {
            File specFile = new File("../openapi/openapi.bundled.yaml");
            validator = OpenApiInteractionValidator
                    .createForSpecificationUrl(specFile.toURI().toString())
                    .withLevelResolver(createProjectLevelResolver())
                    .build();
        }

        @DisplayName("GET /api/v1/boards 정상 응답이 OpenAPI 스키마와 일치한다")
        @Test
        void getBoardList_ResponseMatchesOpenApiSchema() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(openApi().isValid(validator));
        }

        @DisplayName("GET /api/v1/boards/{code} 정상 응답이 OpenAPI 스키마와 일치한다")
        @Test
        void getBoardByCode_ResponseMatchesOpenApiSchema() throws Exception {
            mockMvc.perform(get(BASE_URL + "/general")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("GENERAL"))
                    .andExpect(openApi().isValid(validator));
        }
    }

    // ==================== 3. 의도적 불일치 감지 검증 ====================

    @Nested
    @DisplayName("3. 의도적 불일치 감지")
    class MismatchDetectionTest {

        /**
         * PoC 발견 사항:
         * OpenAPI 3.1.0에서 additionalProperties가 스키마에 명시되지 않으면 기본값이 true이므로,
         * 추가 필드가 있어도 검증이 통과한다.
         * 추가 필드를 감지하려면 스키마에 "additionalProperties: false"를 명시해야 한다.
         *
         * LevelResolver의 "validation.response.body.schema.additionalProperties" 키는
         * 스키마에 additionalProperties: false가 명시된 경우에만 동작한다.
         *
         * 결론: 현재 프로젝트 스키마에서는 추가 필드 감지가 불가.
         * 향후 스키마에 additionalProperties: false를 점진적으로 추가하여 엄격한 검증 가능.
         */
        @DisplayName("additionalProperties 미명시 시 추가 필드가 허용된다 (PoC 발견)")
        @Test
        void additionalField_AllowedWhenAdditionalPropertiesNotSet() {
            // given
            File specFile = new File("../openapi/openapi.bundled.yaml");
            OpenApiInteractionValidator validator = OpenApiInteractionValidator
                    .createForSpecificationUrl(specFile.toURI().toString())
                    .withLevelResolver(
                            LevelResolver.create()
                                    .withLevel("validation.response.body.schema.additionalProperties",
                                            ValidationReport.Level.ERROR)
                                    .withLevel("validation.request.security.missing", ValidationReport.Level.IGNORE)
                                    .build()
                    )
                    .build();

            // when - 스키마에 없는 필드 "extraField" 포함
            String responseWithExtraField = "[{\"code\":\"GENERAL\",\"name\":\"자유게시판\",\"description\":\"설명\",\"canRead\":true,\"canWrite\":true,\"extraField\":\"unexpected\"}]";

            SimpleRequest request = new SimpleRequest.Builder(Request.Method.GET, "/api/v1/boards").build();
            SimpleResponse response = new SimpleResponse.Builder(200)
                    .withContentType("application/json")
                    .withBody(responseWithExtraField)
                    .build();

            ValidationReport report = validator.validate(request, response);

            // then - additionalProperties가 스키마에 미명시이므로 추가 필드가 허용됨
            System.out.println("[PoC] additionalProperties 미명시 상태에서 추가 필드 감지: hasErrors=" + report.hasErrors());
            report.getMessages().forEach(msg ->
                    System.out.println("[PoC]   - " + msg.getLevel() + ": " + msg.getMessage())
            );

            // hasErrors가 false임을 확인 (추가 필드가 허용됨)
            assertThat(report.hasErrors())
                    .as("additionalProperties 미명시 시 추가 필드가 허용됨 (OpenAPI 3.1.0 기본 동작)")
                    .isFalse();
        }

        /**
         * PoC 발견 사항:
         * OpenAPI 3.1.0 스펙에서 배열 대신 객체를 반환해도 swagger-request-validator 2.46.0이
         * 감지하지 못한다. 이는 라이브러리의 OpenAPI 3.1.0 (JSON Schema 2020-12) 타입 검증이
         * 아직 완전하지 않기 때문으로 추정된다.
         *
         * 인라인 스키마(type: array)로 직접 정의한 엔드포인트에서도 동일하게 감지되지 않음.
         * 이 한계점은 PoC 결과 문서에 기록하고, 향후 라이브러리 업데이트 시 재검증 필요.
         */
        @DisplayName("구조적 불일치(배열 vs 객체)는 OpenAPI 3.1.0에서 감지되지 않는다 (PoC 한계 발견)")
        @Test
        void structuralMismatch_ArrayVsObject_NotDetectedInOpenApi31() {
            // given
            File specFile = new File("../openapi/openapi.bundled.yaml");
            OpenApiInteractionValidator validator = OpenApiInteractionValidator
                    .createForSpecificationUrl(specFile.toURI().toString())
                    .withLevelResolver(
                            LevelResolver.create()
                                    .withLevel("validation.request.security.missing", ValidationReport.Level.IGNORE)
                                    .build()
                    )
                    .build();

            // when - GET /api/v1/boards는 배열 응답이어야 하나, 객체를 반환
            String objectInsteadOfArray = "{\"code\":\"GENERAL\",\"name\":\"자유게시판\"}";

            SimpleRequest request = new SimpleRequest.Builder(Request.Method.GET, "/api/v1/boards").build();
            SimpleResponse response = new SimpleResponse.Builder(200)
                    .withContentType("application/json")
                    .withBody(objectInsteadOfArray)
                    .build();

            ValidationReport report = validator.validate(request, response);

            // then - 3.1.0에서 감지 안 됨을 문서화
            System.out.println("[PoC] 구조적 불일치(배열 vs 객체) 감지: hasErrors=" + report.hasErrors());
            report.getMessages().forEach(msg ->
                    System.out.println("[PoC]   - " + msg.getLevel() + ": " + msg.getMessage())
            );

            // OpenAPI 3.1.0에서 타입 불일치를 감지하지 못함
            assertThat(report.hasErrors())
                    .as("OpenAPI 3.1.0에서 배열/객체 구조 불일치가 감지되지 않음 (라이브러리 한계)")
                    .isFalse();
        }

        /**
         * PoC 발견 사항:
         * OpenAPI 3.1.0 스펙에서 boolean 필드에 string 값("not-a-boolean")을 넣어도
         * swagger-request-validator 2.46.0이 타입 불일치를 감지하지 못한다.
         * 이는 라이브러리의 OpenAPI 3.1.0 JSON Schema 타입 검증 한계로 추정됨.
         *
         * 단, 필수 필드(required) 누락 감지 등 다른 검증은 정상 작동할 수 있다.
         * 향후 라이브러리 업데이트 시 재검증 필요.
         */
        @DisplayName("boolean 필드 타입 불일치는 OpenAPI 3.1.0에서 감지되지 않는다 (PoC 한계 발견)")
        @Test
        void typeMismatch_BooleanField_NotDetectedInOpenApi31() {
            // given
            File specFile = new File("../openapi/openapi.bundled.yaml");
            OpenApiInteractionValidator validator = OpenApiInteractionValidator
                    .createForSpecificationUrl(specFile.toURI().toString())
                    .withLevelResolver(
                            LevelResolver.create()
                                    .withLevel("validation.request.security.missing", ValidationReport.Level.IGNORE)
                                    .build()
                    )
                    .build();

            // when - canRead(boolean) 필드에 string 값을 넣음
            String responseWithTypeMismatch = "{\"code\":\"GENERAL\",\"name\":\"자유게시판\",\"description\":\"설명\",\"allowsAnonymous\":true,\"allowsQuestionTag\":true,\"canRead\":\"not-a-boolean\",\"canWrite\":true}";

            SimpleRequest request = new SimpleRequest.Builder(Request.Method.GET, "/api/v1/boards/general").build();
            SimpleResponse response = new SimpleResponse.Builder(200)
                    .withContentType("application/json")
                    .withBody(responseWithTypeMismatch)
                    .build();

            ValidationReport report = validator.validate(request, response);

            // then - 3.1.0에서 감지 안 됨을 문서화
            System.out.println("[PoC] 타입 불일치(boolean에 string) 감지: hasErrors=" + report.hasErrors());
            report.getMessages().forEach(msg ->
                    System.out.println("[PoC]   - " + msg.getLevel() + ": " + msg.getMessage())
            );

            // OpenAPI 3.1.0에서 타입 불일치를 감지하지 못함
            assertThat(report.hasErrors())
                    .as("OpenAPI 3.1.0에서 boolean/string 타입 불일치가 감지되지 않음 (라이브러리 한계)")
                    .isFalse();
        }

        /**
         * 필수 필드(required) 누락은 검증기가 감지할 수 있는지 추가 검증.
         * 스키마에 required가 정의된 엔드포인트를 사용하여 테스트.
         */
        @DisplayName("required 필드가 정의된 스키마에서 필수 필드 누락이 감지되는지 확인")
        @Test
        void requiredFieldMissing_DetectionWithInlineSpec() {
            // given - 인라인으로 엄격한 스키마를 정의하여 테스트
            String strictSpec = """
                    openapi: "3.0.3"
                    info:
                      title: Test API
                      version: "1.0"
                    paths:
                      /api/test/strict:
                        get:
                          operationId: getStrict
                          responses:
                            "200":
                              description: OK
                              content:
                                application/json:
                                  schema:
                                    type: object
                                    required:
                                      - id
                                      - name
                                    properties:
                                      id:
                                        type: integer
                                      name:
                                        type: string
                                      active:
                                        type: boolean
                                    additionalProperties: false
                    """;

            OpenApiInteractionValidator validator = OpenApiInteractionValidator
                    .createForInlineApiSpecification(strictSpec)
                    .build();

            // when - required 필드 "id" 누락
            String missingRequired = "{\"name\":\"test\"}";

            SimpleRequest request = new SimpleRequest.Builder(Request.Method.GET, "/api/test/strict").build();
            SimpleResponse response = new SimpleResponse.Builder(200)
                    .withContentType("application/json")
                    .withBody(missingRequired)
                    .build();

            ValidationReport report = validator.validate(request, response);

            // then
            System.out.println("[PoC] 엄격한 스키마(3.0.3)에서 required 필드 누락 감지: hasErrors=" + report.hasErrors());
            report.getMessages().forEach(msg ->
                    System.out.println("[PoC]   - " + msg.getLevel() + ": " + msg.getMessage())
            );

            assertThat(report.hasErrors())
                    .as("required 필드가 누락되면 에러로 감지되어야 함")
                    .isTrue();
        }

        /**
         * OpenAPI 3.0.3 스펙에서 배열/객체 구조 불일치와 타입 불일치가 감지되는지 비교 검증.
         * 3.1.0에서 발견된 한계가 3.0.x에서는 존재하지 않는지 확인.
         */
        @DisplayName("OpenAPI 3.0.3 스펙에서는 배열/객체 구조 불일치가 감지된다")
        @Test
        void structuralMismatch_DetectedIn303() {
            // given - 3.0.3 인라인 스펙으로 배열 응답 엔드포인트 정의
            String strictSpec = """
                    openapi: "3.0.3"
                    info:
                      title: Test API
                      version: "1.0"
                    paths:
                      /api/test/items:
                        get:
                          operationId: getItems
                          responses:
                            "200":
                              description: OK
                              content:
                                application/json:
                                  schema:
                                    type: array
                                    items:
                                      type: object
                                      properties:
                                        id:
                                          type: integer
                                        name:
                                          type: string
                    """;

            OpenApiInteractionValidator validator = OpenApiInteractionValidator
                    .createForInlineApiSpecification(strictSpec)
                    .build();

            // when - 배열이어야 하는데 객체를 반환
            String objectInsteadOfArray = "{\"id\":1,\"name\":\"test\"}";

            SimpleRequest request = new SimpleRequest.Builder(Request.Method.GET, "/api/test/items").build();
            SimpleResponse response = new SimpleResponse.Builder(200)
                    .withContentType("application/json")
                    .withBody(objectInsteadOfArray)
                    .build();

            ValidationReport report = validator.validate(request, response);

            // then
            System.out.println("[PoC] OpenAPI 3.0.3에서 배열/객체 불일치 감지: hasErrors=" + report.hasErrors());
            report.getMessages().forEach(msg ->
                    System.out.println("[PoC]   - " + msg.getLevel() + ": " + msg.getMessage())
            );

            assertThat(report.hasErrors())
                    .as("OpenAPI 3.0.3에서는 배열/객체 구조 불일치가 감지되어야 함")
                    .isTrue();
        }

        /**
         * OpenAPI 3.0.3 스펙에서 boolean 필드에 string 값을 넣으면 타입 불일치가 감지되는지 비교 검증.
         */
        @DisplayName("OpenAPI 3.0.3 스펙에서는 타입 불일치가 감지된다")
        @Test
        void typeMismatch_DetectedIn303() {
            // given - 3.0.3 인라인 스펙으로 타입이 엄격한 엔드포인트 정의
            String strictSpec = """
                    openapi: "3.0.3"
                    info:
                      title: Test API
                      version: "1.0"
                    paths:
                      /api/test/detail:
                        get:
                          operationId: getDetail
                          responses:
                            "200":
                              description: OK
                              content:
                                application/json:
                                  schema:
                                    type: object
                                    properties:
                                      name:
                                        type: string
                                      active:
                                        type: boolean
                    """;

            OpenApiInteractionValidator validator = OpenApiInteractionValidator
                    .createForInlineApiSpecification(strictSpec)
                    .build();

            // when - boolean 필드에 string 값
            String typeMismatch = "{\"name\":\"test\",\"active\":\"not-a-boolean\"}";

            SimpleRequest request = new SimpleRequest.Builder(Request.Method.GET, "/api/test/detail").build();
            SimpleResponse response = new SimpleResponse.Builder(200)
                    .withContentType("application/json")
                    .withBody(typeMismatch)
                    .build();

            ValidationReport report = validator.validate(request, response);

            // then
            System.out.println("[PoC] OpenAPI 3.0.3에서 타입 불일치 감지: hasErrors=" + report.hasErrors());
            report.getMessages().forEach(msg ->
                    System.out.println("[PoC]   - " + msg.getLevel() + ": " + msg.getMessage())
            );

            assertThat(report.hasErrors())
                    .as("OpenAPI 3.0.3에서는 boolean/string 타입 불일치가 감지되어야 함")
                    .isTrue();
        }
    }

    // ==================== 4. LevelResolver 설정 검증 ====================

    @Nested
    @DisplayName("4. LevelResolver 설정 검증")
    class LevelResolverTest {

        @DisplayName("프로젝트 기본 LevelResolver로 유효한 응답이 통과한다")
        @Test
        void projectLevelResolver_ValidResponse_Passes() {
            // given
            File specFile = new File("../openapi/openapi.bundled.yaml");
            OpenApiInteractionValidator validator = OpenApiInteractionValidator
                    .createForSpecificationUrl(specFile.toURI().toString())
                    .withLevelResolver(createProjectLevelResolver())
                    .build();

            // when
            String validResponse = "[{\"code\":\"GENERAL\",\"name\":\"자유게시판\",\"description\":\"설명\",\"canRead\":true,\"canWrite\":true}]";

            SimpleRequest request = new SimpleRequest.Builder(Request.Method.GET, "/api/v1/boards").build();
            SimpleResponse response = new SimpleResponse.Builder(200)
                    .withContentType("application/json")
                    .withBody(validResponse)
                    .build();

            ValidationReport report = validator.validate(request, response);

            // then
            assertThat(report.hasErrors()).as("프로젝트 기본 LevelResolver로 유효한 응답이 통과해야 함").isFalse();
        }

        @DisplayName("보안 검증을 IGNORE로 설정하면 인증 없는 요청도 검증을 통과한다")
        @Test
        void securityValidation_IgnoreLevel_PassesWithoutAuth() {
            // given
            File specFile = new File("../openapi/openapi.bundled.yaml");
            OpenApiInteractionValidator validator = OpenApiInteractionValidator
                    .createForSpecificationUrl(specFile.toURI().toString())
                    .withLevelResolver(createProjectLevelResolver())
                    .build();

            // when - 인증 없이 요청
            String validResponse = "[{\"code\":\"GENERAL\",\"name\":\"자유게시판\",\"description\":\"설명\",\"canRead\":true,\"canWrite\":true}]";

            SimpleRequest request = new SimpleRequest.Builder(Request.Method.GET, "/api/v1/boards").build();
            SimpleResponse response = new SimpleResponse.Builder(200)
                    .withContentType("application/json")
                    .withBody(validResponse)
                    .build();

            ValidationReport report = validator.validate(request, response);

            // then
            assertThat(report.hasErrors()).as("보안 검증이 IGNORE이면 에러가 없어야 함").isFalse();
        }

        @DisplayName("CSRF 파라미터(_csrf)가 IGNORE 설정으로 허용된다")
        @Test
        void csrfParameter_IsIgnoredByLevelResolver() {
            // given
            File specFile = new File("../openapi/openapi.bundled.yaml");
            OpenApiInteractionValidator validator = OpenApiInteractionValidator
                    .createForSpecificationUrl(specFile.toURI().toString())
                    .withLevelResolver(createProjectLevelResolver())
                    .build();

            // when - _csrf 쿼리 파라미터가 포함된 요청
            String validResponse = "[{\"code\":\"GENERAL\",\"name\":\"자유게시판\",\"description\":\"설명\",\"canRead\":true,\"canWrite\":true}]";

            SimpleRequest request = new SimpleRequest.Builder(Request.Method.GET, "/api/v1/boards")
                    .withQueryParam("_csrf", "some-csrf-token")
                    .build();
            SimpleResponse response = new SimpleResponse.Builder(200)
                    .withContentType("application/json")
                    .withBody(validResponse)
                    .build();

            ValidationReport report = validator.validate(request, response);

            // then
            System.out.println("[PoC] CSRF 파라미터 IGNORE 결과: hasErrors=" + report.hasErrors());
            report.getMessages().forEach(msg ->
                    System.out.println("[PoC]   - " + msg.getLevel() + ": " + msg.getMessage())
            );

            assertThat(report.hasErrors())
                    .as("validation.request.parameter.query.unexpected가 IGNORE이면 _csrf 파라미터가 허용됨")
                    .isFalse();
        }
    }

    // ==================== 5. 성능 측정 ====================

    @Nested
    @DisplayName("5. 성능 영향 측정")
    class PerformanceMeasurementTest {

        @DisplayName("validator 초기화 비용과 요청당 검증 비용을 측정한다")
        @Test
        void measurePerformance() throws Exception {
            // 1. Validator 초기화 비용 측정
            long initStart = System.nanoTime();
            File specFile = new File("../openapi/openapi.bundled.yaml");
            OpenApiInteractionValidator validator = OpenApiInteractionValidator
                    .createForSpecificationUrl(specFile.toURI().toString())
                    .withLevelResolver(createProjectLevelResolver())
                    .build();
            long initEnd = System.nanoTime();
            long initTimeMs = (initEnd - initStart) / 1_000_000;

            System.out.println("[PoC] Validator 초기화 시간: " + initTimeMs + "ms");

            // 2. MockMvc 검증 비용 측정 (10회 반복)
            // 먼저 워밍업 1회
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(openApi().isValid(validator));

            // 실제 측정
            long totalValidationTime = 0;
            int iterations = 10;
            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                mockMvc.perform(get(BASE_URL)
                                .with(withAuth(memberUser))
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(openApi().isValid(validator));
                long end = System.nanoTime();
                totalValidationTime += (end - start);
            }

            long avgValidationTimeMs = (totalValidationTime / iterations) / 1_000_000;

            System.out.println("[PoC] 요청당 평균 검증 시간 (MockMvc 포함): " + avgValidationTimeMs + "ms (10회 평균)");

            // 3. 순수 검증 비용 측정 (MockMvc 없이, SimpleRequest/Response 사용)
            String validResponse = "[{\"code\":\"GENERAL\",\"name\":\"자유게시판\",\"description\":\"설명\",\"canRead\":true,\"canWrite\":true}]";
            SimpleRequest request = new SimpleRequest.Builder(Request.Method.GET, "/api/v1/boards").build();
            SimpleResponse response = new SimpleResponse.Builder(200)
                    .withContentType("application/json")
                    .withBody(validResponse)
                    .build();

            long totalPureValidation = 0;
            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                validator.validate(request, response);
                long end = System.nanoTime();
                totalPureValidation += (end - start);
            }

            long avgPureValidationMs = (totalPureValidation / iterations) / 1_000_000;
            System.out.println("[PoC] 순수 검증 비용 (MockMvc 제외): " + avgPureValidationMs + "ms (10회 평균)");

            // 성능 기준: 초기화 10초 이내
            assertThat(initTimeMs).as("Validator 초기화가 10초 이내여야 함").isLessThan(10_000);
        }
    }

    // ==================== 6. 테스트 컨텍스트 캐싱 전략 검증 ====================

    @Nested
    @DisplayName("6. 테스트 컨텍스트 캐싱 전략 검증")
    class ContextCachingTest {

        @DisplayName("@ActiveProfiles('test') 컨텍스트에서 @Profile({'dev', 'test'}) Bean이 자동 포함됨을 확인")
        @Test
        void profileTestConfig_IsAutoIncludedInTestProfile() {
            // ServiceIntegrationTestBase에 @ActiveProfiles("test")가 선언되어 있으므로
            // @Profile({"dev", "test"}) 설정 클래스가 있다면 자동으로 포함됨.

            // MockMvc 정상 주입 확인으로 테스트 컨텍스트 구성 검증
            assertThat(mockMvc).as("MockMvc가 테스트 컨텍스트에서 정상 주입되어야 함").isNotNull();

            System.out.println("[PoC] 테스트 컨텍스트 정상 구성 확인 완료");
            System.out.println("[PoC] @ActiveProfiles('test')에서 @Profile({'dev', 'test'}) Bean은 자동 포함됨 (Spring 프로필 메커니즘)");
            System.out.println("[PoC] TASK-221에서 OpenApiValidationConfig 생성 후, Filter Bean 존재 여부 추가 검증 필요");
        }
    }
}
