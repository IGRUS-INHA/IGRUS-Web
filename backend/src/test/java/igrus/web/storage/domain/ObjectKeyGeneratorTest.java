package igrus.web.storage.domain;

import igrus.web.storage.exception.UnsupportedContentTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ObjectKeyGenerator 단위 테스트.
 *
 * <p>TC-009: 동일 요청 다수 호출 시 유일한 Object Key 생성</p>
 * <p>TC-010: Object Key 형식 규약 검증</p>
 */
@DisplayName("ObjectKeyGenerator 단위 테스트")
class ObjectKeyGeneratorTest {

    private static final Pattern OBJECT_KEY_PATTERN = Pattern.compile(
            "^(posts|profiles|events)/\\d{4}/\\d{2}/\\d{2}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.\\w+$"
    );

    private ObjectKeyGenerator objectKeyGenerator;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-02-26T10:00:00Z"),
                ZoneId.of("UTC")
        );
        objectKeyGenerator = new ObjectKeyGenerator(fixedClock);
    }

    @DisplayName("TC-009: 동일 파일명으로 10회 호출 시 각각 다른 Object Key 생성")
    @Test
    void generate_WithSameInput10Times_ReturnsUniqueKeys() {
        // given
        Set<String> generatedKeys = new HashSet<>();

        // when
        for (int i = 0; i < 10; i++) {
            String key = objectKeyGenerator.generate("posts", "image/png");
            generatedKeys.add(key);
        }

        // then
        assertThat(generatedKeys).hasSize(10);
    }

    @DisplayName("TC-010: Object Key 형식이 {purpose}/{YYYY}/{MM}/{DD}/{UUID}.{extension}에 부합")
    @Test
    void generate_WithValidInput_ReturnsCorrectFormat() {
        // when
        String key = objectKeyGenerator.generate("posts", "image/png");

        // then
        assertThat(key).matches(OBJECT_KEY_PATTERN);
        assertThat(key).startsWith("posts/2026/02/26/");
        assertThat(key).endsWith(".png");
    }

    @DisplayName("Content-Type별 확장자 매핑 - image/jpeg -> jpeg")
    @Test
    void generate_WithJpegContentType_ReturnsJpegExtension() {
        String key = objectKeyGenerator.generate("profiles", "image/jpeg");
        assertThat(key).endsWith(".jpeg");
        assertThat(key).startsWith("profiles/");
    }

    @DisplayName("Content-Type별 확장자 매핑 - image/gif -> gif")
    @Test
    void generate_WithGifContentType_ReturnsGifExtension() {
        String key = objectKeyGenerator.generate("events", "image/gif");
        assertThat(key).endsWith(".gif");
        assertThat(key).startsWith("events/");
    }

    @DisplayName("Content-Type별 확장자 매핑 - image/webp -> webp")
    @Test
    void generate_WithWebpContentType_ReturnsWebpExtension() {
        String key = objectKeyGenerator.generate("posts", "image/webp");
        assertThat(key).endsWith(".webp");
    }

    @DisplayName("지원하지 않는 Content-Type 시 UnsupportedContentTypeException 발생")
    @Test
    void generate_WithUnsupportedContentType_ThrowsException() {
        assertThatThrownBy(() -> objectKeyGenerator.generate("posts", "image/bmp"))
                .isInstanceOf(UnsupportedContentTypeException.class);
    }
}
