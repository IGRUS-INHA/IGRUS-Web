package igrus.web.security.auth.password.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.common.exception.ErrorCode;
import igrus.web.common.exception.GlobalExceptionHandler;
import igrus.web.security.auth.common.exception.signup.DuplicateEmailException;
import igrus.web.security.auth.common.exception.signup.DuplicatePhoneNumberException;
import igrus.web.security.auth.common.exception.signup.DuplicateStudentIdException;
import igrus.web.security.auth.common.service.AccountRecoveryService;
import igrus.web.security.auth.common.service.AccountStatusService;
import igrus.web.security.auth.common.util.CookieUtil;
import igrus.web.security.auth.password.dto.request.PasswordSignupRequest;
import igrus.web.security.auth.password.dto.response.PasswordSignupResponse;
import igrus.web.security.auth.password.service.PasswordAuthService;
import igrus.web.security.auth.password.service.PasswordResetService;
import igrus.web.security.auth.password.service.PasswordSignupService;
import igrus.web.security.config.ApiSecurityConfig;
import igrus.web.security.config.SecurityConfigUtil;
import igrus.web.security.jwt.JwtAuthenticationFilter;
import igrus.web.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordAuthController.class)
@Import({GlobalExceptionHandler.class, ApiSecurityConfig.class, SecurityConfigUtil.class, JwtAuthenticationFilter.class})
@DisplayName("PasswordAuthController 회원가입 테스트")
class PasswordAuthControllerSignupTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PasswordAuthService passwordAuthService;

    @MockitoBean
    private PasswordSignupService passwordSignupService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccountRecoveryService accountRecoveryService;

    @MockitoBean
    private AccountStatusService accountStatusService;

    @MockitoBean
    private CookieUtil cookieUtil;

    private static final String SIGNUP_URL = "/api/v1/auth/password/signup";

    // Valid test data constants
    private static final String VALID_STUDENT_ID = "12345678";
    private static final String VALID_NAME = "홍길동";
    private static final String VALID_EMAIL = "test@inha.edu";
    private static final String VALID_PASSWORD = "Test1234!@";
    private static final String VALID_PHONE = "010-1234-5678";
    private static final String VALID_DEPARTMENT = "컴퓨터공학과";
    private static final String VALID_MOTIVATION = "프로그래밍을 배우고 싶습니다.";

    private PasswordSignupRequest createValidRequest() {
        return new PasswordSignupRequest(
                VALID_STUDENT_ID,
                VALID_NAME,
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_PHONE,
                VALID_DEPARTMENT,
                VALID_MOTIVATION,
                true
        );
    }

    private PasswordSignupRequest createRequestWithStudentId(String studentId) {
        return new PasswordSignupRequest(
                studentId,
                VALID_NAME,
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_PHONE,
                VALID_DEPARTMENT,
                VALID_MOTIVATION,
                true
        );
    }

    private PasswordSignupRequest createRequestWithName(String name) {
        return new PasswordSignupRequest(
                VALID_STUDENT_ID,
                name,
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_PHONE,
                VALID_DEPARTMENT,
                VALID_MOTIVATION,
                true
        );
    }

    private PasswordSignupRequest createRequestWithEmail(String email) {
        return new PasswordSignupRequest(
                VALID_STUDENT_ID,
                VALID_NAME,
                email,
                VALID_PASSWORD,
                VALID_PHONE,
                VALID_DEPARTMENT,
                VALID_MOTIVATION,
                true
        );
    }

    private PasswordSignupRequest createRequestWithPassword(String password) {
        return new PasswordSignupRequest(
                VALID_STUDENT_ID,
                VALID_NAME,
                VALID_EMAIL,
                password,
                VALID_PHONE,
                VALID_DEPARTMENT,
                VALID_MOTIVATION,
                true
        );
    }

    private PasswordSignupRequest createRequestWithPhone(String phone) {
        return new PasswordSignupRequest(
                VALID_STUDENT_ID,
                VALID_NAME,
                VALID_EMAIL,
                VALID_PASSWORD,
                phone,
                VALID_DEPARTMENT,
                VALID_MOTIVATION,
                true
        );
    }

    private PasswordSignupRequest createRequestWithDepartment(String department) {
        return new PasswordSignupRequest(
                VALID_STUDENT_ID,
                VALID_NAME,
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_PHONE,
                department,
                VALID_MOTIVATION,
                true
        );
    }

    private PasswordSignupRequest createRequestWithMotivation(String motivation) {
        return new PasswordSignupRequest(
                VALID_STUDENT_ID,
                VALID_NAME,
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_PHONE,
                VALID_DEPARTMENT,
                motivation,
                true
        );
    }

    private PasswordSignupRequest createRequestWithPrivacyConsent(Boolean privacyConsent) {
        return new PasswordSignupRequest(
                VALID_STUDENT_ID,
                VALID_NAME,
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_PHONE,
                VALID_DEPARTMENT,
                VALID_MOTIVATION,
                privacyConsent
        );
    }

    @Nested
    @DisplayName("회원가입 성공")
    class SignupSuccessTest {

        @Test
        @DisplayName("[REG-010] 모든 필수 정보 입력 시 가입 성공")
        void signup_WithValidRequest_Returns201() throws Exception {
            // given
            PasswordSignupRequest request = createValidRequest();
            PasswordSignupResponse response = PasswordSignupResponse.pendingVerification(VALID_EMAIL);

            given(passwordSignupService.signup(any(PasswordSignupRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                    .andExpect(jsonPath("$.requiresVerification").value(true));
        }
    }

    @Nested
    @DisplayName("학번 검증")
    class StudentIdValidationTest {

        @Test
        @DisplayName("[REG-011] 학번 8자리 미만 - 400 반환")
        void signup_WithStudentIdLessThan8Digits_Returns400() throws Exception {
            // given
            PasswordSignupRequest request = createRequestWithStudentId("1234567");

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }

        @Test
        @DisplayName("[REG-012] 학번 8자리 초과 - 400 반환")
        void signup_WithStudentIdMoreThan8Digits_Returns400() throws Exception {
            // given
            PasswordSignupRequest request = createRequestWithStudentId("123456789");

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }

        @Test
        @DisplayName("[REG-013] 학번 숫자 외 문자 포함 - 400 반환")
        void signup_WithStudentIdContainingNonDigit_Returns400() throws Exception {
            // given
            PasswordSignupRequest request = createRequestWithStudentId("1234567a");

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }
    }

    @Nested
    @DisplayName("본명 검증")
    class NameValidationTest {

        @Test
        @DisplayName("[REG-014] 본명 미입력 - 400 반환")
        void signup_WithBlankName_Returns400() throws Exception {
            // given
            PasswordSignupRequest request = createRequestWithName("");

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }

        @Test
        @DisplayName("[REG-014] 본명 공백만 입력 - 400 반환")
        void signup_WithWhitespaceName_Returns400() throws Exception {
            // given
            PasswordSignupRequest request = createRequestWithName("   ");

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }
    }

    @Nested
    @DisplayName("이메일 검증")
    class EmailValidationTest {

        @Test
        @DisplayName("[REG-015] 이메일 형식 오류 - 400 반환")
        void signup_WithInvalidEmail_Returns400() throws Exception {
            // given
            PasswordSignupRequest request = createRequestWithEmail("invalid-email");

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }
    }

    @Nested
    @DisplayName("전화번호 검증")
    class PhoneNumberValidationTest {

        @Test
        @DisplayName("[REG-016] 전화번호 형식 오류 - 400 반환")
        void signup_WithInvalidPhoneNumber_Returns400() throws Exception {
            // given
            PasswordSignupRequest request = createRequestWithPhone("invalid-phone");

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }
    }

    @Nested
    @DisplayName("학과 검증")
    class DepartmentValidationTest {

        @Test
        @DisplayName("[REG-017] 학과 미입력 - 400 반환")
        void signup_WithBlankDepartment_Returns400() throws Exception {
            // given
            PasswordSignupRequest request = createRequestWithDepartment("");

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }
    }

    @Nested
    @DisplayName("가입 동기 검증")
    class MotivationValidationTest {

        @Test
        @DisplayName("[REG-018] 가입 동기 미입력 - 400 반환")
        void signup_WithBlankMotivation_Returns400() throws Exception {
            // given
            PasswordSignupRequest request = createRequestWithMotivation("");

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }
    }

    @Nested
    @DisplayName("비밀번호 검증")
    class PasswordValidationTest {

        @Test
        @DisplayName("[REG-021] 8자 미만 비밀번호 - 400 반환")
        void signup_WithPasswordLessThan8Chars_Returns400() throws Exception {
            // given
            PasswordSignupRequest request = createRequestWithPassword("Test1!");

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }

        @Test
        @DisplayName("[REG-022] 대문자 미포함 - 400 반환")
        void signup_WithPasswordWithoutUppercase_Returns400() throws Exception {
            // given
            PasswordSignupRequest request = createRequestWithPassword("test1234!@");

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }

        @Test
        @DisplayName("[REG-023] 소문자 미포함 - 400 반환")
        void signup_WithPasswordWithoutLowercase_Returns400() throws Exception {
            // given
            PasswordSignupRequest request = createRequestWithPassword("TEST1234!@");

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }

        @Test
        @DisplayName("[REG-024] 숫자 미포함 - 400 반환")
        void signup_WithPasswordWithoutDigit_Returns400() throws Exception {
            // given
            PasswordSignupRequest request = createRequestWithPassword("TestTest!@");

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }

        @Test
        @DisplayName("[REG-025] 특수문자 미포함 - 400 반환")
        void signup_WithPasswordWithoutSpecialChar_Returns400() throws Exception {
            // given
            PasswordSignupRequest request = createRequestWithPassword("Test1234");

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }
    }

    @Nested
    @DisplayName("개인정보 동의 검증")
    class PrivacyConsentValidationTest {

        @Test
        @DisplayName("개인정보 동의 false - 400 반환")
        void signup_WithPrivacyConsentFalse_Returns400() throws Exception {
            // given
            PasswordSignupRequest request = createRequestWithPrivacyConsent(false);

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }

        @Test
        @DisplayName("개인정보 동의 null - 400 반환")
        void signup_WithPrivacyConsentNull_Returns400() throws Exception {
            // given
            PasswordSignupRequest request = createRequestWithPrivacyConsent(null);

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }
    }

    @Nested
    @DisplayName("중복 검증")
    class DuplicateCheckTest {

        @Test
        @DisplayName("[REG-030] 중복 학번 - 409 반환")
        void signup_WithDuplicateStudentId_Returns409() throws Exception {
            // given
            PasswordSignupRequest request = createValidRequest();

            given(passwordSignupService.signup(any(PasswordSignupRequest.class)))
                    .willThrow(new DuplicateStudentIdException());

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_STUDENT_ID.getCode()));
        }

        @Test
        @DisplayName("[REG-031] 중복 이메일 - 409 반환")
        void signup_WithDuplicateEmail_Returns409() throws Exception {
            // given
            PasswordSignupRequest request = createValidRequest();

            given(passwordSignupService.signup(any(PasswordSignupRequest.class)))
                    .willThrow(new DuplicateEmailException());

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_EMAIL.getCode()));
        }

        @Test
        @DisplayName("[REG-032] 중복 전화번호 - 409 반환")
        void signup_WithDuplicatePhoneNumber_Returns409() throws Exception {
            // given
            PasswordSignupRequest request = createValidRequest();

            given(passwordSignupService.signup(any(PasswordSignupRequest.class)))
                    .willThrow(new DuplicatePhoneNumberException());

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_PHONE_NUMBER.getCode()));
        }
    }
}
