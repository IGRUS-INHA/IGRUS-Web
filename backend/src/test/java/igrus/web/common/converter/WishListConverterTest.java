package igrus.web.common.converter;

import igrus.web.user.domain.Wish;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WishListConverter 단위 테스트")
class WishListConverterTest {

    private final WishListConverter converter = new WishListConverter();

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
        @DisplayName("정상 Wish 리스트 입력 시 JSON 문자열 반환")
        void valid_list_returnsJsonString() {
            List<Wish> input = List.of(Wish.NETWORKING, Wish.PROGRAMMING);
            String result = converter.convertToDatabaseColumn(input);
            assertThat(result).isEqualTo("[\"NETWORKING\",\"PROGRAMMING\"]");
        }

        @Test
        @DisplayName("단일 요소 리스트 입력 시 JSON 배열 반환")
        void single_element_returnsJsonArray() {
            List<Wish> input = List.of(Wish.CAREER);
            String result = converter.convertToDatabaseColumn(input);
            assertThat(result).isEqualTo("[\"CAREER\"]");
        }

        @Test
        @DisplayName("모든 Wish 값 변환 가능")
        void all_wishes_convertible() {
            List<Wish> input = List.of(Wish.values());
            String result = converter.convertToDatabaseColumn(input);
            assertThat(result).contains("NETWORKING", "STUDY", "PROJECT", "CAREER", "PROGRAMMING");
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute")
    class ConvertToEntityAttributeTest {

        @Test
        @DisplayName("null 입력 시 빈 가변 리스트 반환")
        void null_input_returnsMutableEmptyList() {
            List<Wish> result = converter.convertToEntityAttribute(null);
            assertThat(result).isEmpty();
            result.add(Wish.NETWORKING);
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("빈 문자열 입력 시 빈 가변 리스트 반환")
        void blank_input_returnsMutableEmptyList() {
            List<Wish> result = converter.convertToEntityAttribute("  ");
            assertThat(result).isEmpty();
            result.add(Wish.STUDY);
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("정상 JSON 입력 시 Wish 리스트 반환")
        void valid_json_returnsWishList() {
            List<Wish> result = converter.convertToEntityAttribute("[\"NETWORKING\",\"CAREER\"]");
            assertThat(result).containsExactly(Wish.NETWORKING, Wish.CAREER);
        }

        @Test
        @DisplayName("비정상 JSON 입력 시 예외 발생")
        void invalid_json_throwsException() {
            assertThatThrownBy(() -> converter.convertToEntityAttribute("not-json"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("존재하지 않는 Wish 값 입력 시 예외 발생")
        void invalid_wish_value_throwsException() {
            assertThatThrownBy(() -> converter.convertToEntityAttribute("[\"INVALID_WISH\"]"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("변환 왕복 테스트 - DB 저장 후 다시 읽기")
        void roundTrip_conversion() {
            List<Wish> original = List.of(Wish.NETWORKING, Wish.PROJECT, Wish.PROGRAMMING);
            String json = converter.convertToDatabaseColumn(original);
            List<Wish> restored = converter.convertToEntityAttribute(json);
            assertThat(restored).containsExactlyElementsOf(original);
        }
    }
}
