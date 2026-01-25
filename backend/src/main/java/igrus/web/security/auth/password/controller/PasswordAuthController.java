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
import igrus.web.security.auth.password.dto.response.VerificationResendResponse;
import igrus.web.security.auth.password.service.PasswordAuthService;
import igrus.web.security.auth.password.service.PasswordResetService;
import igrus.web.security.auth.password.service.PasswordSignupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Validated
@Tag(name = "Password Authentication", description = "비밀번호 기반 인증 관련 API")
public class PasswordAuthController {

    private final PasswordAuthService passwordAuthService;
    private final PasswordSignupService passwordSignupService;
    private final PasswordResetService passwordResetService;
    private final AccountRecoveryService accountRecoveryService;

    @Operation(summary = "로그인", description = "학번과 비밀번호로 로그인합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = PasswordLoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (학번 또는 비밀번호 불일치)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "계정 정지 또는 탈퇴 상태",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "이메일 인증 미완료",
                    content = @Content
            )
    })
    @PostMapping("/login")
    public ResponseEntity<PasswordLoginResponse> login(@Valid @RequestBody PasswordLoginRequest request) {
        PasswordLoginResponse response = passwordAuthService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그아웃", description = "리프레시 토큰을 무효화하여 로그아웃합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 리프레시 토큰",
                    content = @Content
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody PasswordLogoutRequest request) {
        passwordAuthService.logout(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새로운 액세스 토큰을 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "토큰 갱신 성공",
                    content = @Content(schema = @Schema(implementation = TokenRefreshResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 리프레시 토큰",
                    content = @Content
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = passwordAuthService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다. 등록 후 이메일 인증이 필요합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "회원가입 요청 성공 (이메일 인증 대기)",
                    content = @Content(schema = @Schema(implementation = PasswordSignupResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "중복된 학번, 이메일 또는 전화번호",
                    content = @Content
            )
    })
    @PostMapping("/signup")
    public ResponseEntity<PasswordSignupResponse> signup(@Valid @RequestBody PasswordSignupRequest request) {
        PasswordSignupResponse response = passwordSignupService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "이메일 인증", description = "이메일로 발송된 인증 코드를 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "이메일 인증 성공",
                    content = @Content(schema = @Schema(implementation = PasswordSignupResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 인증 코드 또는 만료된 코드",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "인증 시도 횟수 초과",
                    content = @Content
            )
    })
    @PostMapping("/verify-email")
    public ResponseEntity<PasswordSignupResponse> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        PasswordSignupResponse response = passwordSignupService.verifyEmail(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "인증 코드 재발송", description = "이메일 인증 코드를 다시 발송합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "인증 코드 재발송 성공",
                    content = @Content(schema = @Schema(implementation = VerificationResendResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "재발송 요청 횟수 초과 (5분 내 재요청 불가)",
                    content = @Content
            )
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<VerificationResendResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        VerificationResendResponse response = passwordSignupService.resendVerification(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "계정 복구 가능 여부 확인",
            description = "탈퇴한 계정의 복구 가능 여부를 확인합니다. 탈퇴 후 5일 이내에는 계정을 복구할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "복구 가능 여부 조회 성공",
                    content = @Content(schema = @Schema(implementation = RecoveryEligibilityResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (학번 형식 오류)",
                    content = @Content
            )
    })
    @GetMapping("/account/recovery-check")
    public ResponseEntity<RecoveryEligibilityResponse> checkRecoveryEligibility(
            @Parameter(description = "복구 가능 여부를 확인할 학번 (8자리 숫자)", example = "12345678", required = true)
            @RequestParam @Pattern(regexp = "^\\d{8}$", message = "학번은 8자리 숫자여야 합니다") String studentId) {
        RecoveryEligibilityResponse response = accountRecoveryService.checkRecoveryEligibility(studentId);
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
                    description = "계정 복구 성공",
                    content = @Content(schema = @Schema(implementation = AccountRecoveryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패) 또는 복구 기간 만료",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (학번 또는 비밀번호 불일치)",
                    content = @Content
            )
    })
    @PostMapping("/account/recover")
    public ResponseEntity<AccountRecoveryResponse> recoverAccount(@Valid @RequestBody AccountRecoveryRequest request) {
        AccountRecoveryResponse response = accountRecoveryService.recoverAccount(request.studentId(), request.password());
        return ResponseEntity.ok(response);
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
                    description = "잘못된 요청 (학번 형식 오류)",
                    content = @Content
            )
    })
    @PostMapping("/reset-request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestPasswordReset(request.studentId());
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
                    description = "잘못된 요청 (유효성 검증 실패) 또는 유효하지 않은/만료된 토큰",
                    content = @Content
            )
    })
    @PostMapping("/reset-confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
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
                    description = "유효하지 않거나 만료된 토큰",
                    content = @Content
            )
    })
    @GetMapping("/reset-validate")
    public ResponseEntity<Void> validateResetToken(
            @Parameter(description = "검증할 재설정 토큰", required = true)
            @RequestParam String token) {
        passwordResetService.validateResetToken(token);
        return ResponseEntity.ok().build();
    }
}
