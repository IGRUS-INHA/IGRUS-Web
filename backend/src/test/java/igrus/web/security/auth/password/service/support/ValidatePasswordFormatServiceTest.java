package igrus.web.security.auth.password.service.support;

import igrus.web.security.auth.password.exception.InvalidPasswordFormatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ValidatePasswordFormatService 테스트")
class ValidatePasswordFormatServiceTest {

    private final ValidatePasswordFormatService validatePasswordFormatService = new ValidatePasswordFormatService();

    @Test
    @DisplayName("유효한 형식의 새 비밀번호 검증 통과 [PWD-030]")
    void validatePasswordFormat_WithValidPassword_Passes() {
        // given
        String validPassword = "ValidPass1!";

        // when & then (예외가 발생하지 않으면 성공)
        validatePasswordFormatService.validatePasswordFormat(validPassword);
    }

    @Test
    @DisplayName("8자 미만 비밀번호 검증 실패 [PWD-031]")
    void validatePasswordFormat_WithShortPassword_ThrowsException() {
        // given
        String shortPassword = "Pass1!"; // 7자 미만

        // when & then
        assertThatThrownBy(() -> validatePasswordFormatService.validatePasswordFormat(shortPassword))
                .isInstanceOf(InvalidPasswordFormatException.class);
    }

    @Test
    @DisplayName("대문자 미포함 비밀번호 검증 실패 [PWD-032]")
    void validatePasswordFormat_WithoutUppercase_ThrowsException() {
        // given
        String noUppercase = "password1!"; // 대문자 없음

        // when & then
        assertThatThrownBy(() -> validatePasswordFormatService.validatePasswordFormat(noUppercase))
                .isInstanceOf(InvalidPasswordFormatException.class);
    }

    @Test
    @DisplayName("소문자 미포함 비밀번호 검증 실패 [PWD-033]")
    void validatePasswordFormat_WithoutLowercase_ThrowsException() {
        // given
        String noLowercase = "PASSWORD1!"; // 소문자 없음

        // when & then
        assertThatThrownBy(() -> validatePasswordFormatService.validatePasswordFormat(noLowercase))
                .isInstanceOf(InvalidPasswordFormatException.class);
    }

    @Test
    @DisplayName("숫자 미포함 비밀번호 검증 실패 [PWD-034]")
    void validatePasswordFormat_WithoutDigit_ThrowsException() {
        // given
        String noDigit = "Password!@"; // 숫자 없음

        // when & then
        assertThatThrownBy(() -> validatePasswordFormatService.validatePasswordFormat(noDigit))
                .isInstanceOf(InvalidPasswordFormatException.class);
    }

    @Test
    @DisplayName("특수문자 미포함 비밀번호 검증 실패 [PWD-035]")
    void validatePasswordFormat_WithoutSpecialChar_ThrowsException() {
        // given
        String noSpecialChar = "Password123"; // 특수문자 없음

        // when & then
        assertThatThrownBy(() -> validatePasswordFormatService.validatePasswordFormat(noSpecialChar))
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
