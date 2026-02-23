package igrus.web.event.integration;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.event.repository.EventRepository;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EventRegistration 컨트롤러 RBAC 통합 테스트.
 *
 * <p>Spring Security의 인증 필터와 서비스 계층의 권한 검증이
 * HTTP 요청/응답 수준에서 올바르게 동작하는지 검증합니다.</p>
 *
 * <p>테스트 케이스:</p>
 * <ul>
 *     <li>INT-030: 비인증 사용자 행사 신청 → 401</li>
 *     <li>INT-031: ASSOCIATE가 행사 신청 → 403</li>
 *     <li>INT-032: MEMBER가 신청 승인 → 403</li>
 *     <li>INT-033: MEMBER가 신청 거절 → 403</li>
 *     <li>INT-034: MEMBER가 승인/거절 되돌리기 → 403</li>
 *     <li>INT-035: MEMBER가 신청자 목록 조회 → 403</li>
 * </ul>
 */
@AutoConfigureMockMvc
@DisplayName("EventRegistration 컨트롤러 RBAC 통합 테스트")
class EventRegistrationControllerIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

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
                    EventRegistrationType.MANUAL_APPROVE
            );
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
                        .header("Authorization", "Bearer " + token))
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
}
