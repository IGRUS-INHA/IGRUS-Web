package igrus.web.event.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import igrus.web.common.OpenApiValidatorUtil;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.dto.request.CreateEventRequest;
import igrus.web.event.dto.request.EventStatusChangeReasonRequest;
import igrus.web.event.repository.EventRepository;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.security.jwt.JwtTokenProvider;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Event 컨트롤러 통합 테스트.
 *
 * <p>Spring Security의 인증 필터와 서비스 계층의 권한 검증이
 * HTTP 요청/응답 수준에서 올바르게 동작하는지 검증합니다.</p>
 *
 * <p>또한 2xx 성공 응답이 OpenAPI 스키마와 일치하는지 검증합니다.</p>
 *
 * <p>테스트 케이스:</p>
 * <ul>
 *     <li>TC-212-01: EventController GET /events 응답 스키마 검증</li>
 *     <li>INT-023: 비인증 사용자 행사 생성 → 401</li>
 *     <li>INT-024: 비인증 사용자 행사 상세 조회 → 401</li>
 *     <li>INT-025: MEMBER가 행사 생성 → 403</li>
 *     <li>INT-026: ASSOCIATE가 행사 상세 조회 → 403</li>
 *     <li>INT-027: MEMBER가 행사 취소 → 403</li>
 *     <li>INT-028: MEMBER가 행사 재활성화 → 403</li>
 *     <li>INT-029: MEMBER가 등록 재오픈 → 403</li>
 * </ul>
 */
@AutoConfigureMockMvc
@DisplayName("Event 컨트롤러 통합 테스트")
class EventControllerIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EventRepository eventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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

    private CreateEventRequest createValidEventRequest() {
        Instant now = Instant.now();
        return new CreateEventRequest(
                "테스트 행사",
                "행사 설명입니다",
                "인하대학교 5호관",
                now.plus(7, ChronoUnit.DAYS),
                now.plus(8, ChronoUnit.DAYS),
                now.plus(1, ChronoUnit.HOURS),
                now.plus(6, ChronoUnit.DAYS),
                30,
                EventRegistrationType.AUTO_APPROVE,
        null, null, null
        );
    }

    /**
     * 테스트용 행사를 생성하고 저장합니다.
     * 행사 상태: UPCOMING, 등록 상태: OPEN
     */
    private Event createAndSaveOpenEventWithAllowExternal() {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator,
                    "외부인 허용 행사",
                    "설명",
                    "장소",
                    now.plus(7, ChronoUnit.DAYS),
                    now.plus(8, ChronoUnit.DAYS),
                    now.minus(1, ChronoUnit.DAYS),
                    now.plus(6, ChronoUnit.DAYS),
                    10,
                    EventRegistrationType.AUTO_APPROVE,
                    null,
                    true
            );
            event.publish();
            return eventRepository.save(event);
        });
    }

    private Event createAndSaveOpenEvent() {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator,
                    "테스트 행사",
                    "설명",
                    "장소",
                    now.plus(7, ChronoUnit.DAYS),
                    now.plus(8, ChronoUnit.DAYS),
                    now.minus(1, ChronoUnit.DAYS),
                    now.plus(6, ChronoUnit.DAYS),
                    10,
                    EventRegistrationType.AUTO_APPROVE,
            null
            );
            event.publish();
            event.openRegistration();
            return eventRepository.save(event);
        });
    }

    /**
     * 테스트용 취소된 행사를 생성하고 저장합니다.
     */
    private Event createAndSaveCanceledEvent() {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator,
                    "취소된 행사",
                    "설명",
                    "장소",
                    now.plus(7, ChronoUnit.DAYS),
                    now.plus(8, ChronoUnit.DAYS),
                    now.minus(1, ChronoUnit.DAYS),
                    now.plus(6, ChronoUnit.DAYS),
                    10,
                    EventRegistrationType.AUTO_APPROVE,
            null
            );
            event.publish();
            event.openRegistration();
            event.cancel();
            return eventRepository.save(event);
        });
    }

    /**
     * 테스트용 등록 마감된 행사를 생성하고 저장합니다.
     */
    private Event createAndSaveClosedEvent() {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator,
                    "마감된 행사",
                    "설명",
                    "장소",
                    now.plus(7, ChronoUnit.DAYS),
                    now.plus(8, ChronoUnit.DAYS),
                    now.minus(1, ChronoUnit.DAYS),
                    now.plus(6, ChronoUnit.DAYS),
                    10,
                    EventRegistrationType.AUTO_APPROVE,
            null
            );
            event.publish();
            event.openRegistration();
            event.closeRegistrationManually();
            return eventRepository.save(event);
        });
    }

    // ==================== OpenAPI 응답 스키마 검증 (TC-212-01) ====================

    @Nested
    @DisplayName("GET /api/v1/events - 행사 목록 조회 (OpenAPI 스키마 검증)")
    class GetEventListOpenApiValidationTest {

        @Test
        @DisplayName("[TC-212-01] 행사 목록 조회 응답이 OpenAPI 스키마와 일치한다")
        void getEventList_ReturnsResponseMatchingOpenApiSpec() throws Exception {
            // given - 공개된 행사를 미리 생성
            createAndSaveOpenEvent();

            // when & then - 행사 목록은 인증 없이 조회 가능 (security: [{}])
            mockMvc.perform(get("/api/v1/events")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").exists())
                    .andExpect(jsonPath("$[0].title").value("테스트 행사"))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        @Test
        @DisplayName("[TC-212-01] 빈 행사 목록 조회 시 빈 배열 응답이 OpenAPI 스키마와 일치한다")
        void getEventList_Empty_ReturnsResponseMatchingOpenApiSpec() throws Exception {
            // when & then - 행사가 없는 경우 빈 배열 반환
            mockMvc.perform(get("/api/v1/events")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/events/{eventId} - 행사 상세 조회 (OpenAPI 스키마 검증)")
    class GetEventDetailOpenApiValidationTest {

        @Test
        @DisplayName("[TC-212-01] OPERATOR가 행사 상세 조회 시 응답이 OpenAPI 스키마와 일치한다")
        void getEvent_WithOperatorRole_ReturnsResponseMatchingOpenApiSpec() throws Exception {
            // given
            Event event = createAndSaveOpenEvent();

            // when & then
            mockMvc.perform(get("/api/v1/events/" + event.getId())
                            .with(withAuth(operator))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(event.getId()))
                    .andExpect(jsonPath("$.title").value("테스트 행사"))
                    .andExpect(jsonPath("$.registrationStatus").exists())
                    .andExpect(jsonPath("$.eventStatus").exists())
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        @Test
        @DisplayName("[TC-212-01] MEMBER가 행사 상세 조회 시 응답이 OpenAPI 스키마와 일치한다")
        void getEvent_WithMemberRole_ReturnsResponseMatchingOpenApiSpec() throws Exception {
            // given
            Event event = createAndSaveOpenEvent();

            // when & then
            mockMvc.perform(get("/api/v1/events/" + event.getId())
                            .with(withAuth(member))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(event.getId()))
                    .andExpect(jsonPath("$.canEdit").value(false))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }
    }

    // ==================== INT-023 ====================

    @Test
    @DisplayName("[INT-023] 비인증 사용자 행사 생성 → 401")
    void createEvent_Unauthenticated_Returns401() throws Exception {
        CreateEventRequest request = createValidEventRequest();

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== INT-024 ====================

    @Test
    @DisplayName("[INT-024] 비인증 사용자가 allowExternal=false 행사 상세 조회 → 401")
    void getEvent_Unauthenticated_AllowExternalFalse_Returns401() throws Exception {
        Event event = createAndSaveOpenEvent();

        mockMvc.perform(get("/api/v1/events/" + event.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ==================== INT-024a ====================

    @Test
    @DisplayName("[INT-024a] 비인증 사용자가 allowExternal=true 행사 상세 조회 → 200")
    void getEvent_Unauthenticated_AllowExternalTrue_Returns200() throws Exception {
        Event event = createAndSaveOpenEventWithAllowExternal();

        mockMvc.perform(get("/api/v1/events/" + event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event.getId()))
                .andExpect(jsonPath("$.canEdit").value(false))
                .andExpect(jsonPath("$.isRegistered").value(false))
                .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
    }

    // ==================== INT-025 ====================

    @Test
    @DisplayName("[INT-025] MEMBER가 행사 생성 → 403")
    void createEvent_MemberRole_Returns403() throws Exception {
        String token = generateToken(member);
        CreateEventRequest request = createValidEventRequest();

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== INT-026 ====================

    @Test
    @DisplayName("[INT-026] ASSOCIATE가 행사 상세 조회 → 403")
    void getEvent_AssociateRole_Returns403() throws Exception {
        Event event = createAndSaveOpenEvent();
        String token = generateToken(associate);

        mockMvc.perform(get("/api/v1/events/" + event.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ==================== INT-027 ====================

    @Test
    @DisplayName("[INT-027] MEMBER가 행사 취소 → 403")
    void cancelEvent_MemberRole_Returns403() throws Exception {
        Event event = createAndSaveOpenEvent();
        String token = generateToken(member);
        EventStatusChangeReasonRequest request = new EventStatusChangeReasonRequest("취소 사유");

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== INT-028 ====================

    @Test
    @DisplayName("[INT-028] MEMBER가 행사 재활성화 → 403")
    void reactivateEvent_MemberRole_Returns403() throws Exception {
        Event event = createAndSaveCanceledEvent();
        String token = generateToken(member);
        EventStatusChangeReasonRequest request = new EventStatusChangeReasonRequest("재활성화 사유");

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/reactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== INT-029 ====================

    @Test
    @DisplayName("[INT-029] MEMBER가 등록 재오픈 → 403")
    void reopenRegistration_MemberRole_Returns403() throws Exception {
        Event event = createAndSaveClosedEvent();
        String token = generateToken(member);
        EventStatusChangeReasonRequest request = new EventStatusChangeReasonRequest("추가 모집이 필요합니다");

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/reopen-registration")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
