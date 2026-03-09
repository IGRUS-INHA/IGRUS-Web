package igrus.web.event.integration;

import igrus.web.common.OpenApiValidatorUtil;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.event.repository.EventRepository;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyAccessLevel;
import igrus.web.survey.question.domain.SurveyQuestionType;
import igrus.web.survey.question.domain.TextSurveyQuestion;
import igrus.web.survey.question.repository.SurveyQuestionRepository;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.survey.response.repository.SurveyResponseRepository;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * EventRegistration 컨트롤러 통합 테스트.
 *
 * <p>Spring Security의 인증 필터와 서비스 계층의 권한 검증이
 * HTTP 요청/응답 수준에서 올바르게 동작하는지 검증합니다.</p>
 *
 * <p>또한 2xx 성공 응답이 OpenAPI 스키마와 일치하는지 검증합니다.</p>
 *
 * <p>테스트 케이스:</p>
 * <ul>
 *     <li>TC-212-02: EventRegistrationController GET /events/{id}/registrations 응답 스키마 검증</li>
 *     <li>INT-030: 비인증 사용자 행사 신청 → 401</li>
 *     <li>INT-031: ASSOCIATE가 행사 신청 → 403</li>
 *     <li>INT-032: MEMBER가 신청 승인 → 403</li>
 *     <li>INT-033: MEMBER가 신청 거절 → 403</li>
 *     <li>INT-034: MEMBER가 승인/거절 되돌리기 → 403</li>
 *     <li>INT-035: MEMBER가 신청자 목록 조회 → 403</li>
 * </ul>
 */
@AutoConfigureMockMvc
@DisplayName("EventRegistration 컨트롤러 통합 테스트")
class EventRegistrationControllerIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveyQuestionRepository surveyQuestionRepository;

    @Autowired
    private SurveyResponseRepository surveyResponseRepository;

    private User operator;
    private User member;
    private User associate;

    @BeforeEach
    void setUp() {
        setUpBase();
        transactionTemplate.execute(status -> {
            operator = createAndSaveUser("20230001", "operator@inha.edu", UserRole.OPERATOR);
            member = createAndSaveUser("20230002", "member@inha.edu", UserRole.MEMBER);
            associate = createAndSaveUser("20230003", "associate@inha.edu", UserRole.ASSOCIATE);
            return null;
        });
    }

    private String generateToken(User user) {
        return jwtTokenProvider.createAccessToken(user.getId(), user.getStudentId(), user.getRole().name());
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

    /**
     * 테스트용 선발제 행사를 생성하고 저장합니다.
     * 행사 상태: UPCOMING, 등록 상태: OPEN
     */
    private Event createAndSaveManualApproveEvent() {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator,
                    "선발제 행사",
                    "설명",
                    "장소",
                    now.plus(7, ChronoUnit.DAYS),
                    now.plus(8, ChronoUnit.DAYS),
                    now.minus(1, ChronoUnit.DAYS),
                    now.plus(6, ChronoUnit.DAYS),
                    10,
                    EventRegistrationType.MANUAL_APPROVE,
            null
            );
            event.publish();
            event.openRegistration();
            return eventRepository.save(event);
        });
    }

    /**
     * 테스트용 선발제 행사에 대기(WAITING) 상태 신청을 생성합니다.
     */
    private EventRegistration createAndSaveWaitingRegistration(Event event, User user) {
        return transactionTemplate.execute(status -> {
            EventRegistration registration = EventRegistration.create(event, user);
            return eventRegistrationRepository.save(registration);
        });
    }

    // ==================== OpenAPI 응답 스키마 검증 (TC-212-02) ====================

    @Nested
    @DisplayName("GET /api/v1/events/{eventId}/registrations - 신청자 목록 조회 (OpenAPI 스키마 검증)")
    class GetRegistrationListOpenApiValidationTest {

        @Test
        @DisplayName("[TC-212-02] OPERATOR가 신청자 목록 조회 시 페이지네이션 응답이 OpenAPI 스키마와 일치한다")
        void getRegistrationList_WithOperatorRole_ReturnsResponseMatchingOpenApiSpec() throws Exception {
            // given - 행사와 신청자를 미리 생성
            Event event = createAndSaveManualApproveEvent();
            createAndSaveWaitingRegistration(event, member);

            // when & then
            mockMvc.perform(get("/api/v1/events/" + event.getId() + "/registrations")
                            .with(withAuth(operator))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].registrationId").exists())
                    .andExpect(jsonPath("$.content[0].userName").exists())
                    .andExpect(jsonPath("$.content[0].status").exists())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        @Test
        @DisplayName("[TC-212-02] 신청자가 없는 행사의 신청자 목록 조회 시 빈 페이지 응답이 OpenAPI 스키마와 일치한다")
        void getRegistrationList_Empty_ReturnsResponseMatchingOpenApiSpec() throws Exception {
            // given - 행사만 생성 (신청자 없음)
            Event event = createAndSaveManualApproveEvent();

            // when & then
            mockMvc.perform(get("/api/v1/events/" + event.getId() + "/registrations")
                            .with(withAuth(operator))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.empty").value(true))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }
    }

    // ==================== INT-030 ====================

    @Test
    @DisplayName("[INT-030] 비인증 사용자 행사 신청 → 401")
    void registerEvent_Unauthenticated_Returns401() throws Exception {
        Event event = createAndSaveManualApproveEvent();

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== INT-031 ====================

    @Test
    @DisplayName("[INT-031] ASSOCIATE가 행사 신청 → 403")
    void registerEvent_AssociateRole_Returns403() throws Exception {
        Event event = createAndSaveManualApproveEvent();
        String token = generateToken(associate);

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ==================== INT-032 ====================

    @Test
    @DisplayName("[INT-032] MEMBER가 신청 승인 → 403")
    void approveRegistration_MemberRole_Returns403() throws Exception {
        Event event = createAndSaveManualApproveEvent();
        EventRegistration registration = createAndSaveWaitingRegistration(event, member);
        String token = generateToken(member);

        mockMvc.perform(post("/api/v1/registrations/" + registration.getId() + "/approve")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ==================== INT-033 ====================

    @Test
    @DisplayName("[INT-033] MEMBER가 신청 거절 → 403")
    void rejectRegistration_MemberRole_Returns403() throws Exception {
        Event event = createAndSaveManualApproveEvent();
        EventRegistration registration = createAndSaveWaitingRegistration(event, member);
        String token = generateToken(member);

        mockMvc.perform(post("/api/v1/registrations/" + registration.getId() + "/reject")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ==================== INT-034 ====================

    @Test
    @DisplayName("[INT-034] MEMBER가 승인/거절 되돌리기 → 403")
    void revertRegistration_MemberRole_Returns403() throws Exception {
        Event event = createAndSaveManualApproveEvent();
        EventRegistration registration = createAndSaveWaitingRegistration(event, member);
        String token = generateToken(member);

        mockMvc.perform(post("/api/v1/registrations/" + registration.getId() + "/revert")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ==================== INT-035 ====================

    @Test
    @DisplayName("[INT-035] MEMBER가 신청자 목록 조회 → 403")
    void getRegistrationList_MemberRole_Returns403() throws Exception {
        Event event = createAndSaveManualApproveEvent();
        String token = generateToken(member);

        mockMvc.perform(get("/api/v1/events/" + event.getId() + "/registrations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ==================== 설문 연결 행사 보안 테스트 ====================

    @Nested
    @DisplayName("설문 연결 행사 보안 테스트 (SEC-SEVT)")
    class SurveyEventSecurityTest {

        private Survey createAndSaveOpenSurvey() {
            return transactionTemplate.execute(status -> {
                Survey survey = Survey.create("보안 테스트 설문", "설명", SurveyAccessLevel.MEMBER, null);
                survey.publishAndOpen();
                Survey saved = surveyRepository.save(survey);

                TextSurveyQuestion question = TextSurveyQuestion.create(
                        saved, SurveyQuestionType.SHORT_ANSWER, "질문1", "설명", true, 1);
                surveyQuestionRepository.save(question);

                return saved;
            });
        }

        private Event createAndSaveSurveyLinkedEvent(Survey survey) {
            return transactionTemplate.execute(status -> {
                Instant now = Instant.now();
                Event event = Event.create(
                        operator, "설문 연동 행사", "설명", "장소",
                        now.plus(7, ChronoUnit.DAYS),
                        now.plus(8, ChronoUnit.DAYS),
                        now.minus(1, ChronoUnit.DAYS),
                        now.plus(6, ChronoUnit.DAYS),
                        10,
                        EventRegistrationType.AUTO_APPROVE,
                        survey
                );
                event.publish();
                event.openRegistration();
                return eventRepository.save(event);
            });
        }

        private Long getQuestionId(Survey survey) {
            return transactionTemplate.execute(status -> {
                Survey loaded = surveyRepository.findById(survey.getId()).orElseThrow();
                return loaded.getQuestions().get(0).getId();
            });
        }

        // ==================== TC-050 ====================

        @Test
        @DisplayName("[TC-050] ASSOCIATE가 설문 연결 행사 신청 시 403")
        void registerSurveyEvent_AssociateRole_Returns403() throws Exception {
            Survey survey = createAndSaveOpenSurvey();
            Long questionId = getQuestionId(survey);
            Event event = createAndSaveSurveyLinkedEvent(survey);
            String token = generateToken(associate);

            String requestBody = """
                    {
                        "surveyAnswers": [
                            {
                                "questionId": %d,
                                "textValue": "답변"
                            }
                        ]
                    }
                    """.formatted(questionId);

            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden());
        }

        // ==================== TC-051 ====================

        @Test
        @DisplayName("[TC-051] MEMBER가 설문 연결 포함 행사 생성 시 403")
        void createSurveyEvent_MemberRole_Returns403() throws Exception {
            Survey survey = createAndSaveOpenSurvey();
            String token = generateToken(member);

            Instant now = Instant.now();
            String requestBody = """
                    {
                        "title": "테스트 행사",
                        "description": "설명",
                        "location": "장소",
                        "eventStartAt": "%s",
                        "eventEndAt": "%s",
                        "registrationStartAt": "%s",
                        "registrationEndAt": "%s",
                        "capacity": 10,
                        "registrationType": "AUTO_APPROVE",
                        "surveyId": %d
                    }
                    """.formatted(
                    now.plus(7, ChronoUnit.DAYS).toString(),
                    now.plus(8, ChronoUnit.DAYS).toString(),
                    now.minus(1, ChronoUnit.DAYS).toString(),
                    now.plus(6, ChronoUnit.DAYS).toString(),
                    survey.getId()
            );

            mockMvc.perform(post("/api/v1/events")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden());
        }

        // ==================== TC-054 ====================

        @Test
        @DisplayName("[TC-054] 미인증 사용자가 설문 연결 행사 생성 시 401 + DB 변경 없음")
        void createSurveyEvent_Unauthenticated_Returns401AndNoDbChange() throws Exception {
            Survey survey = createAndSaveOpenSurvey();
            long eventCountBefore = eventRepository.count();

            Instant now = Instant.now();
            String requestBody = """
                    {
                        "title": "테스트 행사",
                        "description": "설명",
                        "location": "장소",
                        "eventStartAt": "%s",
                        "eventEndAt": "%s",
                        "registrationStartAt": "%s",
                        "registrationEndAt": "%s",
                        "capacity": 10,
                        "registrationType": "AUTO_APPROVE",
                        "surveyId": %d
                    }
                    """.formatted(
                    now.plus(7, ChronoUnit.DAYS).toString(),
                    now.plus(8, ChronoUnit.DAYS).toString(),
                    now.minus(1, ChronoUnit.DAYS).toString(),
                    now.plus(6, ChronoUnit.DAYS).toString(),
                    survey.getId()
            );

            mockMvc.perform(post("/api/v1/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnauthorized());

            // DB에 Event 레코드 생성 없음 확인
            long eventCountAfter = eventRepository.count();
            assertThat(eventCountAfter).isEqualTo(eventCountBefore);
        }

        // ==================== TC-055 ====================

        @Test
        @DisplayName("[TC-055] ASSOCIATE 행사 신청 실패 시 SurveyResponse 미존재 확인")
        void registerSurveyEvent_AssociateRole_NoSurveyResponseSaved() throws Exception {
            Survey survey = createAndSaveOpenSurvey();
            Long questionId = getQuestionId(survey);
            Event event = createAndSaveSurveyLinkedEvent(survey);
            String token = generateToken(associate);

            String requestBody = """
                    {
                        "surveyAnswers": [
                            {
                                "questionId": %d,
                                "textValue": "답변"
                            }
                        ]
                    }
                    """.formatted(questionId);

            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden());

            // SurveyResponse DB 레코드 없음 확인 (권한 검증이 설문 저장보다 선행)
            boolean hasResponse = surveyResponseRepository
                    .existsBySurveyIdAndUserId(survey.getId(), associate.getId());
            assertThat(hasResponse).isFalse();
        }

        // ==================== TC-052 ====================

        @Test
        @DisplayName("[TC-052] 설문 응답 미존재 상태에서 설문 연결 행사 신청 시 400")
        void registerSurveyEvent_NoSurveyResponse_Returns400() throws Exception {
            Survey survey = createAndSaveOpenSurvey();
            Event event = createAndSaveSurveyLinkedEvent(survey);

            // surveyAnswers 미포함 (기존 응답도 없음) -> SurveyResponseRequiredException -> 400
            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                            .with(withAuth(member))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }
}
