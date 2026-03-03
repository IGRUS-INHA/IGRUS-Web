package igrus.web.common.openapi;

import igrus.web.common.config.OpenApiValidationConfig;
import igrus.web.common.config.TestExternalServiceConfig;
import igrus.web.common.config.TestPasswordEncoderConfig;
import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.domain.BoardPermission;
import igrus.web.community.board.repository.BoardPermissionRepository;
import igrus.web.community.board.repository.BoardRepository;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-222: OpenApiValidationFilter 동작 검증 통합 테스트.
 *
 * <p>Filter가 활성화된 상태에서의 동작을 검증한다.
 * {@code openapi.validation.filter.enabled=true}로 설정하여
 * {@link OpenApiValidationConfig}가 활성화되도록 한다.</p>
 *
 * <h3>컨텍스트 캐싱 규칙 위반 사유</h3>
 * <p>작업 계획(TASK-222)은 {@code ControllerIntegrationTestBase}를 상속하여
 * 기존 테스트 컨텍스트를 재사용하도록 규정하고 있으나, 이 테스트는 독립적인
 * Spring 컨텍스트를 사용한다. 그 이유는 다음과 같다:</p>
 * <ol>
 *     <li><b>Filter 활성화 필요</b>: 이 테스트의 핵심 목적은
 *         {@code openapi.validation.filter.enabled=true} 상태에서의 Filter 동작을
 *         검증하는 것이다. 기존 테스트 컨텍스트는 {@code application-test.yml}에서
 *         {@code openapi.validation.filter.enabled=false}로 설정되어 있어,
 *         기존 컨텍스트를 그대로 사용하면 Filter가 비활성화된 상태로 테스트가 실행된다.</li>
 *     <li><b>{@code @TestPropertySource} 불가피</b>: Filter를 활성화하려면
 *         프로퍼티 값을 오버라이드해야 하며, 이를 위해 {@code @TestPropertySource}가
 *         필수적이다. 이 어노테이션은 기존 컨텍스트와 다른 프로퍼티 조합을 생성하므로,
 *         Spring Test 프레임워크가 새로운 ApplicationContext를 생성한다.</li>
 *     <li><b>컨텍스트 캐싱 규칙 위반 인지</b>: {@code backend/src/test/CLAUDE.md} 3절의
 *         "서브클래스에서 {@code @ActiveProfiles} 추가 금지" 규칙을 위반하지 않으나,
 *         {@code @TestPropertySource} 사용으로 인해 새로운 컨텍스트가 생성되는 것은
 *         인지하고 있다. 이는 Filter 동작 검증이라는 테스트 목적상 불가피하며,
 *         기존 테스트의 컨텍스트 캐싱에는 영향을 주지 않는다
 *         (별도 컨텍스트가 추가로 생성될 뿐, 기존 컨텍스트가 무효화되지 않는다).</li>
 * </ol>
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>TC-221-01: test 프로필에서 OpenApiValidationFilter Bean 등록 확인</li>
 *     <li>TC-221-02: 정상 API 호출 시 검증 통과 확인</li>
 *     <li>TC-221-03: 스키마 불일치 시 로그 경고 확인 (검증 실패 감지 동작)</li>
 *     <li>TC-222-01: Filter 활성화 상태에서 정상 요청/응답 흐름 검증</li>
 *     <li>TC-222-02: Filter 활성화 상태에서 다수 엔드포인트 순차 호출</li>
 * </ul>
 * </p>
 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({TestPasswordEncoderConfig.class, TestExternalServiceConfig.class})
@TestPropertySource(properties = "openapi.validation.filter.enabled=true")
@DisplayName("TASK-222: OpenApiValidationFilter 동작 검증")
class OpenApiValidationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardPermissionRepository boardPermissionRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private User memberUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        cleanupDatabase();
        setupTestData();
    }

    /**
     * ServiceIntegrationTestBase.cleanupDatabase()와 동일한 테이블 목록 사용.
     * 이 테스트는 독립 컨텍스트를 위해 베이스 클래스를 상속하지 않으므로
     * cleanupDatabase 로직을 직접 포함한다.
     */
    private void cleanupDatabase() {
        transactionTemplate.execute(status -> {
            // Phase 1: Inquiry 계층
            entityManager.createNativeQuery("DELETE FROM inquiry_status_change_histories").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM inquiry_memos").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM inquiry_replies").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM inquiry_attachments").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM guest_inquiries").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM member_inquiries").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM inquiries").executeUpdate();

            // Phase 1.3: Survey 계층
            entityManager.createNativeQuery("DELETE FROM survey_answers").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM survey_responses").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM survey_question_rows").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM survey_question_options").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM survey_questions").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM surveys").executeUpdate();

            // Phase 1.5: Event 계층
            entityManager.createNativeQuery("DELETE FROM event_status_change_histories").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM event_registrations").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM events").executeUpdate();

            // Phase 2: Community 계층
            entityManager.createNativeQuery("DELETE FROM comment_reports").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM comment_likes").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM comments").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM likes").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM bookmarks").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM post_views").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM pinned_posts").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM post_images").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM posts").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM board_permissions").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM boards").executeUpdate();

            // Phase 2.5: Semester Members
            entityManager.createNativeQuery("DELETE FROM semester_members").executeUpdate();

            // Phase 3: User 종속 테이블
            entityManager.createNativeQuery("DELETE FROM account_status_change_histories").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM withdrawal_logs").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM login_histories").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM refresh_tokens").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM password_reset_tokens").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM password_credentials").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM privacy_consents").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM associate_decisions").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM user_role_histories").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM user_suspensions").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM user_positions").executeUpdate();

            // Phase 3: 부모 테이블
            entityManager.createNativeQuery("DELETE FROM users").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM positions").executeUpdate();

            // Phase 3.5: Storage
            entityManager.createNativeQuery("DELETE FROM file_metadata").executeUpdate();

            // Phase 4: 독립 테이블
            entityManager.createNativeQuery("DELETE FROM email_verifications").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM login_attempts").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM temp_student_id_sequences").executeUpdate();

            entityManager.flush();
            entityManager.clear();
            return null;
        });
    }

    private void setupTestData() {
        transactionTemplate.execute(status -> {
            memberUser = User.create(
                    "20200001", "테스트유저", "member@inha.edu",
                    "010-2020-0001", "컴퓨터공학과", "테스트 동기",
                    List.of(), Gender.MALE, 1, EnrollmentStatus.ENROLLED,
                    List.of(), null, null, null
            );
            memberUser.changeRole(UserRole.MEMBER);
            userRepository.save(memberUser);

            adminUser = User.create(
                    "20200002", "관리자", "admin@inha.edu",
                    "010-2020-0002", "컴퓨터공학과", "테스트 동기",
                    List.of(), Gender.MALE, 1, EnrollmentStatus.ENROLLED,
                    List.of(), null, null, null
            );
            adminUser.changeRole(UserRole.ADMIN);
            userRepository.save(adminUser);

            Board generalBoard = Board.create(
                    BoardCode.GENERAL, "자유게시판",
                    "자유롭게 이야기를 나눌 수 있는 공간입니다.", true, true, 2
            );
            boardRepository.save(generalBoard);

            boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.MEMBER, true, true));
            boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.OPERATOR, true, true));
            boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.ADMIN, true, true));

            return null;
        });
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

    @Nested
    @DisplayName("Filter Bean 등록 검증")
    class FilterBeanRegistrationTest {

        @DisplayName("TC-221-01: test 프로필에서 openapi.validation.filter.enabled=true일 때 " +
                "OpenApiValidationFilter Bean이 등록된다")
        @Test
        void filterBeanIsRegistered_WhenPropertyEnabled() {
            // when - Bean 이름으로 검증 (openApiValidationFilter는 OpenApiValidationConfig에서 등록)
            boolean filterExists = applicationContext.containsBean("openApiValidationFilter");

            // then
            assertThat(filterExists)
                    .as("openapi.validation.filter.enabled=true일 때 OpenApiValidationFilter Bean이 등록되어야 한다")
                    .isTrue();
        }

        @DisplayName("OpenApiValidationConfig Bean이 등록된다")
        @Test
        void configBeanIsRegistered_WhenPropertyEnabled() {
            // when
            boolean configExists = applicationContext.getBeanNamesForType(OpenApiValidationConfig.class).length > 0;

            // then
            assertThat(configExists)
                    .as("openapi.validation.filter.enabled=true일 때 OpenApiValidationConfig Bean이 등록되어야 한다")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Filter 활성화 상태에서 정상 요청/응답 검증")
    class FilterEnabledValidationTest {

        @DisplayName("TC-221-02, TC-222-01: 정상 API 호출 시 Filter를 통과하여 200 OK를 반환한다")
        @Test
        void normalApiCall_PassesThroughFilter_Returns200() throws Exception {
            // GET /api/v1/boards는 OpenAPI 스펙에 정의된 엔드포인트
            mockMvc.perform(get("/api/v1/boards")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].code").value("GENERAL"));
        }

        @DisplayName("GET /api/v1/boards/{code} 정상 호출 시 Filter를 통과한다")
        @Test
        void getBoardByCode_PassesThroughFilter_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/boards/general")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("GENERAL"))
                    .andExpect(jsonPath("$.name").value("자유게시판"));
        }
    }

    @Nested
    @DisplayName("스키마 불일치 감지 검증")
    class SchemaMismatchDetectionTest {

        @DisplayName("TC-221-03: 스펙에 정의된 엔드포인트에 필수 필드가 누락된 요청을 보내면 " +
                "LoggingValidationReportHandler가 WARN 로그를 남긴다")
        @Test
        void schemaMismatch_RequiredFieldMissing_LogsWarning(CapturedOutput capturedOutput) throws Exception {
            // given - POST /api/v1/boards/{code}/posts는 title, content가 required
            // 빈 JSON 바디 {}를 보내면 OpenAPI 요청 검증에서 필수 필드 누락이 감지된다.
            String emptyBody = "{}";

            // when - 요청을 보냄 (Bean Validation에 의해 400 반환 예상)
            mockMvc.perform(post("/api/v1/boards/general/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(emptyBody)
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print());

            // then - LoggingValidationReportHandler가 검증 실패/경고 로그를 남겼는지 확인
            // LoggingValidationReportHandler는 "[OpenAPI 요청 검증 실패]" 또는 "[OpenAPI 요청 검증 경고]"
            // 또는 "[OpenAPI 응답 검증 실패]" 또는 "[OpenAPI 응답 검증 경고]" 형식으로 로그를 남긴다.
            String output = capturedOutput.getAll();
            boolean hasValidationLog = output.contains("[OpenAPI 요청 검증 실패]")
                    || output.contains("[OpenAPI 요청 검증 경고]")
                    || output.contains("[OpenAPI 응답 검증 실패]")
                    || output.contains("[OpenAPI 응답 검증 경고]");

            assertThat(hasValidationLog)
                    .as("스키마 불일치 시 LoggingValidationReportHandler가 검증 실패/경고 로그를 남겨야 한다. " +
                            "캡처된 출력 중 관련 내용:\n" +
                            extractValidationLines(output))
                    .isTrue();
        }

        @DisplayName("정상 API 호출 시에는 검증 실패/경고 로그가 남지 않는다")
        @Test
        void normalApiCall_NoValidationWarningLog(CapturedOutput capturedOutput) throws Exception {
            // when - 정상적인 GET 요청
            mockMvc.perform(get("/api/v1/boards")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andExpect(status().isOk());

            // then - 검증 실패/경고 로그가 없어야 한다
            String output = capturedOutput.getAll();
            boolean hasValidationFailureLog = output.contains("[OpenAPI 요청 검증 실패]")
                    || output.contains("[OpenAPI 응답 검증 실패]");

            assertThat(hasValidationFailureLog)
                    .as("정상 API 호출 시에는 검증 실패 로그가 남지 않아야 한다")
                    .isFalse();
        }

        /**
         * 캡처된 출력에서 OpenAPI 검증 관련 라인만 추출한다.
         */
        private String extractValidationLines(String output) {
            StringBuilder sb = new StringBuilder();
            for (String line : output.split("\n")) {
                if (line.contains("[OpenAPI") || line.contains("검증")) {
                    sb.append(line).append("\n");
                }
            }
            return sb.length() > 0 ? sb.toString() : "(OpenAPI 검증 관련 로그 없음)";
        }
    }

    @Nested
    @DisplayName("다수 엔드포인트 순차 호출 검증")
    class MultiEndpointSequentialTest {

        @DisplayName("TC-222-02: 다수 엔드포인트를 순차 호출해도 Filter가 정상 동작한다")
        @Test
        void multipleEndpoints_SequentialCalls_AllPassFilter() throws Exception {
            // 1. GET /api/v1/boards
            mockMvc.perform(get("/api/v1/boards")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());

            // 2. GET /api/v1/boards/{code}
            mockMvc.perform(get("/api/v1/boards/general")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("GENERAL"));

            // 3. GET /api/v1/admin/dashboard (admin 전용)
            mockMvc.perform(get("/api/v1/admin/dashboard")
                            .with(withAuth(adminUser))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.todayPostCount").exists());

            // 4. 다시 boards 호출 (반복 호출 안정성 확인)
            mockMvc.perform(get("/api/v1/boards")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }
}
