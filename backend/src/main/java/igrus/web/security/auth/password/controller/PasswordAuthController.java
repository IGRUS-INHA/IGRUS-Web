package igrus.web.security.auth.password.controller;

import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.dto.request.ResendVerificationRequest;
import igrus.web.security.auth.password.dto.request.PasswordLoginRequest;
import igrus.web.security.auth.password.dto.request.PasswordLogoutRequest;
import igrus.web.security.auth.password.dto.request.PasswordSignupRequest;
import igrus.web.security.auth.password.dto.request.TokenRefreshRequest;
import igrus.web.security.auth.password.dto.response.PasswordLoginResponse;
import igrus.web.security.auth.password.dto.response.PasswordSignupResponse;
import igrus.web.security.auth.password.dto.response.TokenRefreshResponse;
import igrus.web.security.auth.password.service.PasswordAuthService;
import igrus.web.security.auth.password.service.PasswordSignupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password")
@RequiredArgsConstructor
public class PasswordAuthController implements PasswordAuthControllerApi {

    private final PasswordAuthService passwordAuthService;
    private final PasswordSignupService passwordSignupService;

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
}
