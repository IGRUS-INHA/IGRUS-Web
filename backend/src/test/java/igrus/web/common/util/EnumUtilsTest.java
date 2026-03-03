package igrus.web.common.util;

import igrus.web.common.exception.InvalidEnumValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EnumUtils 단위 테스트")
class EnumUtilsTest {

    private enum TestColor { RED, GREEN, BLUE }

    @Nested
    @DisplayName("fromStringOrNull")
    class FromStringOrNullTest {

        @Test
        @DisplayName("null 입력 시 null 반환")
        void null_input_returnsNull() {
            assertThat(EnumUtils.fromStringOrNull(TestColor.class, null)).isNull();
        }

        @Test
        @DisplayName("유효한 enum 값 변환 성공")
        void valid_value_returnsEnum() {
            assertThat(EnumUtils.fromStringOrNull(TestColor.class, "RED")).isEqualTo(TestColor.RED);
            assertThat(EnumUtils.fromStringOrNull(TestColor.class, "GREEN")).isEqualTo(TestColor.GREEN);
            assertThat(EnumUtils.fromStringOrNull(TestColor.class, "BLUE")).isEqualTo(TestColor.BLUE);
        }

        @Test
        @DisplayName("잘못된 enum 값 입력 시 InvalidEnumValueException 발생")
        void invalid_value_throwsException() {
            assertThatThrownBy(() -> EnumUtils.fromStringOrNull(TestColor.class, "YELLOW"))
                    .isInstanceOf(InvalidEnumValueException.class);
        }

        @Test
        @DisplayName("빈 문자열 입력 시 InvalidEnumValueException 발생")
        void empty_string_throwsException() {
            assertThatThrownBy(() -> EnumUtils.fromStringOrNull(TestColor.class, ""))
                    .isInstanceOf(InvalidEnumValueException.class);
        }
    }
}
