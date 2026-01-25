package igrus.web.security.auth.password.controller;

import igrus.web.security.auth.common.dto.request.AccountRecoveryRequest;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.dto.request.ResendVerificationRequest;
import igrus.web.security.auth.common.dto.response.AccountRecoveryResponse;
import igrus.web.security.auth.common.dto.response.RecoveryEligibilityResponse;
import igrus.web.security.auth.common.service.AccountRecoveryService;
import igrus.web.security.auth.password.dto.request.PasswordLoginRequest;
import igrus.web.security.auth.password.dto.request.PasswordLogoutRequest;
import igrus.web.security.auth.password.dto.request.PasswordResetConfirmRequest;
import igrus.web.security.auth.password.dto.request.PasswordResetRequest;
import igrus.web.security.auth.password.dto.request.PasswordSignupRequest;
import igrus.web.security.auth.password.dto.request.TokenRefreshRequest;
import igrus.web.security.auth.password.dto.response.PasswordLoginResponse;
import igrus.web.security.auth.password.dto.response.PasswordSignupResponse;
import igrus.web.security.auth.password.dto.response.TokenRefreshResponse;
import igrus.web.security.auth.password.service.PasswordAuthService;
import igrus.web.security.auth.password.service.PasswordResetService;
import igrus.web.security.auth.password.service.PasswordSignupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password")
@RequiredArgsConstructor
public class PasswordAuthController implements PasswordAuthControllerApi {

    private final PasswordAuthService passwordAuthService;
    private final PasswordSignupService passwordSignupService;
    private final PasswordResetService passwordResetService;
    private final AccountRecoveryService accountRecoveryService;

    @Override
    @PostMapping("/login")
    public ResponseEntity<PasswordLoginResponse> login(@Valid @RequestBody PasswordLoginRequest request) {
        PasswordLoginResponse response = passwordAuthService.login(request);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody PasswordLogoutRequest request) {
        passwordAuthService.logout(request);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = passwordAuthService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/signup")
    public ResponseEntity<PasswordSignupResponse> signup(@Valid @RequestBody PasswordSignupRequest request) {
        PasswordSignupResponse response = passwordSignupService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PostMapping("/verify-email")
    public ResponseEntity<PasswordSignupResponse> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        PasswordSignupResponse response = passwordSignupService.verifyEmail(request);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/resend-verification")
    public ResponseEntity<PasswordSignupResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        PasswordSignupResponse response = passwordSignupService.resendVerification(request);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/account/recovery-check")
    public ResponseEntity<RecoveryEligibilityResponse> checkRecoveryEligibility(
            @RequestParam @Pattern(regexp = "^\\d{8}$", message = "학번은 8자리 숫자여야 합니다") String studentId) {
        RecoveryEligibilityResponse response = accountRecoveryService.checkRecoveryEligibility(studentId);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/account/recover")
    public ResponseEntity<AccountRecoveryResponse> recoverAccount(@Valid @RequestBody AccountRecoveryRequest request) {
        AccountRecoveryResponse response = accountRecoveryService.recoverAccount(request.studentId(), request.password());
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/reset-request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestPasswordReset(request.studentId());
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/reset-confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping("/reset-validate")
    public ResponseEntity<Void> validateResetToken(@RequestParam String token) {
        passwordResetService.validateResetToken(token);
        return ResponseEntity.ok().build();
    }
}
