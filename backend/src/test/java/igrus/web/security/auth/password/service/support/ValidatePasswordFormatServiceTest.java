package igrus.web.security.auth.password.service.support;

import igrus.web.security.auth.password.exception.InvalidPasswordFormatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ValidatePasswordFormatService 테스트")
class ValidatePasswordFormatServiceTest {

    private final ValidatePasswordFormatService validatePasswordFormatService = new ValidatePasswordFormatService();

    @Test
    @DisplayName("유효한 형식의 비밀번호 검증 통과 (영문+숫자) [PWD-030]")
    void validatePasswordFormat_WithValidPassword_Passes() {
        // given
        String validPassword = "password1";

        // when & then (예외가 발생하지 않으면 성공)
        validatePasswordFormatService.validatePasswordFormat(validPassword);
    }

    @Test
    @DisplayName("유효한 형식의 비밀번호 검증 통과 (대문자+숫자) [PWD-030-2]")
    void validatePasswordFormat_WithUppercaseAndDigit_Passes() {
        // given
        String validPassword = "PASSWORD1";

        // when & then (예외가 발생하지 않으면 성공)
        validatePasswordFormatService.validatePasswordFormat(validPassword);
    }

    @Test
    @DisplayName("유효한 형식의 비밀번호 검증 통과 (영문+숫자+특수문자) [PWD-030-3]")
    void validatePasswordFormat_WithSpecialChar_Passes() {
        // given
        String validPassword = "password1!";

        // when & then (예외가 발생하지 않으면 성공)
        validatePasswordFormatService.validatePasswordFormat(validPassword);
    }

    @Test
    @DisplayName("8자 미만 비밀번호 검증 실패 [PWD-031]")
    void validatePasswordFormat_WithShortPassword_ThrowsException() {
        // given
        String shortPassword = "pass1"; // 5자

        // when & then
        assertThatThrownBy(() -> validatePasswordFormatService.validatePasswordFormat(shortPassword))
                .isInstanceOf(InvalidPasswordFormatException.class);
    }

    @Test
    @DisplayName("영문 미포함 비밀번호 검증 실패 [PWD-032]")
    void validatePasswordFormat_WithoutLetter_ThrowsException() {
        // given
        String noLetter = "12345678"; // 영문 없음

        // when & then
        assertThatThrownBy(() -> validatePasswordFormatService.validatePasswordFormat(noLetter))
                .isInstanceOf(InvalidPasswordFormatException.class);
    }

    @Test
    @DisplayName("숫자 미포함 비밀번호 검증 실패 [PWD-034]")
    void validatePasswordFormat_WithoutDigit_ThrowsException() {
        // given
        String noDigit = "password"; // 숫자 없음

        // when & then
        assertThatThrownBy(() -> validatePasswordFormatService.validatePasswordFormat(noDigit))
                .isInstanceOf(InvalidPasswordFormatException.class);
    }

    @Test
    @DisplayName("null 비밀번호 검증 실패")
    void validatePasswordFormat_WithNull_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> validatePasswordFormatService.validatePasswordFormat(null))
                .isInstanceOf(InvalidPasswordFormatException.class);
    }
}
