package igrus.web.common.util;

import igrus.web.common.exception.InvalidPageParameterException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PageableUtils 단위 테스트")
class PageableUtilsTest {

    @Nested
    @DisplayName("of")
    class OfTest {

        @Test
        @DisplayName("기본값으로 Pageable 생성 (page=null, size=null, sort=null)")
        void defaults_when_all_null() {
            Pageable pageable = PageableUtils.of(null, null, null);

            assertThat(pageable.getPageNumber()).isEqualTo(0);
            assertThat(pageable.getPageSize()).isEqualTo(20);
            assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        @Test
        @DisplayName("page=0, size=10으로 정상 생성")
        void explicit_page_and_size() {
            Pageable pageable = PageableUtils.of(0, 10, null);

            assertThat(pageable.getPageNumber()).isEqualTo(0);
            assertThat(pageable.getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("page 음수(-1) 시 InvalidPageParameterException 발생")
        void negative_page_throwsException() {
            assertThatThrownBy(() -> PageableUtils.of(-1, 10, null))
                    .isInstanceOf(InvalidPageParameterException.class);
        }

        @Test
        @DisplayName("size 0 이하 시 InvalidPageParameterException 발생")
        void zero_size_throwsException() {
            assertThatThrownBy(() -> PageableUtils.of(0, 0, null))
                    .isInstanceOf(InvalidPageParameterException.class);
        }

        @Test
        @DisplayName("sort=[\"createdAt,DESC\"] 정상 파싱")
        void sort_with_comma_direction() {
            Pageable pageable = PageableUtils.of(0, 10, List.of("createdAt,DESC"));

            Sort.Order order = pageable.getSort().getOrderFor("createdAt");
            assertThat(order).isNotNull();
            assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("sort=[\"createdAt\", \"DESC\"] (Spring 쉼표 분리) 정상 병합")
        void sort_spring_comma_split_merged() {
            Pageable pageable = PageableUtils.of(0, 10, List.of("createdAt", "DESC"));

            Sort.Order order = pageable.getSort().getOrderFor("createdAt");
            assertThat(order).isNotNull();
            assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("sort 방향 생략 시 ASC 기본값 적용")
        void sort_without_direction_defaults_to_asc() {
            Pageable pageable = PageableUtils.of(0, 10, List.of("title"));

            Sort.Order order = pageable.getSort().getOrderFor("title");
            assertThat(order).isNotNull();
            assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("빈 sort 리스트 시 기본값(createdAt DESC) 적용")
        void empty_sort_list_defaults_to_createdAt_desc() {
            Pageable pageable = PageableUtils.of(0, 10, List.of());

            assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
    }
}
