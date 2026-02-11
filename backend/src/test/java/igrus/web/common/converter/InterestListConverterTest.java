package igrus.web.common.converter;

import igrus.web.user.domain.Interest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InterestListConverter 단위 테스트")
class InterestListConverterTest {

    private final InterestListConverter converter = new InterestListConverter();

    @Nested
    @DisplayName("convertToDatabaseColumn - 엔티티 → DB")
    class ConvertToDatabaseColumnTest {

        @Test
        @DisplayName("null → null 반환 [SINT-001]")
        void convertToDatabaseColumn_Null_ReturnsNull() {
            // when
            String result = converter.convertToDatabaseColumn(null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 리스트 → null 반환 [SINT-002]")
        void convertToDatabaseColumn_EmptyList_ReturnsNull() {
            // when
            String result = converter.convertToDatabaseColumn(new ArrayList<>());

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("단일 요소 → JSON 변환 [SINT-003]")
        void convertToDatabaseColumn_SingleElement_ReturnsJson() {
            // when
            String result = converter.convertToDatabaseColumn(List.of(Interest.WEB_FRONTEND));

            // then
            assertThat(result).isEqualTo("[\"WEB_FRONTEND\"]");
        }

        @Test
        @DisplayName("복수 요소 → JSON 변환 [SINT-004]")
        void convertToDatabaseColumn_MultipleElements_ReturnsJson() {
            // when
            String result = converter.convertToDatabaseColumn(List.of(Interest.WEB_FRONTEND, Interest.AI, Interest.CLOUD));

            // then
            assertThat(result).isEqualTo("[\"WEB_FRONTEND\",\"AI\",\"CLOUD\"]");
        }

        @Test
        @DisplayName("전체 요소(10개) → JSON 변환 [SINT-005]")
        void convertToDatabaseColumn_AllElements_ReturnsJson() {
            // given
            List<Interest> allInterests = Arrays.asList(Interest.values());

            // when
            String result = converter.convertToDatabaseColumn(allInterests);

            // then
            assertThat(result).isNotNull();
            for (Interest interest : Interest.values()) {
                assertThat(result).contains(interest.name());
            }
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute - DB → 엔티티")
    class ConvertToEntityAttributeTest {

        @Test
        @DisplayName("null JSON → 빈 리스트 반환 [SINT-006]")
        void convertToEntityAttribute_Null_ReturnsEmptyList() {
            // when
            List<Interest> result = converter.convertToEntityAttribute(null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("유효 JSON → 리스트 변환 [SINT-007]")
        void convertToEntityAttribute_ValidJson_ReturnsList() {
            // when
            List<Interest> result = converter.convertToEntityAttribute("[\"AI\",\"GAME\"]");

            // then
            assertThat(result).containsExactly(Interest.AI, Interest.GAME);
        }

        @Test
        @DisplayName("잘못된 JSON 형식 → 예외 [SINT-008]")
        void convertToEntityAttribute_InvalidJson_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> converter.convertToEntityAttribute("not-json"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("잘못된 enum 값 포함 JSON → 예외 [SINT-009]")
        void convertToEntityAttribute_InvalidEnumValue_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> converter.convertToEntityAttribute("[\"INVALID_VALUE\"]"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
