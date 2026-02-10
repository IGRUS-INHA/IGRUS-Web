package igrus.web.common.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StringListConverter 단위 테스트")
class StringListConverterTest {

    private final StringListConverter converter = new StringListConverter();

    @Nested
    @DisplayName("convertToDatabaseColumn")
    class ConvertToDatabaseColumnTest {

        @Test
        @DisplayName("null 입력 시 null 반환")
        void null_input_returnsNull() {
            assertThat(converter.convertToDatabaseColumn(null)).isNull();
        }

        @Test
        @DisplayName("빈 리스트 입력 시 null 반환")
        void empty_list_returnsNull() {
            assertThat(converter.convertToDatabaseColumn(List.of())).isNull();
        }

        @Test
        @DisplayName("정상 리스트 입력 시 JSON 문자열 반환")
        void valid_list_returnsJsonString() {
            List<String> input = List.of("a", "b", "c");
            String result = converter.convertToDatabaseColumn(input);
            assertThat(result).isEqualTo("[\"a\",\"b\",\"c\"]");
        }

        @Test
        @DisplayName("단일 요소 리스트 입력 시 JSON 배열 반환")
        void single_element_returnsJsonArray() {
            List<String> input = List.of("only");
            String result = converter.convertToDatabaseColumn(input);
            assertThat(result).isEqualTo("[\"only\"]");
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute")
    class ConvertToEntityAttributeTest {

        @Test
        @DisplayName("null 입력 시 빈 가변 리스트 반환")
        void null_input_returnsMutableEmptyList() {
            List<String> result = converter.convertToEntityAttribute(null);
            assertThat(result).isEmpty();
            result.add("test");
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("빈 문자열 입력 시 빈 가변 리스트 반환")
        void blank_input_returnsMutableEmptyList() {
            List<String> result = converter.convertToEntityAttribute("  ");
            assertThat(result).isEmpty();
            result.add("test");
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("정상 JSON 입력 시 리스트 반환")
        void valid_json_returnsList() {
            List<String> result = converter.convertToEntityAttribute("[\"a\",\"b\"]");
            assertThat(result).containsExactly("a", "b");
        }

        @Test
        @DisplayName("비정상 JSON 입력 시 예외 발생")
        void invalid_json_throwsException() {
            assertThatThrownBy(() -> converter.convertToEntityAttribute("not-json"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
