package igrus.web.security.auth.password.controller.fixture;

import igrus.web.security.auth.common.dto.request.AccountRecoveryRequest;
import igrus.web.security.auth.common.dto.response.AccountRecoveryResponse;
import igrus.web.security.auth.common.dto.response.RecoveryEligibilityResponse;
import igrus.web.security.auth.password.dto.request.PasswordLoginRequest;
import igrus.web.security.auth.password.dto.request.PasswordLogoutRequest;
import igrus.web.security.auth.password.dto.response.PasswordLoginResponse;
import igrus.web.user.domain.UserRole;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * PasswordAuthController 테스트를 위한 픽스처 클래스
 */
public final class PasswordAuthTestFixture {

    // 유효한 테스트 데이터
    public static final String VALID_STUDENT_ID = "12345678";
    public static final String VALID_PASSWORD = "Test1234!@";
    public static final String VALID_EMAIL = "test@inha.edu";
    public static final String VALID_NAME = "홍길동";
    public static final Long VALID_USER_ID = 1L;
    public static final long VALID_EXPIRES_IN = 3600000L; // 1시간

    // 토큰 데이터
    public static final String VALID_ACCESS_TOKEN = "valid.access.token";
    public static final String VALID_REFRESH_TOKEN = "valid.refresh.token";
    public static final String INVALID_REFRESH_TOKEN = "invalid.refresh.token";

    // 잘못된 테스트 데이터
    public static final String INVALID_STUDENT_ID = "99999999";
    public static final String INVALID_PASSWORD = "wrongPassword";
    public static final String INVALID_FORMAT_STUDENT_ID = "1234"; // 8자리가 아닌 학번
    public static final String EMPTY_STRING = "";
    public static final String BLANK_STRING = "   ";

    private PasswordAuthTestFixture() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    // ==================== Login Request Fixtures ====================

    /**
     * 유효한 로그인 요청 생성
     */
    public static PasswordLoginRequest validLoginRequest() {
        return new PasswordLoginRequest(VALID_STUDENT_ID, VALID_PASSWORD);
    }

    /**
     * 잘못된 학번으로 로그인 요청 생성
     */
    public static PasswordLoginRequest loginRequestWithInvalidStudentId() {
        return new PasswordLoginRequest(INVALID_STUDENT_ID, VALID_PASSWORD);
    }

    /**
     * 잘못된 비밀번호로 로그인 요청 생성
     */
    public static PasswordLoginRequest loginRequestWithInvalidPassword() {
        return new PasswordLoginRequest(VALID_STUDENT_ID, INVALID_PASSWORD);
    }

    /**
     * 빈 학번으로 로그인 요청 생성
     */
    public static PasswordLoginRequest loginRequestWithEmptyStudentId() {
        return new PasswordLoginRequest(EMPTY_STRING, VALID_PASSWORD);
    }

    /**
     * 빈 비밀번호로 로그인 요청 생성
     */
    public static PasswordLoginRequest loginRequestWithEmptyPassword() {
        return new PasswordLoginRequest(VALID_STUDENT_ID, EMPTY_STRING);
    }

    /**
     * 공백 학번으로 로그인 요청 생성
     */
    public static PasswordLoginRequest loginRequestWithBlankStudentId() {
        return new PasswordLoginRequest(BLANK_STRING, VALID_PASSWORD);
    }

    /**
     * 공백 비밀번호로 로그인 요청 생성
     */
    public static PasswordLoginRequest loginRequestWithBlankPassword() {
        return new PasswordLoginRequest(VALID_STUDENT_ID, BLANK_STRING);
    }

    // ==================== Login Response Fixtures ====================

    /**
     * 특정 역할에 대한 로그인 성공 응답 생성
     */
    public static PasswordLoginResponse loginSuccessResponse(UserRole role) {
        return PasswordLoginResponse.of(
                VALID_ACCESS_TOKEN,
                VALID_REFRESH_TOKEN,
                VALID_USER_ID,
                VALID_STUDENT_ID,
                VALID_NAME,
                role,
                VALID_EXPIRES_IN
        );
    }

    /**
     * 준회원 로그인 성공 응답 생성
     */
    public static PasswordLoginResponse associateLoginResponse() {
        return loginSuccessResponse(UserRole.ASSOCIATE);
    }

    /**
     * 정회원 로그인 성공 응답 생성
     */
    public static PasswordLoginResponse memberLoginResponse() {
        return loginSuccessResponse(UserRole.MEMBER);
    }

    /**
     * 운영진 로그인 성공 응답 생성
     */
    public static PasswordLoginResponse operatorLoginResponse() {
        return loginSuccessResponse(UserRole.OPERATOR);
    }

    /**
     * 관리자 로그인 성공 응답 생성
     */
    public static PasswordLoginResponse adminLoginResponse() {
        return loginSuccessResponse(UserRole.ADMIN);
    }

    // ==================== Logout Request Fixtures ====================

    /**
     * 유효한 로그아웃 요청 생성
     */
    public static PasswordLogoutRequest validLogoutRequest() {
        return new PasswordLogoutRequest(VALID_REFRESH_TOKEN);
    }

    /**
     * 유효하지 않은 토큰으로 로그아웃 요청 생성
     */
    public static PasswordLogoutRequest logoutRequestWithInvalidToken() {
        return new PasswordLogoutRequest(INVALID_REFRESH_TOKEN);
    }

    /**
     * 빈 토큰으로 로그아웃 요청 생성
     */
    public static PasswordLogoutRequest logoutRequestWithEmptyToken() {
        return new PasswordLogoutRequest(EMPTY_STRING);
    }

    /**
     * 공백 토큰으로 로그아웃 요청 생성
     */
    public static PasswordLogoutRequest logoutRequestWithBlankToken() {
        return new PasswordLogoutRequest(BLANK_STRING);
    }

    // ==================== Account Recovery Request Fixtures ====================

    /**
     * 유효한 계정 복구 요청 생성
     */
    public static AccountRecoveryRequest validRecoveryRequest() {
        return new AccountRecoveryRequest(VALID_STUDENT_ID, VALID_PASSWORD);
    }

    /**
     * 잘못된 비밀번호로 계정 복구 요청 생성
     */
    public static AccountRecoveryRequest recoveryRequestWithInvalidPassword() {
        return new AccountRecoveryRequest(VALID_STUDENT_ID, INVALID_PASSWORD);
    }

    /**
     * 빈 학번으로 계정 복구 요청 생성
     */
    public static AccountRecoveryRequest recoveryRequestWithEmptyStudentId() {
        return new AccountRecoveryRequest(EMPTY_STRING, VALID_PASSWORD);
    }

    /**
     * 빈 비밀번호로 계정 복구 요청 생성
     */
    public static AccountRecoveryRequest recoveryRequestWithEmptyPassword() {
        return new AccountRecoveryRequest(VALID_STUDENT_ID, EMPTY_STRING);
    }

    /**
     * 잘못된 형식의 학번으로 계정 복구 요청 생성
     */
    public static AccountRecoveryRequest recoveryRequestWithInvalidStudentIdFormat() {
        return new AccountRecoveryRequest(INVALID_FORMAT_STUDENT_ID, VALID_PASSWORD);
    }

    // ==================== Account Recovery Response Fixtures ====================

    /**
     * 계정 복구 성공 응답 생성
     */
    public static AccountRecoveryResponse recoverySuccessResponse() {
        return AccountRecoveryResponse.of(
                VALID_ACCESS_TOKEN,
                VALID_REFRESH_TOKEN,
                VALID_USER_ID,
                VALID_STUDENT_ID,
                VALID_NAME,
                UserRole.MEMBER,
                VALID_EXPIRES_IN
        );
    }

    // ==================== Recovery Eligibility Response Fixtures ====================

    /**
     * 복구 가능 응답 생성
     */
    public static RecoveryEligibilityResponse recoverableResponse() {
        Instant deadline = Instant.now().plus(3, ChronoUnit.DAYS);
        return RecoveryEligibilityResponse.recoverable(deadline);
    }

    /**
     * 복구 불가능 응답 생성 (기간 만료)
     */
    public static RecoveryEligibilityResponse notRecoverableResponse() {
        return RecoveryEligibilityResponse.notRecoverable();
    }

    /**
     * 탈퇴 상태가 아닌 계정 응답 생성
     */
    public static RecoveryEligibilityResponse notWithdrawnResponse() {
        return RecoveryEligibilityResponse.notWithdrawn();
    }
}
