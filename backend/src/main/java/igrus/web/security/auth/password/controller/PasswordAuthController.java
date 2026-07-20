package igrus.web.security.auth.password.controller;

import igrus.web.common.exception.CommonErrorCode;
import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.util.EnumUtils;
import igrus.web.common.util.ServletContextUtil;
import igrus.web.generated.api.PasswordAuthenticationApi;
import igrus.web.generated.model.ApiAccountRecoveryResponse;
import igrus.web.generated.model.ApiDuplicateCheckResponse;
import igrus.web.generated.model.ApiPasswordLoginResponse;
import igrus.web.generated.model.ApiPasswordSignupResponse;
import igrus.web.generated.model.ApiPreSignupVerificationResponse;
import igrus.web.generated.model.ApiReRegistrationCheckResult;
import igrus.web.generated.model.ApiRecoveryEligibilityResponse;
import igrus.web.generated.model.ApiTokenRefreshResponse;
import igrus.web.generated.model.ApiVerificationResendResponse;
import igrus.web.generated.model.ApiPasswordLoginRequest;
import igrus.web.generated.model.ApiPasswordSignupRequest;
import igrus.web.generated.model.ApiTemporaryStudentIdSignupRequest;
import igrus.web.generated.model.ApiResendVerificationRequest;
import igrus.web.generated.model.ApiEmailVerificationRequest;
import igrus.web.generated.model.ApiAccountRecoveryRequest;
import igrus.web.generated.model.ApiPasswordResetRequest;
import igrus.web.generated.model.ApiPasswordResetConfirmRequest;
import igrus.web.security.auth.common.dto.internal.RecoveryResult;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.dto.request.ResendVerificationRequest;
import igrus.web.security.auth.password.dto.request.PasswordLoginRequest;
import igrus.web.security.auth.password.dto.request.PasswordSignupRequest;
import igrus.web.user.dto.ProfileLinkMapper;
import igrus.web.security.auth.password.dto.request.TemporaryStudentIdSignupRequest;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.Interest;
import igrus.web.user.domain.JoinRoute;
import igrus.web.user.domain.Wish;
import igrus.web.security.auth.common.exception.token.RefreshTokenExpiredException;
import igrus.web.security.auth.common.exception.token.RefreshTokenInvalidException;
import igrus.web.security.auth.common.exception.token.RefreshTokenTheftException;
import igrus.web.security.auth.common.service.account.CheckReRegistrationEligibilityService;
import igrus.web.security.auth.common.service.account.CheckRecoveryEligibilityService;
import igrus.web.security.auth.common.service.account.RecoverAccountService;
import igrus.web.security.auth.common.util.CookieUtil;
import igrus.web.security.auth.password.dto.internal.LoginResult;
import igrus.web.security.auth.password.dto.internal.TokenRotationResult;
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
    public ResponseEntity<ApiPasswordLoginResponse> login(ApiPasswordLoginRequest passwordLoginRequest) {
        HttpServletRequest httpRequest = ServletContextUtil.getCurrentRequest();
        HttpServletResponse httpResponse = ServletContextUtil.getCurrentResponse();

        String ipAddress = ServletContextUtil.extractIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        var request = new PasswordLoginRequest(
                passwordLoginRequest.getStudentId(),
                passwordLoginRequest.getPassword()
        );

        LoginResult result = loginService.login(request, ipAddress, userAgent);

        ResponseCookie cookie = cookieUtil.createRefreshTokenCookie(
                result.refreshToken(),
                Duration.ofMillis(result.refreshTokenValidity())
        );
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        var response = result.toResponse();
        return ResponseEntity.ok(new ApiPasswordLoginResponse()
                .accessToken(response.accessToken())
                .userId(response.userId())
                .studentId(response.studentId())
                .name(response.name())
                .role(ApiPasswordLoginResponse.RoleEnum.fromValue(response.role().name()))
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
    public ResponseEntity<ApiTokenRefreshResponse> refreshToken() {
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

        var response = result.toResponse();
        return ResponseEntity.ok(new ApiTokenRefreshResponse()
                .accessToken(response.accessToken())
                .expiresIn(response.expiresIn()));
    }

    @Override
    public ResponseEntity<ApiPasswordSignupResponse> signup(ApiPasswordSignupRequest passwordSignupRequest) {
        validatePrivacyConsent(passwordSignupRequest.getPrivacyConsent());
        var request = new PasswordSignupRequest(
                passwordSignupRequest.getStudentId(),
                passwordSignupRequest.getName(),
                passwordSignupRequest.getEmail(),
                passwordSignupRequest.getPassword(),
                passwordSignupRequest.getPhoneNumber(),
                passwordSignupRequest.getDepartment(),
                passwordSignupRequest.getMotivation(),
                passwordSignupRequest.getWishes() != null
                        ? passwordSignupRequest.getWishes().stream()
                                .map(w -> EnumUtils.fromStringOrNull(Wish.class, w.getValue()))
                                .toList()
                        : null,
                passwordSignupRequest.getInterests() != null
                        ? passwordSignupRequest.getInterests().stream()
                                .map(i -> EnumUtils.fromStringOrNull(Interest.class, i.getValue()))
                                .toList()
                        : null,
                passwordSignupRequest.getCustomInterest(),
                passwordSignupRequest.getJoinRoute() != null
                        ? EnumUtils.fromStringOrNull(JoinRoute.class, passwordSignupRequest.getJoinRoute().getValue()) : null,
                passwordSignupRequest.getCustomJoinRoute(),
                passwordSignupRequest.getGender() != null
                        ? EnumUtils.fromStringOrNull(Gender.class, passwordSignupRequest.getGender().getValue()) : null,
                passwordSignupRequest.getGrade(),
                passwordSignupRequest.getEnrollmentStatus() != null
                        ? EnumUtils.fromStringOrNull(EnrollmentStatus.class, passwordSignupRequest.getEnrollmentStatus().getValue()) : null,
                Boolean.TRUE.equals(passwordSignupRequest.getPrivacyConsent()),
                passwordSignupRequest.getVerificationToken()
        );

        var response = signupService.signup(request,
                passwordSignupRequest.getNickname(),
                passwordSignupRequest.getIntroduction(),
                ProfileLinkMapper.fromApi(passwordSignupRequest.getLinks()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiPasswordSignupResponse()
                .message(response.message())
                .email(response.email())
                .temporaryStudentId(response.temporaryStudentId()));
    }

    @Override
    public ResponseEntity<ApiPasswordSignupResponse> signupWithTemporaryStudentId(
            ApiTemporaryStudentIdSignupRequest temporaryStudentIdSignupRequest
    ) {
        validatePrivacyConsent(temporaryStudentIdSignupRequest.getPrivacyConsent());
        var request = new TemporaryStudentIdSignupRequest(
                temporaryStudentIdSignupRequest.getName(),
                temporaryStudentIdSignupRequest.getEmail(),
                temporaryStudentIdSignupRequest.getPassword(),
                temporaryStudentIdSignupRequest.getPhoneNumber(),
                temporaryStudentIdSignupRequest.getDepartment(),
                temporaryStudentIdSignupRequest.getMotivation(),
                temporaryStudentIdSignupRequest.getWishes() != null
                        ? temporaryStudentIdSignupRequest.getWishes().stream()
                                .map(w -> EnumUtils.fromStringOrNull(Wish.class, w.getValue()))
                                .toList()
                        : null,
                temporaryStudentIdSignupRequest.getInterests() != null
                        ? temporaryStudentIdSignupRequest.getInterests().stream()
                                .map(i -> EnumUtils.fromStringOrNull(Interest.class, i.getValue()))
                                .toList()
                        : null,
                temporaryStudentIdSignupRequest.getCustomInterest(),
                temporaryStudentIdSignupRequest.getJoinRoute() != null
                        ? EnumUtils.fromStringOrNull(JoinRoute.class, temporaryStudentIdSignupRequest.getJoinRoute().getValue()) : null,
                temporaryStudentIdSignupRequest.getCustomJoinRoute(),
                temporaryStudentIdSignupRequest.getGender() != null
                        ? EnumUtils.fromStringOrNull(Gender.class, temporaryStudentIdSignupRequest.getGender().getValue()) : null,
                temporaryStudentIdSignupRequest.getGrade(),
                temporaryStudentIdSignupRequest.getEnrollmentStatus() != null
                        ? EnumUtils.fromStringOrNull(EnrollmentStatus.class, temporaryStudentIdSignupRequest.getEnrollmentStatus().getValue()) : null,
                Boolean.TRUE.equals(temporaryStudentIdSignupRequest.getPrivacyConsent()),
                temporaryStudentIdSignupRequest.getVerificationToken()
        );

        var response = tempStudentIdSignupService.signup(request,
                temporaryStudentIdSignupRequest.getNickname(),
                temporaryStudentIdSignupRequest.getIntroduction(),
                ProfileLinkMapper.fromApi(temporaryStudentIdSignupRequest.getLinks()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiPasswordSignupResponse()
                .message(response.message())
                .email(response.email())
                .temporaryStudentId(response.temporaryStudentId()));
    }

    @Override
    public ResponseEntity<ApiDuplicateCheckResponse> checkStudentIdDuplicate(String studentId) {
        var response = checkDuplicateService.checkStudentId(studentId);
        return ResponseEntity.ok(new ApiDuplicateCheckResponse()
                .available(response.available())
                .message(response.message()));
    }

    @Override
    public ResponseEntity<ApiDuplicateCheckResponse> checkEmailDuplicate(String email) {
        var response = checkDuplicateService.checkEmail(email);
        return ResponseEntity.ok(new ApiDuplicateCheckResponse()
                .available(response.available())
                .message(response.message()));
    }

    @Override
    public ResponseEntity<ApiDuplicateCheckResponse> checkPhoneNumberDuplicate(String phoneNumber) {
        var response = checkDuplicateService.checkPhoneNumber(phoneNumber);
        return ResponseEntity.ok(new ApiDuplicateCheckResponse()
                .available(response.available())
                .message(response.message()));
    }

    @Override
    public ResponseEntity<ApiVerificationResendResponse> sendPreSignupCode(
            ApiResendVerificationRequest resendVerificationRequest
    ) {
        var request =
                new ResendVerificationRequest(
                        resendVerificationRequest.getEmail()
                );

        var response = preSignupSendCodeService.sendCode(request);
        return ResponseEntity.ok(new ApiVerificationResendResponse()
                .message(response.message())
                .email(response.email()));
    }

    @Override
    public ResponseEntity<ApiPreSignupVerificationResponse> verifyPreSignupCode(
            ApiEmailVerificationRequest emailVerificationRequest
    ) {
        var request =
                new EmailVerificationRequest(
                        emailVerificationRequest.getEmail(),
                        emailVerificationRequest.getCode()
                );

        var response = preSignupVerifyCodeService.verifyCode(request);
        return ResponseEntity.ok(new ApiPreSignupVerificationResponse()
                .message(response.message())
                .email(response.email())
                .verified(response.verified())
                .verificationToken(response.verificationToken()));
    }

    @Override
    public ResponseEntity<ApiReRegistrationCheckResult> checkReRegistrationEligibility(
            String studentId
    ) {
        var result = checkReRegistrationEligibilityService.checkReRegistrationEligibility(studentId);
        return ResponseEntity.ok(new ApiReRegistrationCheckResult()
                .isEligible(result.isEligible())
                .isAlreadyRegistered(result.isAlreadyRegistered())
                .reRegistrationAvailableAt(result.reRegistrationAvailableAt())
                .message(result.message()));
    }

    @Override
    public ResponseEntity<ApiRecoveryEligibilityResponse> checkRecoveryEligibility(String studentId) {
        var response = checkRecoveryEligibilityService.checkRecoveryEligibility(studentId);
        return ResponseEntity.ok(new ApiRecoveryEligibilityResponse()
                .recoverable(response.recoverable())
                .recoveryDeadline(response.recoveryDeadline())
                .message(response.message()));
    }

    @Override
    public ResponseEntity<ApiAccountRecoveryResponse> recoverAccount(
            ApiAccountRecoveryRequest accountRecoveryRequest
    ) {
        HttpServletResponse httpResponse = ServletContextUtil.getCurrentResponse();

        RecoveryResult result = recoverAccountService.recoverAccount(
                accountRecoveryRequest.getStudentId(), accountRecoveryRequest.getPassword());

        ResponseCookie cookie = cookieUtil.createRefreshTokenCookie(
                result.refreshToken(),
                Duration.ofMillis(result.refreshTokenValidity())
        );
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        var response = result.toResponse();
        return ResponseEntity.ok(new ApiAccountRecoveryResponse()
                .accessToken(response.accessToken())
                .userId(response.userId())
                .studentId(response.studentId())
                .name(response.name())
                .role(ApiAccountRecoveryResponse.RoleEnum.fromValue(response.role().name()))
                .expiresIn(response.expiresIn())
                .message(response.message()));
    }

    @Override
    public ResponseEntity<Void> requestPasswordReset(
            ApiPasswordResetRequest passwordResetRequest
    ) {
        requestPasswordResetService.requestPasswordReset(passwordResetRequest.getStudentId());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> confirmPasswordReset(
            ApiPasswordResetConfirmRequest passwordResetConfirmRequest
    ) {
        resetPasswordService.resetPassword(
                passwordResetConfirmRequest.getToken(),
                passwordResetConfirmRequest.getNewPassword());
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
