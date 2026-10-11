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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    // ==================== 신청 취소 요청 / 운영진 알림 ====================

    @Nested
    @DisplayName("신청 취소 요청 및 운영진 취소 요청 목록")
    class CancelRequestTest {

        private String cancelRequestUrl(Event event) {
            return "/api/v1/events/" + event.getId() + "/registrations/cancel-request";
        }

        private org.springframework.test.web.servlet.ResultActions requestCancel(Event event, User user, String body)
                throws Exception {
            return mockMvc.perform(post(cancelRequestUrl(event))
                    .with(withAuth(user))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));
        }

        @Test
        @DisplayName("[INT-050] 신청자가 취소 요청하면 200, 신청 상태는 유지되고 사유가 저장된다 (OpenAPI 스키마 검증)")
        void requestCancel_ByRegistrant_Returns200AndKeepsStatus() throws Exception {
            Event event = createAndSaveManualApproveEvent();
            EventRegistration registration = createAndSaveWaitingRegistration(event, member);

            requestCancel(event, member, "{\"reason\":\"개인 사정\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.registrationId").value(registration.getId()))
                    .andExpect(jsonPath("$.status").value("WAITING"))
                    .andExpect(jsonPath("$.isRegistered").value(true))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());

            EventRegistration saved = eventRegistrationRepository.findById(registration.getId()).orElseThrow();
            assertThat(saved.getCancelReason()).isEqualTo("개인 사정");
            assertThat(saved.getCancelRequestedAt()).isNotNull();
        }

        @Test
        @DisplayName("[INT-051] 이미 취소 요청한 신청에 다시 요청하면 409 EVENT_CANCEL_ALREADY_REQUESTED")
        void requestCancel_Twice_Returns409() throws Exception {
            Event event = createAndSaveManualApproveEvent();
            createAndSaveWaitingRegistration(event, member);
            requestCancel(event, member, "{\"reason\":\"개인 사정\"}").andExpect(status().isOk());

            requestCancel(event, member, "{\"reason\":\"다른 사유\"}")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("EVENT_CANCEL_ALREADY_REQUESTED"));
        }

        @Test
        @DisplayName("[INT-052] 사유가 없거나 공백뿐이거나 500자를 넘으면 400이고 요청이 저장되지 않는다")
        void requestCancel_InvalidReason_Returns400() throws Exception {
            Event event = createAndSaveManualApproveEvent();
            EventRegistration registration = createAndSaveWaitingRegistration(event, member);

            requestCancel(event, member, "{}").andExpect(status().isBadRequest());
            requestCancel(event, member, "{\"reason\":\"   \"}").andExpect(status().isBadRequest());
            requestCancel(event, member, "{\"reason\":\"" + "가".repeat(501) + "\"}")
                    .andExpect(status().isBadRequest());

            assertThat(eventRegistrationRepository.findById(registration.getId()).orElseThrow()
                    .getCancelRequestedAt()).isNull();
        }

        @Test
        @DisplayName("[INT-053] 500자 사유는 허용된다 (경계값)")
        void requestCancel_MaxLengthReason_Returns200() throws Exception {
            Event event = createAndSaveManualApproveEvent();
            createAndSaveWaitingRegistration(event, member);

            requestCancel(event, member, "{\"reason\":\"" + "가".repeat(500) + "\"}")
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("[INT-054] 신청하지 않은 사용자가 취소 요청하면 404")
        void requestCancel_NotRegistered_Returns404() throws Exception {
            Event event = createAndSaveManualApproveEvent();

            requestCancel(event, member, "{\"reason\":\"개인 사정\"}")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("[INT-055] 비인증 사용자가 취소 요청하면 401")
        void requestCancel_Unauthenticated_Returns401() throws Exception {
            Event event = createAndSaveManualApproveEvent();

            mockMvc.perform(post(cancelRequestUrl(event))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"개인 사정\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[INT-056] 신청자 본인 즉시 취소(DELETE)는 더 이상 지원하지 않는다 → 405, 신청 유지")
        void deleteRegistration_NoLongerSupported_Returns405() throws Exception {
            Event event = createAndSaveManualApproveEvent();
            EventRegistration registration = createAndSaveWaitingRegistration(event, member);

            mockMvc.perform(delete("/api/v1/events/" + event.getId() + "/registrations")
                            .with(withAuth(member))
                            .with(csrf()))
                    .andExpect(status().isMethodNotAllowed());

            assertThat(eventRegistrationRepository.findById(registration.getId()).orElseThrow().isCanceled())
                    .isFalse();
        }

        @Test
        @DisplayName("[INT-057] OPERATOR가 취소 요청 목록 조회 시 요청 항목이 반환되고, 취소 처리 후에도 CANCELED로 남는다 (OpenAPI 스키마 검증)")
        void getCancelRequestList_Operator_ReturnsRequestsBeforeAndAfterCancel() throws Exception {
            Event event = createAndSaveManualApproveEvent();
            EventRegistration registration = createAndSaveWaitingRegistration(event, member);
            createAndSaveWaitingRegistration(event, associate); // 취소 요청 없는 신청은 목록에 없어야 함
            requestCancel(event, member, "{\"reason\":\"개인 사정\"}").andExpect(status().isOk());

            // 처리 대기 중
            mockMvc.perform(get("/api/v1/registrations/cancel-requests")
                            .with(withAuth(operator)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].registrationId").value(registration.getId()))
                    .andExpect(jsonPath("$.content[0].eventId").value(event.getId()))
                    .andExpect(jsonPath("$.content[0].eventTitle").value("선발제 행사"))
                    .andExpect(jsonPath("$.content[0].userName").value(member.getName()))
                    .andExpect(jsonPath("$.content[0].studentId").value(member.getStudentId()))
                    .andExpect(jsonPath("$.content[0].isExternal").value(false))
                    .andExpect(jsonPath("$.content[0].cancelReason").value("개인 사정"))
                    .andExpect(jsonPath("$.content[0].cancelRequestedAt").exists())
                    .andExpect(jsonPath("$.content[0].status").value("WAITING"))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());

            // 운영진이 취소 처리
            mockMvc.perform(post("/api/v1/registrations/" + registration.getId() + "/cancel")
                            .with(withAuth(operator))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELED"))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());

            // 취소 후에도 알림은 남고 CANCELED로 표시
            mockMvc.perform(get("/api/v1/registrations/cancel-requests")
                            .with(withAuth(operator)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].status").value("CANCELED"))
                    .andExpect(jsonPath("$.content[0].cancelReason").value("개인 사정"))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());

            // 신청자 목록에도 취소 요청 정보가 포함됨
            mockMvc.perform(get("/api/v1/events/" + event.getId() + "/registrations")
                            .with(withAuth(operator)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[?(@.registrationId == %d)].cancelReason", registration.getId())
                            .value("개인 사정"))
                    .andExpect(jsonPath("$.content[?(@.cancelRequestedAt != null)].registrationId")
                            .value(registration.getId().intValue()))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        private String rejectUrl(EventRegistration registration) {
            return "/api/v1/registrations/" + registration.getId() + "/cancel-request/reject";
        }

        @Test
        @DisplayName("[INT-070] OPERATOR가 취소 요청을 거절하면 200, 요청이 초기화되어 목록에서 사라지고 신청자가 다시 요청할 수 있다 (OpenAPI 스키마 검증)")
        void rejectCancelRequest_Operator_ClearsRequestAndAllowsReRequest() throws Exception {
            Event event = createAndSaveManualApproveEvent();
            EventRegistration registration = createAndSaveWaitingRegistration(event, member);
            requestCancel(event, member, "{\"reason\":\"개인 사정\"}").andExpect(status().isOk());

            mockMvc.perform(post(rejectUrl(registration)).with(withAuth(operator)).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.registrationId").value(registration.getId()))
                    .andExpect(jsonPath("$.status").value("WAITING"))
                    .andExpect(jsonPath("$.isRegistered").value(true))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());

            EventRegistration saved = eventRegistrationRepository.findById(registration.getId()).orElseThrow();
            assertThat(saved.getCancelReason()).isNull();
            assertThat(saved.getCancelRequestedAt()).isNull();

            mockMvc.perform(get("/api/v1/registrations/cancel-requests").with(withAuth(operator)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());

            // 거절 후 재요청은 409가 아니라 200
            requestCancel(event, member, "{\"reason\":\"다시 요청\"}").andExpect(status().isOk());
        }

        @Test
        @DisplayName("[INT-071] 취소 요청이 없는 신청의 요청 거절 → 400 EVENT_CANCEL_REQUEST_NOT_PENDING")
        void rejectCancelRequest_NoRequest_Returns400() throws Exception {
            Event event = createAndSaveManualApproveEvent();
            EventRegistration registration = createAndSaveWaitingRegistration(event, member);

            mockMvc.perform(post(rejectUrl(registration)).with(withAuth(operator)).with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("EVENT_CANCEL_REQUEST_NOT_PENDING"));
        }

        @Test
        @DisplayName("[INT-072] 이미 취소 처리된 신청의 요청 거절 → 400, 취소됨 알림은 목록에 남는다")
        void rejectCancelRequest_AlreadyCanceled_Returns400AndKeepsNotification() throws Exception {
            Event event = createAndSaveManualApproveEvent();
            EventRegistration registration = createAndSaveWaitingRegistration(event, member);
            requestCancel(event, member, "{\"reason\":\"개인 사정\"}").andExpect(status().isOk());
            mockMvc.perform(post("/api/v1/registrations/" + registration.getId() + "/cancel")
                            .with(withAuth(operator)).with(csrf()))
                    .andExpect(status().isOk());

            mockMvc.perform(post(rejectUrl(registration)).with(withAuth(operator)).with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("EVENT_CANCEL_REQUEST_NOT_PENDING"));

            mockMvc.perform(get("/api/v1/registrations/cancel-requests").with(withAuth(operator)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].status").value("CANCELED"))
                    .andExpect(jsonPath("$.content[0].cancelReason").value("개인 사정"));
        }

        @Test
        @DisplayName("[INT-073] MEMBER가 취소 요청 거절 → 403, 요청은 유지된다")
        void rejectCancelRequest_MemberRole_Returns403() throws Exception {
            Event event = createAndSaveManualApproveEvent();
            EventRegistration registration = createAndSaveWaitingRegistration(event, member);
            requestCancel(event, member, "{\"reason\":\"개인 사정\"}").andExpect(status().isOk());

            mockMvc.perform(post(rejectUrl(registration))
                            .header("Authorization", "Bearer " + generateToken(member)))
                    .andExpect(status().isForbidden());

            assertThat(eventRegistrationRepository.findById(registration.getId()).orElseThrow()
                    .getCancelReason()).isEqualTo("개인 사정");
        }

        @Test
        @DisplayName("[INT-074] 존재하지 않는 신청의 취소 요청 거절 → 404")
        void rejectCancelRequest_NotFound_Returns404() throws Exception {
            mockMvc.perform(post("/api/v1/registrations/999999/cancel-request/reject")
                            .with(withAuth(operator)).with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("[INT-058] MEMBER가 취소 요청 목록 조회 → 403")
        void getCancelRequestList_MemberRole_Returns403() throws Exception {
            mockMvc.perform(get("/api/v1/registrations/cancel-requests")
                            .header("Authorization", "Bearer " + generateToken(member)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[INT-059] 비인증 사용자가 취소 요청 목록 조회 → 401")
        void getCancelRequestList_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/v1/registrations/cancel-requests"))
                    .andExpect(status().isUnauthorized());
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
