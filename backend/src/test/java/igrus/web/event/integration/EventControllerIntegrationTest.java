package igrus.web.event.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.dto.request.CreateEventRequest;
import igrus.web.event.dto.request.ReopenRegistrationRequest;
import igrus.web.event.repository.EventRepository;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Event 컨트롤러 RBAC 통합 테스트.
 *
 * <p>Spring Security의 인증 필터와 서비스 계층의 권한 검증이
 * HTTP 요청/응답 수준에서 올바르게 동작하는지 검증합니다.</p>
 *
 * <p>테스트 케이스:</p>
 * <ul>
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
@DisplayName("Event 컨트롤러 RBAC 통합 테스트")
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
                EventRegistrationType.AUTO_APPROVE
        );
    }

    /**
     * 테스트용 행사를 생성하고 저장합니다.
     * 행사 상태: UPCOMING, 등록 상태: OPEN
     */
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
                    EventRegistrationType.AUTO_APPROVE
            );
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
                    EventRegistrationType.AUTO_APPROVE
            );
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
                    EventRegistrationType.AUTO_APPROVE
            );
            event.openRegistration();
            event.closeRegistrationManually();
            return eventRepository.save(event);
        });
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
    @DisplayName("[INT-024] 비인증 사용자 행사 상세 조회 → 401")
    void getEvent_Unauthenticated_Returns401() throws Exception {
        Event event = createAndSaveOpenEvent();

        mockMvc.perform(get("/api/v1/events/" + event.getId()))
                .andExpect(status().isUnauthorized());
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

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ==================== INT-028 ====================

    @Test
    @DisplayName("[INT-028] MEMBER가 행사 재활성화 → 403")
    void reactivateEvent_MemberRole_Returns403() throws Exception {
        Event event = createAndSaveCanceledEvent();
        String token = generateToken(member);

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/reactivate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ==================== INT-029 ====================

    @Test
    @DisplayName("[INT-029] MEMBER가 등록 재오픈 → 403")
    void reopenRegistration_MemberRole_Returns403() throws Exception {
        Event event = createAndSaveClosedEvent();
        String token = generateToken(member);
        ReopenRegistrationRequest request = new ReopenRegistrationRequest("추가 모집이 필요합니다");

        mockMvc.perform(post("/api/v1/events/" + event.getId() + "/reopen-registration")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
