package igrus.web.security.auth.password.controller;

import igrus.web.security.auth.common.dto.internal.RecoveryResult;
import igrus.web.security.auth.common.dto.request.AccountRecoveryRequest;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.dto.request.ResendVerificationRequest;
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
import igrus.web.security.auth.password.dto.request.PasswordLoginRequest;
import igrus.web.security.auth.password.dto.request.PasswordResetConfirmRequest;
import igrus.web.security.auth.password.dto.request.PasswordResetRequest;
import igrus.web.security.auth.password.dto.request.PasswordSignupRequest;
import igrus.web.security.auth.password.dto.request.TemporaryStudentIdSignupRequest;
import igrus.web.security.auth.password.dto.response.DuplicateCheckResponse;
import igrus.web.security.auth.password.dto.response.PasswordLoginResponse;
import igrus.web.security.auth.password.dto.response.PasswordSignupResponse;
import igrus.web.security.auth.password.dto.internal.TokenRotationResult;
import igrus.web.security.auth.password.dto.response.TokenRefreshResponse;
import igrus.web.security.auth.password.dto.response.VerificationResendResponse;
import igrus.web.security.auth.password.service.reset.RequestPasswordResetService;
import igrus.web.security.auth.password.service.reset.ResetPasswordService;
import igrus.web.security.auth.password.service.reset.ValidateResetTokenService;
import igrus.web.security.auth.password.service.auth.LoginService;
import igrus.web.security.auth.password.service.auth.LogoutService;
import igrus.web.security.auth.password.service.auth.RefreshTokenService;
import igrus.web.security.auth.password.service.signup.CheckDuplicateService;
import igrus.web.security.auth.password.service.signup.ResendVerificationService;
import igrus.web.security.auth.password.service.signup.SignupService;
import igrus.web.security.auth.password.service.signup.TempStudentIdSignupService;
import igrus.web.security.auth.password.service.signup.VerifyEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/password")
@RequiredArgsConstructor
@Validated
@Tag(name = "Password Authentication", description = "비밀번호 기반 인증 관련 API")
public class PasswordAuthController {

    private final LoginService loginService;
    private final LogoutService logoutService;
    private final RefreshTokenService refreshTokenService;
    private final SignupService signupService;
    private final CheckDuplicateService checkDuplicateService;
    private final VerifyEmailService verifyEmailService;
    private final ResendVerificationService resendVerificationService;
    private final RequestPasswordResetService requestPasswordResetService;
    private final ResetPasswordService resetPasswordService;
    private final ValidateResetTokenService validateResetTokenService;
    private final CheckReRegistrationEligibilityService checkReRegistrationEligibilityService;
    private final CheckRecoveryEligibilityService checkRecoveryEligibilityService;
    private final RecoverAccountService recoverAccountService;
    private final TempStudentIdSignupService tempStudentIdSignupService;
    private final CookieUtil cookieUtil;

    @Operation(summary = "로그인", description = "학번과 비밀번호로 로그인합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (학번 또는 비밀번호 불일치)"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "계정 정지 또는 탈퇴 상태"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "이메일 인증 미완료"
            )
    })
    @PostMapping("/login")
    public ResponseEntity<PasswordLoginResponse> login(
            @Valid @RequestBody PasswordLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String ipAddress = extractIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResult result = loginService.login(request, ipAddress, userAgent);

        ResponseCookie cookie = cookieUtil.createRefreshTokenCookie(
                result.refreshToken(),
                Duration.ofMillis(result.refreshTokenValidity())
        );
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(result.toResponse());
    }

    /**
     * 클라이언트의 실제 IP 주소를 추출합니다.
     * 프록시/로드밸런서를 고려하여 X-Forwarded-For 헤더를 우선 확인합니다.
     */
    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    @Operation(summary = "로그아웃", description = "리프레시 토큰을 무효화하여 로그아웃합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 리프레시 토큰"
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String refreshToken = cookieUtil.getRefreshTokenFromCookies(httpRequest)
                .orElseThrow(RefreshTokenInvalidException::new);

        logoutService.logout(refreshToken);

        ResponseCookie deleteCookie = cookieUtil.deleteRefreshTokenCookie();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "토큰 갱신",
            description = "리프레시 토큰으로 새로운 액세스 토큰을 발급합니다. " +
                    "토큰 로테이션이 적용되어 매 갱신마다 새 리프레시 토큰이 Set-Cookie로 발급됩니다. " +
                    "Grace Period(10초) 내 중복 요청 시에는 액세스 토큰만 갱신됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "토큰 갱신 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 리프레시 토큰, 또는 토큰 탈취 감지"
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
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

        return ResponseEntity.ok(result.toResponse());
    }

    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다. 등록 후 이메일 인증이 필요합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "회원가입 요청 성공 (이메일 인증 대기)"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패)"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "중복된 학번, 이메일 또는 전화번호"
            )
    })
    @PostMapping("/signup")
    public ResponseEntity<PasswordSignupResponse> signup(@Valid @RequestBody PasswordSignupRequest request) {
        PasswordSignupResponse response = signupService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "임시 학번 회원가입", description = "1~2월에 1학년 신입생이 임시 학번으로 회원가입합니다. 임시 학번이 자동 발급되어 이메일로 전송됩니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "회원가입 요청 성공 (이메일 인증 대기, 임시 학번 발급)"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패, 1~2월이 아닌 경우, 1학년이 아닌 경우)"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "중복된 이메일 또는 전화번호"
            )
    })
    @PostMapping("/signup/temporary")
    public ResponseEntity<PasswordSignupResponse> signupWithTemporaryStudentId(
            @Valid @RequestBody TemporaryStudentIdSignupRequest request) {
        PasswordSignupResponse response = tempStudentIdSignupService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "학번 중복 체크",
            description = "학번의 유효성 검사 및 중복 여부를 확인합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "사용 가능한 학번"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "학번 형식 오류 (8자리 숫자가 아님)"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 가입된 학번"
            )
    })
    @GetMapping("/check-student-id")
    public ResponseEntity<DuplicateCheckResponse> checkStudentIdDuplicate(
            @Parameter(description = "확인할 학번 (8자리 숫자)", example = "12345678", required = true)
            @RequestParam String studentId) {
        DuplicateCheckResponse response = checkDuplicateService.checkStudentId(studentId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "이메일 중복 체크",
            description = "이메일의 유효성 검사 및 중복 여부를 확인합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "사용 가능한 이메일"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "이메일 형식 오류"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 존재하는 이메일"
            )
    })
    @GetMapping("/check-email")
    public ResponseEntity<DuplicateCheckResponse> checkEmailDuplicate(
            @Parameter(description = "확인할 이메일", example = "user@example.com", required = true)
            @RequestParam String email) {
        DuplicateCheckResponse response = checkDuplicateService.checkEmail(email);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "전화번호 중복 체크",
            description = "전화번호의 유효성 검사 및 중복 여부를 확인합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "사용 가능한 전화번호"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "전화번호 형식 오류 (XXX-XXXX-XXXX 형식이 아님)"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 등록된 전화번호"
            )
    })
    @GetMapping("/check-phone-number")
    public ResponseEntity<DuplicateCheckResponse> checkPhoneNumberDuplicate(
            @Parameter(description = "확인할 전화번호 (XXX-XXXX-XXXX)", example = "010-1234-5678", required = true)
            @RequestParam String phoneNumber) {
        DuplicateCheckResponse response = checkDuplicateService.checkPhoneNumber(phoneNumber);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "이메일 인증", description = "이메일로 발송된 인증 코드를 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "이메일 인증 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 인증 코드 또는 만료된 코드"
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "인증 시도 횟수 초과"
            )
    })
    @PostMapping("/verify-email")
    public ResponseEntity<PasswordSignupResponse> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        PasswordSignupResponse response = verifyEmailService.verifyEmail(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "인증 코드 재발송", description = "이메일 인증 코드를 다시 발송합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "인증 코드 재발송 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패 또는 해당 이메일로 가입 요청된 계정 없음)"
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "재발송 요청 횟수 초과 (5분 내 재요청 불가)"
            )
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<VerificationResendResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        VerificationResendResponse response = resendVerificationService.resendVerification(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "재가입 가능 여부 확인",
            description = "탈퇴 후 재가입 제한 기간(5일)이 지났는지 확인합니다. 회원가입 전 호출하여 재가입 가능 여부를 확인할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (학번 형식 오류)"
            )
    })
    @GetMapping("/account/reregistration-check")
    public ResponseEntity<ReRegistrationCheckResult> checkReRegistrationEligibility(
            @Parameter(description = "재가입 가능 여부를 확인할 학번 (8자리 숫자)", example = "12345678", required = true)
            @RequestParam @Pattern(regexp = "^\\d{8}$", message = "학번은 8자리 숫자여야 합니다") String studentId) {
        ReRegistrationCheckResult result = checkReRegistrationEligibilityService.checkReRegistrationEligibility(studentId);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "계정 복구 가능 여부 확인",
            description = "탈퇴한 계정의 복구 가능 여부를 확인합니다. 탈퇴 후 5일 이내에는 계정을 복구할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "복구 가능 여부 조회 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (학번 형식 오류)"
            )
    })
    @GetMapping("/account/recovery-check")
    public ResponseEntity<RecoveryEligibilityResponse> checkRecoveryEligibility(
            @Parameter(description = "복구 가능 여부를 확인할 학번 (8자리 숫자)", example = "12345678", required = true)
            @RequestParam @Pattern(regexp = "^\\d{8}$", message = "학번은 8자리 숫자여야 합니다") String studentId) {
        RecoveryEligibilityResponse response = checkRecoveryEligibilityService.checkRecoveryEligibility(studentId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "계정 복구",
            description = "탈퇴한 계정을 복구합니다. 탈퇴 후 5일 이내에만 가능하며, 학번과 비밀번호로 인증이 필요합니다. " +
                    "복구 성공 시 새로운 Access Token과 Refresh Token이 발급됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "계정 복구 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패) 또는 복구 기간 만료"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (학번 또는 비밀번호 불일치)"
            )
    })
    @PostMapping("/account/recover")
    public ResponseEntity<AccountRecoveryResponse> recoverAccount(
            @Valid @RequestBody AccountRecoveryRequest request,
            HttpServletResponse httpResponse) {
        RecoveryResult result = recoverAccountService.recoverAccount(request.studentId(), request.password());

        ResponseCookie cookie = cookieUtil.createRefreshTokenCookie(
                result.refreshToken(),
                Duration.ofMillis(result.refreshTokenValidity())
        );
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(result.toResponse());
    }

    @Operation(
            summary = "비밀번호 재설정 요청",
            description = "학번을 입력하여 비밀번호 재설정 링크를 이메일로 발송합니다. 보안상 존재하지 않는 학번도 동일한 응답을 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "요청 처리 완료 (이메일이 등록된 경우 재설정 링크 발송)"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (학번 형식 오류)"
            )
    })
    @PostMapping("/reset-request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        requestPasswordResetService.requestPasswordReset(request.studentId());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "비밀번호 재설정 확인",
            description = "재설정 토큰과 새 비밀번호를 입력하여 비밀번호를 변경합니다. 비밀번호는 영문 대/소문자, 숫자, 특수문자를 포함한 8자 이상이어야 합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "비밀번호 재설정 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패) 또는 유효하지 않은/만료된 토큰"
            )
    })
    @PostMapping("/reset-confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        resetPasswordService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "비밀번호 재설정 토큰 검증",
            description = "재설정 토큰의 유효성을 검증합니다. 토큰이 유효하고 만료되지 않았는지 확인합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "토큰 유효"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않거나 만료된 토큰"
            )
    })
    @GetMapping("/reset-validate")
    public ResponseEntity<Void> validateResetToken(
            @Parameter(description = "검증할 재설정 토큰", required = true)
            @RequestParam String token) {
        validateResetTokenService.validateResetToken(token);
        return ResponseEntity.ok().build();
    }
}
