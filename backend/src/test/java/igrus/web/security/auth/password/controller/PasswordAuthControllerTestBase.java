package igrus.web.security.auth.password.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.common.exception.GlobalExceptionHandler;
import igrus.web.security.auth.common.service.AccountStatusService;
import igrus.web.security.auth.common.service.account.CheckReRegistrationEligibilityService;
import igrus.web.security.auth.common.service.account.CheckRecoveryEligibilityService;
import igrus.web.security.auth.common.service.account.RecoverAccountService;
import igrus.web.security.auth.common.util.CookieUtil;
import igrus.web.security.auth.password.service.auth.LoginService;
import igrus.web.security.auth.password.service.auth.LogoutService;
import igrus.web.security.auth.password.service.auth.RefreshTokenService;
import igrus.web.security.auth.password.service.reset.RequestPasswordResetService;
import igrus.web.security.auth.password.service.reset.ResetPasswordService;
import igrus.web.security.auth.password.service.reset.ValidateResetTokenService;
import igrus.web.security.auth.password.service.signup.AutoResendVerificationService;
import igrus.web.security.auth.password.service.signup.CheckDuplicateService;
import igrus.web.security.auth.password.service.signup.ResendVerificationService;
import igrus.web.security.auth.password.service.signup.SignupService;
import igrus.web.security.auth.password.service.signup.TempStudentIdSignupService;
import igrus.web.security.auth.password.service.signup.VerifyEmailService;
import igrus.web.security.config.ApiSecurityConfig;
import igrus.web.security.config.SecurityConfigUtil;
import igrus.web.security.jwt.JwtAuthenticationEntryPoint;
import igrus.web.security.jwt.JwtAuthenticationFilter;
import igrus.web.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * PasswordAuthController 슬라이스 테스트를 위한 공통 베이스 클래스.
 *
 * <p>모든 하위 테스트 클래스가 동일한 어노테이션 조합을 공유하여
 * Spring 테스트 컨텍스트를 1개로 캐싱/재사용합니다.</p>
 */
@WebMvcTest(PasswordAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        ApiSecurityConfig.class,
        SecurityConfigUtil.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class
})
abstract class PasswordAuthControllerTestBase {

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    // === Auth Services ===
    @MockitoBean
    protected LoginService loginService;

    @MockitoBean
    protected LogoutService logoutService;

    @MockitoBean
    protected RefreshTokenService refreshTokenService;

    // === Signup Services ===
    @MockitoBean
    protected SignupService signupService;

    @MockitoBean
    protected TempStudentIdSignupService tempStudentIdSignupService;

    @MockitoBean
    protected CheckDuplicateService checkDuplicateService;

    @MockitoBean
    protected VerifyEmailService verifyEmailService;

    @MockitoBean
    protected ResendVerificationService resendVerificationService;

    @MockitoBean
    protected AutoResendVerificationService autoResendVerificationService;

    // === Password Reset Services ===
    @MockitoBean
    protected RequestPasswordResetService requestPasswordResetService;

    @MockitoBean
    protected ResetPasswordService resetPasswordService;

    @MockitoBean
    protected ValidateResetTokenService validateResetTokenService;

    // === Account Services ===
    @MockitoBean
    protected CheckReRegistrationEligibilityService checkReRegistrationEligibilityService;

    @MockitoBean
    protected CheckRecoveryEligibilityService checkRecoveryEligibilityService;

    @MockitoBean
    protected RecoverAccountService recoverAccountService;

    @MockitoBean
    protected AccountStatusService accountStatusService;

    // === Security ===
    @MockitoBean
    protected JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    protected JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    protected SecurityConfigUtil securityConfigUtil;

    // === Util ===
    @MockitoBean
    protected CookieUtil cookieUtil;
}
