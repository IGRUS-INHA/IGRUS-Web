package igrus.web.event.integration;

import igrus.web.common.OpenApiValidatorUtil;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.repository.EventRegistrationRepository;
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
 * 외부인 행사 신청 + 관리자 취소 컨트롤러 통합 테스트.
 * 테스트 케이스 문서: docs/test-case/event/external-event-registration-test-cases.md
 *
 * <p>TASK-025: 외부인 컨트롤러 통합 테스트 (TC-057~TC-069, TC-080)</p>
 * <p>TASK-026: 관리자 취소 통합 테스트 (TC-021, TC-067, TC-068)</p>
 */
@AutoConfigureMockMvc
@DisplayName("외부인 행사 신청 + 관리자 취소 컨트롤러 통합 테스트")
class ExternalEventRegistrationControllerIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

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

    private Event createAndSaveOpenEvent(boolean allowExternal, EventRegistrationType type) {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator, "테스트 행사", "설명", "장소",
                    now.plus(7, ChronoUnit.DAYS),
                    now.plus(8, ChronoUnit.DAYS),
                    now.minus(1, ChronoUnit.DAYS),
                    now.plus(6, ChronoUnit.DAYS),
                    10,
                    type,
                    null,
                    allowExternal
            );
            event.publish();
            event.openRegistration();
            return eventRepository.save(event);
        });
    }

    private Event createAndSaveUnpublishedEvent(boolean allowExternal) {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator, "비공개 행사", "설명", "장소",
                    now.plus(7, ChronoUnit.DAYS),
                    now.plus(8, ChronoUnit.DAYS),
                    now.minus(1, ChronoUnit.DAYS),
                    now.plus(6, ChronoUnit.DAYS),
                    10,
                    EventRegistrationType.AUTO_APPROVE,
                    null,
                    allowExternal
            );
            // publish 하지 않음 -> UNPUBLISHED
            return eventRepository.save(event);
        });
    }

    private String externalRegistrationJson(String name, String studentId, String phone, String department) {
        return """
                {
                    "name": "%s",
                    "studentId": "%s",
                    "phone": "%s",
                    "department": "%s"
                }
                """.formatted(name, studentId, phone, department);
    }

    private EventRegistration createAndSaveExternalRegistration(Event event, String name, String studentId,
                                                                 String phone, String department) {
        return transactionTemplate.execute(status -> {
            EventRegistration registration = EventRegistration.createExternal(event, name, studentId, phone, department);
            return eventRegistrationRepository.save(registration);
        });
    }

    // ==================== TASK-025: 외부인 컨트롤러 통합 테스트 ====================

    @Nested
    @DisplayName("POST /api/v1/events/{eventId}/registrations/external - 외부인 신청")
    class ExternalRegistrationTest {

        /**
         * TC-057: allowExternal=true + 외부인 -> 201 Created
         */
        @Test
        @DisplayName("[TC-057] allowExternal=true 행사에 외부인 신청 -> 201 Created")
        void externalRegister_AllowExternalTrue_Returns201() throws Exception {
            Event event = createAndSaveOpenEvent(true, EventRegistrationType.AUTO_APPROVE);

            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations/external")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(externalRegistrationJson("홍길동", "12345678", "01012345678", "컴퓨터공학과")))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.registrationId").exists())
                    .andExpect(jsonPath("$.status").value("REGISTERED"))
                    .andExpect(jsonPath("$.isRegistered").value(true))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        /**
         * TC-058: allowExternal=true + 준회원(ASSOCIATE) -> 201 Created
         * allowExternal=true이면 서비스 레벨에서 준회원도 신청 허용 (EXT-INV-05).
         */
        @Test
        @DisplayName("[TC-058] allowExternal=true 행사에 준회원 신청 -> 201 Created")
        void memberRegister_AssociateAllowExternalTrue_Returns201() throws Exception {
            Event event = createAndSaveOpenEvent(true, EventRegistrationType.AUTO_APPROVE);

            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                            .with(withAuth(associate))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.registrationId").exists())
                    .andExpect(jsonPath("$.isRegistered").value(true))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        /**
         * TC-059: allowExternal=true + 정회원(MEMBER+) -> 201 Created
         */
        @Test
        @DisplayName("[TC-059] allowExternal=true 행사에 정회원 신청 -> 201 Created")
        void memberRegister_MemberAllowExternalTrue_Returns201() throws Exception {
            Event event = createAndSaveOpenEvent(true, EventRegistrationType.AUTO_APPROVE);

            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                            .with(withAuth(member))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.registrationId").exists())
                    .andExpect(jsonPath("$.isRegistered").value(true))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        /**
         * TC-060: allowExternal=false + 외부인 -> 400 Bad Request
         */
        @Test
        @DisplayName("[TC-060] allowExternal=false 행사에 외부인 신청 -> 400 Bad Request")
        void externalRegister_AllowExternalFalse_Returns400() throws Exception {
            Event event = createAndSaveOpenEvent(false, EventRegistrationType.AUTO_APPROVE);

            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations/external")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(externalRegistrationJson("홍길동", "12345678", "01012345678", "컴퓨터공학과")))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        /**
         * TC-061: allowExternal=false + 준회원(ASSOCIATE) -> 403 Forbidden
         */
        @Test
        @DisplayName("[TC-061] allowExternal=false 행사에 준회원 신청 -> 403 Forbidden")
        void memberRegister_AssociateAllowExternalFalse_Returns403() throws Exception {
            Event event = createAndSaveOpenEvent(false, EventRegistrationType.AUTO_APPROVE);

            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                            .with(withAuth(associate))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        /**
         * TC-062: allowExternal=false + 정회원(MEMBER+) -> 201 Created
         * allowExternal 설정과 무관하게 정회원 이상은 항상 신청 가능.
         */
        @Test
        @DisplayName("[TC-062] allowExternal=false 행사에 정회원 신청 -> 201 Created")
        void memberRegister_MemberAllowExternalFalse_Returns201() throws Exception {
            Event event = createAndSaveOpenEvent(false, EventRegistrationType.AUTO_APPROVE);

            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                            .with(withAuth(member))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.registrationId").exists())
                    .andExpect(jsonPath("$.isRegistered").value(true))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        /**
         * TC-063: SEC-EXT-01 — 외부인이 allowExternal=false 행사에 신청 시 400
         */
        @Test
        @DisplayName("[TC-063] SEC-EXT-01: allowExternal=false 행사에 외부인 신청 -> 400")
        void secExt01_ExternalOnDisabledEvent_Returns400() throws Exception {
            Event event = createAndSaveOpenEvent(false, EventRegistrationType.AUTO_APPROVE);

            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations/external")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(externalRegistrationJson("외부인", "99998888", "01099998888", "경영학과")))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        /**
         * TC-064: SEC-EXT-02 — 외부인 엔드포인트에 인증 토큰 없이 접근 시 정상 처리
         */
        @Test
        @DisplayName("[TC-064] SEC-EXT-02: 인증 없이 외부인 엔드포인트 접근 -> 201 Created")
        void secExt02_NoAuth_Returns201() throws Exception {
            Event event = createAndSaveOpenEvent(true, EventRegistrationType.AUTO_APPROVE);

            // 인증 토큰 없이 요청
            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations/external")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(externalRegistrationJson("비인증외부인", "77776666", "01077776666", "물리학과")))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        /**
         * TC-065: SEC-EXT-03 — 준회원이 allowExternal=false 행사에 신청 시 403
         */
        @Test
        @DisplayName("[TC-065] SEC-EXT-03: 준회원 + allowExternal=false -> 403")
        void secExt03_AssociateOnDisabledEvent_Returns403() throws Exception {
            Event event = createAndSaveOpenEvent(false, EventRegistrationType.AUTO_APPROVE);

            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                            .with(withAuth(associate))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        /**
         * TC-066: SEC-EXT-04 — 준회원이 allowExternal=true 행사에 신청 시 201
         * allowExternal=true이면 서비스 레벨에서 준회원도 신청 허용 (EXT-INV-05).
         */
        @Test
        @DisplayName("[TC-066] SEC-EXT-04: 준회원 + allowExternal=true -> 201 Created")
        void secExt04_AssociateOnEnabledEvent_Returns201() throws Exception {
            Event event = createAndSaveOpenEvent(true, EventRegistrationType.AUTO_APPROVE);

            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                            .with(withAuth(associate))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.registrationId").exists())
                    .andExpect(jsonPath("$.isRegistered").value(true))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        /**
         * TC-069: SEC-EXT-07 — 외부인이 allowExternal=true + UNPUBLISHED 행사에 신청 시 404
         */
        @Test
        @DisplayName("[TC-069] SEC-EXT-07: UNPUBLISHED 행사에 외부인 신청 -> 404")
        void secExt07_UnpublishedEvent_Returns404() throws Exception {
            Event event = createAndSaveUnpublishedEvent(true);

            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations/external")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(externalRegistrationJson("외부인", "55554444", "01055554444", "화학과")))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        /**
         * TC-080: RegistrationListResponse 스키마 변경 — 외부인 신청 포함 시 응답 필드 검증
         * 회원 신청은 OPERATOR 권한으로 수행 (Security Config에서 OPERATOR/ADMIN만 접근 가능).
         */
        @Test
        @DisplayName("[TC-080] 신청자 목록에서 회원+외부인 혼합 시 isExternal, phone 필드 검증")
        void registrationList_MixedMemberAndExternal_ResponseFieldsCorrect() throws Exception {
            Event event = createAndSaveOpenEvent(true, EventRegistrationType.AUTO_APPROVE);

            // 회원 신청 (OPERATOR 권한으로 수행 - Security Config 제약)
            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations")
                            .with(withAuth(operator))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isCreated());

            // 외부인 신청
            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations/external")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(externalRegistrationJson("외부인", "33332222", "01033332222", "전자공학과")))
                    .andExpect(status().isCreated());

            // 신청자 목록 조회 (OPERATOR)
            mockMvc.perform(get("/api/v1/events/" + event.getId() + "/registrations")
                            .with(withAuth(operator))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    // 외부인 신청 레코드 검증 (content[0] - 최신순 정렬)
                    .andExpect(jsonPath("$.content[0].userId").isEmpty())
                    .andExpect(jsonPath("$.content[0].isExternal").value(true))
                    .andExpect(jsonPath("$.content[0].phone").value("01033332222"))
                    .andExpect(jsonPath("$.content[0].userName").value("외부인"))
                    .andExpect(jsonPath("$.content[0].studentId").value("33332222"))
                    // 회원 신청 레코드 검증 (content[1])
                    .andExpect(jsonPath("$.content[1].userId").value(operator.getId().intValue()))
                    .andExpect(jsonPath("$.content[1].isExternal").value(false))
                    .andExpect(jsonPath("$.content[1].phone").isEmpty())
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }
    }

    // ==================== TASK-026: 관리자 취소 통합 테스트 ====================

    @Nested
    @DisplayName("POST /api/v1/registrations/{registrationId}/cancel - 관리자 취소")
    class AdminCancelTest {

        /**
         * TC-068: SEC-EXT-06 — OPERATOR가 외부인 신청 취소 시 정상
         */
        @Test
        @DisplayName("[TC-068] SEC-EXT-06: OPERATOR가 외부인 신청 취소 -> 200 OK")
        void cancelByAdmin_Operator_Returns200() throws Exception {
            Event event = createAndSaveOpenEvent(true, EventRegistrationType.AUTO_APPROVE);
            EventRegistration registration = createAndSaveExternalRegistration(
                    event, "외부인", "44443333", "01044443333", "수학과");

            mockMvc.perform(post("/api/v1/registrations/" + registration.getId() + "/cancel")
                            .with(withAuth(operator))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.registrationId").value(registration.getId()))
                    .andExpect(jsonPath("$.status").value("CANCELED"))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        /**
         * TC-067: SEC-EXT-05 — 일반 회원(MEMBER)이 외부인 신청 취소 시 403
         */
        @Test
        @DisplayName("[TC-067] SEC-EXT-05: MEMBER가 외부인 신청 취소 -> 403 Forbidden")
        void cancelByAdmin_Member_Returns403() throws Exception {
            Event event = createAndSaveOpenEvent(true, EventRegistrationType.AUTO_APPROVE);
            EventRegistration registration = createAndSaveExternalRegistration(
                    event, "외부인", "66665555", "01066665555", "생물학과");

            mockMvc.perform(post("/api/v1/registrations/" + registration.getId() + "/cancel")
                            .with(withAuth(member))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        /**
         * TC-021: MEMBER가 외부인 신청 취소 시도 시 403 에러 (컨트롤러 레벨 검증)
         */
        @Test
        @DisplayName("[TC-021] MEMBER가 관리자 취소 엔드포인트 호출 -> 403 Forbidden")
        void cancelByAdmin_MemberRole_Returns403() throws Exception {
            Event event = createAndSaveOpenEvent(true, EventRegistrationType.AUTO_APPROVE);
            EventRegistration registration = createAndSaveExternalRegistration(
                    event, "외부인2", "88887777", "01088887777", "국문학과");

            mockMvc.perform(post("/api/v1/registrations/" + registration.getId() + "/cancel")
                            .with(withAuth(member))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        /**
         * 비인증 사용자가 관리자 취소 시도 시 401
         */
        @Test
        @DisplayName("비인증 사용자가 관리자 취소 시도 -> 401 Unauthorized")
        void cancelByAdmin_Unauthenticated_Returns401() throws Exception {
            Event event = createAndSaveOpenEvent(true, EventRegistrationType.AUTO_APPROVE);
            EventRegistration registration = createAndSaveExternalRegistration(
                    event, "외부인3", "11112222", "01011112222", "역사학과");

            mockMvc.perform(post("/api/v1/registrations/" + registration.getId() + "/cancel"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        /**
         * 관리자 취소 후 currentCount 감소 검증
         */
        @Test
        @DisplayName("관리자 취소 후 currentCount 감소 검증")
        void cancelByAdmin_DecrementCurrentCount() throws Exception {
            Event event = createAndSaveOpenEvent(true, EventRegistrationType.AUTO_APPROVE);

            // 외부인 신청 (currentCount 증가)
            mockMvc.perform(post("/api/v1/events/" + event.getId() + "/registrations/external")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(externalRegistrationJson("외부인4", "22221111", "01022221111", "철학과")))
                    .andExpect(status().isCreated());

            // currentCount 확인
            Event afterRegister = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            assertThat(afterRegister.getCurrentCount()).isEqualTo(1);

            // 신청 ID 조회
            EventRegistration registration = eventRegistrationRepository
                    .findAll().stream()
                    .filter(r -> "22221111".equals(r.getExternalStudentId()))
                    .findFirst()
                    .orElseThrow();

            // 관리자 취소
            mockMvc.perform(post("/api/v1/registrations/" + registration.getId() + "/cancel")
                            .with(withAuth(operator))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELED"));

            // currentCount 감소 확인
            Event afterCancel = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            assertThat(afterCancel.getCurrentCount()).isEqualTo(0);
        }
    }
}
