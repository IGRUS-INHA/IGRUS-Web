package igrus.web.security.auth.password.controller;

import igrus.web.common.exception.CommonErrorCode;
import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.util.ServletContextUtil;
import igrus.web.generated.api.PasswordAuthenticationApi;
import igrus.web.generated.model.CheckReRegistrationEligibility200Response;
import igrus.web.generated.model.CheckRecoveryEligibility200Response;
import igrus.web.generated.model.CheckStudentIdDuplicate200Response;
import igrus.web.generated.model.ConfirmPasswordResetRequest;
import igrus.web.generated.model.Login200Response;
import igrus.web.generated.model.LoginRequest;
import igrus.web.generated.model.RecoverAccount200Response;
import igrus.web.generated.model.RecoverAccountRequest;
import igrus.web.generated.model.RefreshToken200Response;
import igrus.web.generated.model.RequestPasswordResetRequest;
import igrus.web.generated.model.SendPreSignupCode200Response;
import igrus.web.generated.model.SendPreSignupCodeRequest;
import igrus.web.generated.model.Signup201Response;
import igrus.web.generated.model.SignupRequest;
import igrus.web.generated.model.SignupWithTemporaryStudentIdRequest;
import igrus.web.generated.model.VerifyEmailChangeRequest;
import igrus.web.generated.model.VerifyPreSignupCode200Response;
import igrus.web.security.auth.common.dto.internal.RecoveryResult;
import igrus.web.security.auth.common.dto.response.AccountRecoveryResponse;
import igrus.web.security.auth.common.dto.response.RecoveryEligibilityResponse;
import igrus.web.security.auth.common.exception.token.RefreshTokenExpiredException;
import igrus.web.security.auth.common.exception.token.RefreshTokenInvalidException;
import igrus.web.security.auth.common.exception.token.RefreshTokenTheftException;
import igrus.web.security.auth.common.service.account.CheckReRegistrationEligibilityService;
import igrus.web.security.auth.common.service.account.CheckRecoveryEligibilityService;
import igrus.web.security.auth.common.service.account.ReRegistrationCheckResult;
import igrus.web.security.auth.common.service.account.RecoverAccountService;
import igrus.web.security.auth.common.util.CookieUtil;
import igrus.web.security.auth.password.dto.internal.LoginResult;
import igrus.web.security.auth.password.dto.internal.TokenRotationResult;
import igrus.web.security.auth.password.dto.request.PasswordLoginRequest;
import igrus.web.security.auth.password.dto.request.PasswordResetConfirmRequest;
import igrus.web.security.auth.password.dto.request.PasswordSignupRequest;
import igrus.web.security.auth.password.dto.request.TemporaryStudentIdSignupRequest;
import igrus.web.security.auth.password.dto.response.DuplicateCheckResponse;
import igrus.web.security.auth.password.dto.response.PasswordLoginResponse;
import igrus.web.security.auth.password.dto.response.PasswordSignupResponse;
import igrus.web.security.auth.password.dto.response.PreSignupVerificationResponse;
import igrus.web.security.auth.password.dto.response.TokenRefreshResponse;
import igrus.web.security.auth.password.dto.response.VerificationResendResponse;
import igrus.web.security.auth.password.service.auth.LoginService;
import igrus.web.security.auth.password.service.auth.LogoutService;
import igrus.web.security.auth.password.service.auth.RefreshTokenService;
import igrus.web.security.auth.password.service.presignup.PreSignupSendCodeService;
import igrus.web.security.auth.password.service.presignup.PreSignupVerifyCodeService;
import igrus.web.security.auth.password.service.reset.RequestPasswordResetService;
import igrus.web.security.auth.password.service.reset.ResetPasswordService;
import igrus.web.security.auth.password.service.reset.ValidateResetTokenService;
import igrus.web.security.auth.password.service.signup.CheckDuplicateService;
import igrus.web.security.auth.password.service.signup.SignupService;
import igrus.web.security.auth.password.service.signup.TempStudentIdSignupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PasswordAuthController implements PasswordAuthenticationApi {

    private final LoginService loginService;
    private final LogoutService logoutService;
    private final RefreshTokenService refreshTokenService;
    private final SignupService signupService;
    private final CheckDuplicateService checkDuplicateService;
    private final PreSignupSendCodeService preSignupSendCodeService;
    private final PreSignupVerifyCodeService preSignupVerifyCodeService;
    private final RequestPasswordResetService requestPasswordResetService;
    private final ResetPasswordService resetPasswordService;
    private final ValidateResetTokenService validateResetTokenService;
    private final CheckReRegistrationEligibilityService checkReRegistrationEligibilityService;
    private final CheckRecoveryEligibilityService checkRecoveryEligibilityService;
    private final RecoverAccountService recoverAccountService;
    private final TempStudentIdSignupService tempStudentIdSignupService;
    private final CookieUtil cookieUtil;

    @Override
    public ResponseEntity<Login200Response> login(LoginRequest loginRequest) {
        HttpServletRequest httpRequest = ServletContextUtil.getCurrentRequest();
        HttpServletResponse httpResponse = ServletContextUtil.getCurrentResponse();

        String ipAddress = ServletContextUtil.extractIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        PasswordLoginRequest request = new PasswordLoginRequest(
                loginRequest.getStudentId(),
                loginRequest.getPassword()
        );

        LoginResult result = loginService.login(request, ipAddress, userAgent);

        ResponseCookie cookie = cookieUtil.createRefreshTokenCookie(
                result.refreshToken(),
                Duration.ofMillis(result.refreshTokenValidity())
        );
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        PasswordLoginResponse response = result.toResponse();
        return ResponseEntity.ok(new Login200Response()
                .accessToken(response.accessToken())
                .userId(response.userId())
                .studentId(response.studentId())
                .name(response.name())
                .role(Login200Response.RoleEnum.fromValue(response.role().name()))
                .expiresIn(response.expiresIn()));
    }

    @Override
    public ResponseEntity<Void> logout() {
        HttpServletRequest httpRequest = ServletContextUtil.getCurrentRequest();
        HttpServletResponse httpResponse = ServletContextUtil.getCurrentResponse();

        String refreshToken = cookieUtil.getRefreshTokenFromCookies(httpRequest)
                .orElseThrow(RefreshTokenInvalidException::new);

        logoutService.logout(refreshToken);

        ResponseCookie deleteCookie = cookieUtil.deleteRefreshTokenCookie();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<RefreshToken200Response> refreshToken() {
        HttpServletRequest httpRequest = ServletContextUtil.getCurrentRequest();
        HttpServletResponse httpResponse = ServletContextUtil.getCurrentResponse();

        String refreshToken = cookieUtil.getRefreshTokenFromCookies(httpRequest)
                .orElseThrow(RefreshTokenInvalidException::new);

        TokenRotationResult result;
        try {
            result = refreshTokenService.refreshToken(refreshToken);
        } catch (RefreshTokenTheftException | RefreshTokenExpiredException e) {
            ResponseCookie deleteCookie = cookieUtil.deleteRefreshTokenCookie();
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
            throw e;
        }

        // 새 리프레시 토큰이 있을 때만 Set-Cookie 설정 (Grace Period 시 null)
        if (result.newRefreshToken() != null) {
            ResponseCookie cookie = cookieUtil.createRefreshTokenCookie(
                    result.newRefreshToken(),
                    Duration.ofMillis(result.refreshTokenValidity())
            );
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        TokenRefreshResponse response = result.toResponse();
        return ResponseEntity.ok(new RefreshToken200Response()
                .accessToken(response.accessToken())
                .expiresIn(response.expiresIn()));
    }

    @Override
    public ResponseEntity<Signup201Response> signup(SignupRequest signupRequest) {
        validatePrivacyConsent(signupRequest.getPrivacyConsent());
        PasswordSignupRequest request = new PasswordSignupRequest(
                signupRequest.getStudentId(),
                signupRequest.getName(),
                signupRequest.getEmail(),
                signupRequest.getPassword(),
                signupRequest.getPhoneNumber(),
                signupRequest.getDepartment(),
                signupRequest.getMotivation(),
                signupRequest.getWishes() != null
                        ? signupRequest.getWishes().stream()
                                .map(w -> igrus.web.user.domain.Wish.valueOf(w.getValue()))
                                .toList()
                        : null,
                signupRequest.getInterests() != null
                        ? signupRequest.getInterests().stream()
                                .map(i -> igrus.web.user.domain.Interest.valueOf(i.getValue()))
                                .toList()
                        : null,
                signupRequest.getCustomInterest(),
                signupRequest.getJoinRoute() != null
                        ? igrus.web.user.domain.JoinRoute.valueOf(signupRequest.getJoinRoute().getValue()) : null,
                signupRequest.getCustomJoinRoute(),
                signupRequest.getGender() != null
                        ? igrus.web.user.domain.Gender.valueOf(signupRequest.getGender().getValue()) : null,
                signupRequest.getGrade(),
                signupRequest.getEnrollmentStatus() != null
                        ? igrus.web.user.domain.EnrollmentStatus.valueOf(signupRequest.getEnrollmentStatus().getValue()) : null,
                Boolean.TRUE.equals(signupRequest.getPrivacyConsent()),
                signupRequest.getVerificationToken()
        );

        PasswordSignupResponse response = signupService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Signup201Response()
                .message(response.message())
                .email(response.email())
                .temporaryStudentId(response.temporaryStudentId()));
    }

    @Override
    public ResponseEntity<Signup201Response> signupWithTemporaryStudentId(
            SignupWithTemporaryStudentIdRequest signupWithTemporaryStudentIdRequest
    ) {
        validatePrivacyConsent(signupWithTemporaryStudentIdRequest.getPrivacyConsent());
        TemporaryStudentIdSignupRequest request = new TemporaryStudentIdSignupRequest(
                signupWithTemporaryStudentIdRequest.getName(),
                signupWithTemporaryStudentIdRequest.getEmail(),
                signupWithTemporaryStudentIdRequest.getPassword(),
                signupWithTemporaryStudentIdRequest.getPhoneNumber(),
                signupWithTemporaryStudentIdRequest.getDepartment(),
                signupWithTemporaryStudentIdRequest.getMotivation(),
                signupWithTemporaryStudentIdRequest.getWishes() != null
                        ? signupWithTemporaryStudentIdRequest.getWishes().stream()
                                .map(w -> igrus.web.user.domain.Wish.valueOf(w.getValue()))
                                .toList()
                        : null,
                signupWithTemporaryStudentIdRequest.getInterests() != null
                        ? signupWithTemporaryStudentIdRequest.getInterests().stream()
                                .map(i -> igrus.web.user.domain.Interest.valueOf(i.getValue()))
                                .toList()
                        : null,
                signupWithTemporaryStudentIdRequest.getCustomInterest(),
                signupWithTemporaryStudentIdRequest.getJoinRoute() != null
                        ? igrus.web.user.domain.JoinRoute.valueOf(signupWithTemporaryStudentIdRequest.getJoinRoute().getValue()) : null,
                signupWithTemporaryStudentIdRequest.getCustomJoinRoute(),
                signupWithTemporaryStudentIdRequest.getGender() != null
                        ? igrus.web.user.domain.Gender.valueOf(signupWithTemporaryStudentIdRequest.getGender().getValue()) : null,
                signupWithTemporaryStudentIdRequest.getGrade(),
                signupWithTemporaryStudentIdRequest.getEnrollmentStatus() != null
                        ? igrus.web.user.domain.EnrollmentStatus.valueOf(signupWithTemporaryStudentIdRequest.getEnrollmentStatus().getValue()) : null,
                Boolean.TRUE.equals(signupWithTemporaryStudentIdRequest.getPrivacyConsent()),
                signupWithTemporaryStudentIdRequest.getVerificationToken()
        );

        PasswordSignupResponse response = tempStudentIdSignupService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Signup201Response()
                .message(response.message())
                .email(response.email())
                .temporaryStudentId(response.temporaryStudentId()));
    }

    @Override
    public ResponseEntity<CheckStudentIdDuplicate200Response> checkStudentIdDuplicate(String studentId) {
        DuplicateCheckResponse response = checkDuplicateService.checkStudentId(studentId);
        return ResponseEntity.ok(new CheckStudentIdDuplicate200Response()
                .available(response.available())
                .message(response.message()));
    }

    @Override
    public ResponseEntity<CheckStudentIdDuplicate200Response> checkEmailDuplicate(String email) {
        DuplicateCheckResponse response = checkDuplicateService.checkEmail(email);
        return ResponseEntity.ok(new CheckStudentIdDuplicate200Response()
                .available(response.available())
                .message(response.message()));
    }

    @Override
    public ResponseEntity<CheckStudentIdDuplicate200Response> checkPhoneNumberDuplicate(String phoneNumber) {
        DuplicateCheckResponse response = checkDuplicateService.checkPhoneNumber(phoneNumber);
        return ResponseEntity.ok(new CheckStudentIdDuplicate200Response()
                .available(response.available())
                .message(response.message()));
    }

    @Override
    public ResponseEntity<SendPreSignupCode200Response> sendPreSignupCode(
            SendPreSignupCodeRequest sendPreSignupCodeRequest
    ) {
        igrus.web.security.auth.common.dto.request.ResendVerificationRequest request =
                new igrus.web.security.auth.common.dto.request.ResendVerificationRequest(
                        sendPreSignupCodeRequest.getEmail()
                );

        VerificationResendResponse response = preSignupSendCodeService.sendCode(request);
        return ResponseEntity.ok(new SendPreSignupCode200Response()
                .message(response.message())
                .email(response.email()));
    }

    @Override
    public ResponseEntity<VerifyPreSignupCode200Response> verifyPreSignupCode(
            VerifyEmailChangeRequest verifyEmailChangeRequest
    ) {
        igrus.web.security.auth.common.dto.request.EmailVerificationRequest request =
                new igrus.web.security.auth.common.dto.request.EmailVerificationRequest(
                        verifyEmailChangeRequest.getEmail(),
                        verifyEmailChangeRequest.getCode()
                );

        PreSignupVerificationResponse response = preSignupVerifyCodeService.verifyCode(request);
        return ResponseEntity.ok(new VerifyPreSignupCode200Response()
                .message(response.message())
                .email(response.email())
                .verified(response.verified())
                .verificationToken(response.verificationToken()));
    }

    @Override
    public ResponseEntity<CheckReRegistrationEligibility200Response> checkReRegistrationEligibility(
            String studentId
    ) {
        ReRegistrationCheckResult result = checkReRegistrationEligibilityService.checkReRegistrationEligibility(studentId);
        return ResponseEntity.ok(new CheckReRegistrationEligibility200Response()
                .isEligible(result.isEligible())
                .isAlreadyRegistered(result.isAlreadyRegistered())
                .reRegistrationAvailableAt(result.reRegistrationAvailableAt())
                .message(result.message()));
    }

    @Override
    public ResponseEntity<CheckRecoveryEligibility200Response> checkRecoveryEligibility(String studentId) {
        RecoveryEligibilityResponse response = checkRecoveryEligibilityService.checkRecoveryEligibility(studentId);
        return ResponseEntity.ok(new CheckRecoveryEligibility200Response()
                .recoverable(response.recoverable())
                .recoveryDeadline(response.recoveryDeadline())
                .message(response.message()));
    }

    @Override
    public ResponseEntity<RecoverAccount200Response> recoverAccount(
            RecoverAccountRequest recoverAccountRequest
    ) {
        HttpServletResponse httpResponse = ServletContextUtil.getCurrentResponse();

        RecoveryResult result = recoverAccountService.recoverAccount(
                recoverAccountRequest.getStudentId(), recoverAccountRequest.getPassword());

        ResponseCookie cookie = cookieUtil.createRefreshTokenCookie(
                result.refreshToken(),
                Duration.ofMillis(result.refreshTokenValidity())
        );
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        AccountRecoveryResponse response = result.toResponse();
        return ResponseEntity.ok(new RecoverAccount200Response()
                .accessToken(response.accessToken())
                .userId(response.userId())
                .studentId(response.studentId())
                .name(response.name())
                .role(RecoverAccount200Response.RoleEnum.fromValue(response.role().name()))
                .expiresIn(response.expiresIn())
                .message(response.message()));
    }

    @Override
    public ResponseEntity<Void> requestPasswordReset(
            RequestPasswordResetRequest requestPasswordResetRequest
    ) {
        requestPasswordResetService.requestPasswordReset(requestPasswordResetRequest.getStudentId());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> confirmPasswordReset(
            ConfirmPasswordResetRequest confirmPasswordResetRequest
    ) {
        resetPasswordService.resetPassword(
                confirmPasswordResetRequest.getToken(),
                confirmPasswordResetRequest.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> validateResetToken(String token) {
        validateResetTokenService.validateResetToken(token);
        return ResponseEntity.ok().build();
    }

    /**
     * OpenAPI 스펙에서 boolean 필드에 대한 {@code @AssertTrue}를 표현할 수 없으므로
     * 컨트롤러에서 직접 검증한다.
     */
    private void validatePrivacyConsent(Boolean privacyConsent) {
        if (!Boolean.TRUE.equals(privacyConsent)) {
            throw new CustomBaseException(CommonErrorCode.INVALID_INPUT_VALUE) {};
        }
    }
}
