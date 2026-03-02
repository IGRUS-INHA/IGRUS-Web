package igrus.web.common.util;

import igrus.web.common.util.PageResponseMapper.PageMeta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PageResponseMapper 단위 테스트")
class PageResponseMapperTest {

    @Nested
    @DisplayName("extractMeta")
    class ExtractMetaTest {

        @Test
        @DisplayName("빈 Page에서 extractMeta - 기본값 확인")
        void empty_page_returns_default_meta() {
            Page<String> emptyPage = new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                    0
            );

            PageMeta meta = PageResponseMapper.extractMeta(emptyPage);

            assertThat(meta.totalElements()).isEqualTo(0);
            assertThat(meta.totalPages()).isEqualTo(0);
            assertThat(meta.number()).isEqualTo(0);
            assertThat(meta.size()).isEqualTo(20);
            assertThat(meta.numberOfElements()).isEqualTo(0);
            assertThat(meta.first()).isTrue();
            assertThat(meta.last()).isTrue();
            assertThat(meta.empty()).isTrue();
            assertThat(meta.pageable()).isNotNull();
            assertThat(meta.pageable().getPaged()).isTrue();
            assertThat(meta.pageable().getPageNumber()).isEqualTo(0);
            assertThat(meta.pageable().getPageSize()).isEqualTo(20);
            assertThat(meta.sort()).isNotNull();
            assertThat(meta.sort().getSorted()).isTrue();
            assertThat(meta.sort().getEmpty()).isFalse();
        }

        @Test
        @DisplayName("content가 있는 Page에서 extractMeta")
        void page_with_content_returns_correct_meta() {
            List<String> content = List.of("a", "b", "c");
            Page<String> page = new PageImpl<>(
                    content,
                    PageRequest.of(0, 10),
                    30
            );

            PageMeta meta = PageResponseMapper.extractMeta(page);

            assertThat(meta.totalElements()).isEqualTo(30);
            assertThat(meta.totalPages()).isEqualTo(3);
            assertThat(meta.number()).isEqualTo(0);
            assertThat(meta.size()).isEqualTo(10);
            assertThat(meta.numberOfElements()).isEqualTo(3);
            assertThat(meta.first()).isTrue();
            assertThat(meta.last()).isFalse();
            assertThat(meta.empty()).isFalse();
        }
    }

    @Nested
    @DisplayName("mapContent")
    class MapContentTest {

        @Test
        @DisplayName("mapContent로 content 변환")
        void maps_content_correctly() {
            List<Integer> content = List.of(1, 2, 3);
            Page<Integer> page = new PageImpl<>(content);

            List<String> mapped = PageResponseMapper.mapContent(page, i -> "item-" + i);

            assertThat(mapped).containsExactly("item-1", "item-2", "item-3");
        }
    }

    @Nested
    @DisplayName("toSpringPageResponse")
    class ToSpringPageResponseTest {

        @Test
        @DisplayName("toSpringPageResponse 전체 파이프라인 테스트")
        void full_pipeline() {
            List<Integer> content = List.of(10, 20);
            Page<Integer> page = new PageImpl<>(
                    content,
                    PageRequest.of(1, 5, Sort.by(Sort.Direction.ASC, "id")),
                    12
            );

            TestPageResponse response = PageResponseMapper.toSpringPageResponse(
                    page,
                    i -> "val-" + i,
                    TestPageResponse::new,
                    (r, items, meta) -> {
                        r.items = items;
                        r.totalElements = meta.totalElements();
                        r.totalPages = meta.totalPages();
                        r.number = meta.number();
                        r.empty = meta.empty();
                        return r;
                    }
            );

            assertThat(response.items).containsExactly("val-10", "val-20");
            assertThat(response.totalElements).isEqualTo(12);
            assertThat(response.totalPages).isEqualTo(3);
            assertThat(response.number).isEqualTo(1);
            assertThat(response.empty).isFalse();
        }
    }

    /**
     * toSpringPageResponse 테스트용 간단한 응답 객체.
     */
    static class TestPageResponse {
        List<String> items;
        long totalElements;
        int totalPages;
        int number;
        boolean empty;
    }
}
