package igrus.web.storage.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import igrus.web.storage.exception.UnsupportedContentTypeException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * S3 Object Key 생성 유틸리티.
 * Object Key 형식: {purpose}/{YYYY}/{MM}/{DD}/{UUID}.{extension}
 */
@Component
@RequiredArgsConstructor
public class ObjectKeyGenerator {

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", "jpeg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp"
    );

    private final Clock clock;

    /**
     * S3 Object Key를 생성한다.
     *
     * @param purpose     사용처 (posts, profiles, events 등)
     * @param contentType Content-Type (image/jpeg, image/png, image/gif, image/webp)
     * @return 생성된 Object Key
     * @throws UnsupportedContentTypeException 지원하지 않는 Content-Type인 경우
     */
    public String generate(String purpose, String contentType) {
        String extension = CONTENT_TYPE_TO_EXTENSION.get(contentType);
        if (extension == null) {
            throw new UnsupportedContentTypeException(contentType);
        }

        LocalDate today = LocalDate.now(clock);
        String uuid = UUID.randomUUID().toString();

        return String.format("%s/%04d/%02d/%02d/%s.%s",
                purpose,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                uuid,
                extension);
    }
}
