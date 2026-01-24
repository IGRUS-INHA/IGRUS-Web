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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Password Authentication", description = "비밀번호 기반 인증 관련 API")
public interface PasswordAuthControllerApi {

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
    ResponseEntity<PasswordLoginResponse> login(PasswordLoginRequest request);

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
    ResponseEntity<Void> logout(PasswordLogoutRequest request);

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
    ResponseEntity<TokenRefreshResponse> refreshToken(TokenRefreshRequest request);

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
    ResponseEntity<PasswordSignupResponse> signup(PasswordSignupRequest request);

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
    ResponseEntity<PasswordSignupResponse> verifyEmail(EmailVerificationRequest request);

    @Operation(summary = "인증 코드 재발송", description = "이메일 인증 코드를 다시 발송합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "인증 코드 재발송 성공",
                    content = @Content(schema = @Schema(implementation = PasswordSignupResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패)",
                    content = @Content
            )
    })
    ResponseEntity<PasswordSignupResponse> resendVerification(ResendVerificationRequest request);
}
