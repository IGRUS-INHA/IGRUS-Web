package igrus.web.event.integration;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.repository.EventRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminEventController SecurityConfig 접근 제어 통합 테스트.
 *
 * <p>/api/v1/admin/events/** 경로에 대한 RBAC 규칙이
 * SecurityConfig에서 올바르게 적용되는지 검증합니다.</p>
 *
 * <p>테스트 케이스:</p>
 * <ul>
 *     <li>SEC-EVT-11: 비인증 사용자 → 401</li>
 *     <li>SEC-EVT-12: ASSOCIATE 역할 → 403</li>
 *     <li>SEC-EVT-13~15: MEMBER 역할 → 403 (목록, 상세, publish, unpublish)</li>
 *     <li>SEC-EVT-16: OPERATOR 역할 → 200 OK</li>
 *     <li>SEC-EVT-17: ADMIN 역할 → 200 OK</li>
 * </ul>
 */
@AutoConfigureMockMvc
@DisplayName("AdminEventController SecurityConfig 접근 제어 통합 테스트")
class AdminEventControllerSecurityTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    private User operator;
    private User admin;
    private User member;
    private User associate;

    private Event publishedEvent;
    private Event unpublishedEvent;

    @BeforeEach
    void setUp() {
        setUpBase();
        transactionTemplate.execute(status -> {
            operator = createAndSaveUser("20230001", "operator@inha.edu", UserRole.OPERATOR);
            admin = createAndSaveUser("20230002", "admin@inha.edu", UserRole.ADMIN);
            member = createAndSaveUser("20230003", "member@inha.edu", UserRole.MEMBER);
            associate = createAndSaveUser("20230004", "associate@inha.edu", UserRole.ASSOCIATE);
            return null;
        });

        // 테스트용 행사 생성
        publishedEvent = transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator,
                    "공개 행사",
                    "설명",
                    "장소",
                    now.plus(7, ChronoUnit.DAYS),
                    now.plus(8, ChronoUnit.DAYS),
                    now.minus(1, ChronoUnit.DAYS),
                    now.plus(6, ChronoUnit.DAYS),
                    10,
                    EventRegistrationType.AUTO_APPROVE
            );
            event.publish();
            return eventRepository.save(event);
        });

        unpublishedEvent = transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator,
                    "비공개 행사",
                    "설명",
                    "장소",
                    now.plus(7, ChronoUnit.DAYS),
                    now.plus(8, ChronoUnit.DAYS),
                    now.minus(1, ChronoUnit.DAYS),
                    now.plus(6, ChronoUnit.DAYS),
                    10,
                    EventRegistrationType.AUTO_APPROVE
            );
            return eventRepository.save(event);
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

    // ==================== 비인증 사용자 (401) ====================

    @Nested
    @DisplayName("비인증 사용자 접근 차단 (401)")
    class UnauthenticatedAccessTest {

        @Test
        @DisplayName("[SEC-EVT-11] 비인증 사용자 관리자 행사 목록 조회 -> 401")
        void getAdminEventList_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/v1/admin/events"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[SEC-EVT-11] 비인증 사용자 관리자 행사 상세 조회 -> 401")
        void getAdminEvent_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/v1/admin/events/" + publishedEvent.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[SEC-EVT-11] 비인증 사용자 행사 공개 -> 401")
        void publishEvent_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(post("/api/v1/admin/events/" + unpublishedEvent.getId() + "/publish"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[SEC-EVT-11] 비인증 사용자 행사 비공개 -> 401")
        void unpublishEvent_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(post("/api/v1/admin/events/" + publishedEvent.getId() + "/unpublish"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== ASSOCIATE 역할 (403) ====================

    @Nested
    @DisplayName("ASSOCIATE 역할 접근 차단 (403)")
    class AssociateAccessTest {

        @Test
        @DisplayName("[SEC-EVT-12] ASSOCIATE 관리자 행사 목록 조회 -> 403")
        void getAdminEventList_Associate_Returns403() throws Exception {
            mockMvc.perform(get("/api/v1/admin/events")
                            .with(withAuth(associate)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[SEC-EVT-12] ASSOCIATE 관리자 행사 상세 조회 -> 403")
        void getAdminEvent_Associate_Returns403() throws Exception {
            mockMvc.perform(get("/api/v1/admin/events/" + publishedEvent.getId())
                            .with(withAuth(associate)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[SEC-EVT-12] ASSOCIATE 행사 공개 -> 403")
        void publishEvent_Associate_Returns403() throws Exception {
            mockMvc.perform(post("/api/v1/admin/events/" + unpublishedEvent.getId() + "/publish")
                            .with(withAuth(associate)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[SEC-EVT-12] ASSOCIATE 행사 비공개 -> 403")
        void unpublishEvent_Associate_Returns403() throws Exception {
            mockMvc.perform(post("/api/v1/admin/events/" + publishedEvent.getId() + "/unpublish")
                            .with(withAuth(associate)))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== MEMBER 역할 (403) ====================

    @Nested
    @DisplayName("MEMBER 역할 접근 차단 (403)")
    class MemberAccessTest {

        @Test
        @DisplayName("[SEC-EVT-13] MEMBER 관리자 행사 목록 조회 -> 403")
        void getAdminEventList_Member_Returns403() throws Exception {
            mockMvc.perform(get("/api/v1/admin/events")
                            .with(withAuth(member)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[SEC-EVT-14] MEMBER 관리자 행사 상세 조회 -> 403")
        void getAdminEvent_Member_Returns403() throws Exception {
            mockMvc.perform(get("/api/v1/admin/events/" + publishedEvent.getId())
                            .with(withAuth(member)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[SEC-EVT-15] MEMBER 행사 공개 -> 403")
        void publishEvent_Member_Returns403() throws Exception {
            mockMvc.perform(post("/api/v1/admin/events/" + unpublishedEvent.getId() + "/publish")
                            .with(withAuth(member)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[SEC-EVT-15] MEMBER 행사 비공개 -> 403")
        void unpublishEvent_Member_Returns403() throws Exception {
            mockMvc.perform(post("/api/v1/admin/events/" + publishedEvent.getId() + "/unpublish")
                            .with(withAuth(member)))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== OPERATOR 역할 (200 OK) ====================

    @Nested
    @DisplayName("OPERATOR 역할 접근 허용 (200 OK)")
    class OperatorAccessTest {

        @Test
        @DisplayName("[SEC-EVT-16] OPERATOR 관리자 행사 목록 조회 -> 200 OK")
        void getAdminEventList_Operator_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/admin/events")
                            .with(withAuth(operator)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("[SEC-EVT-16] OPERATOR 관리자 행사 상세 조회 -> 200 OK")
        void getAdminEvent_Operator_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/admin/events/" + publishedEvent.getId())
                            .with(withAuth(operator)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("[SEC-EVT-17] OPERATOR 행사 공개 -> 200 OK")
        void publishEvent_Operator_Returns200() throws Exception {
            mockMvc.perform(post("/api/v1/admin/events/" + unpublishedEvent.getId() + "/publish")
                            .with(withAuth(operator)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("[SEC-EVT-17] OPERATOR 행사 비공개 -> 200 OK")
        void unpublishEvent_Operator_Returns200() throws Exception {
            mockMvc.perform(post("/api/v1/admin/events/" + publishedEvent.getId() + "/unpublish")
                            .with(withAuth(operator)))
                    .andExpect(status().isOk());
        }
    }

    // ==================== ADMIN 역할 (200 OK) ====================

    @Nested
    @DisplayName("ADMIN 역할 접근 허용 (200 OK)")
    class AdminAccessTest {

        @Test
        @DisplayName("[SEC-EVT-17] ADMIN 관리자 행사 목록 조회 -> 200 OK")
        void getAdminEventList_Admin_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/admin/events")
                            .with(withAuth(admin)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("[SEC-EVT-17] ADMIN 관리자 행사 상세 조회 -> 200 OK")
        void getAdminEvent_Admin_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/admin/events/" + publishedEvent.getId())
                            .with(withAuth(admin)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("[SEC-EVT-17] ADMIN 행사 공개 -> 200 OK")
        void publishEvent_Admin_Returns200() throws Exception {
            // unpublished 행사를 사용해야 publish 성공
            mockMvc.perform(post("/api/v1/admin/events/" + unpublishedEvent.getId() + "/publish")
                            .with(withAuth(admin)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("[SEC-EVT-17] ADMIN 행사 비공개 -> 200 OK")
        void unpublishEvent_Admin_Returns200() throws Exception {
            mockMvc.perform(post("/api/v1/admin/events/" + publishedEvent.getId() + "/unpublish")
                            .with(withAuth(admin)))
                    .andExpect(status().isOk());
        }
    }
}
