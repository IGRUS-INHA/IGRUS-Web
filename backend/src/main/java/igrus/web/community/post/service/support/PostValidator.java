package igrus.web.community.post.service.support;

import igrus.web.community.post.exception.PostImageLimitExceededException;
import org.springframework.stereotype.Component;

/**
 * 게시글 관련 공통 검증 로직.
 */
@Component
public class PostValidator {

    private static final int MAX_IMAGE_COUNT = 5;

    /**
     * 이미지 개수 제한을 검증합니다.
     */
    public void validateImageCount(int count) {
        if (count > MAX_IMAGE_COUNT) {
            throw new PostImageLimitExceededException(MAX_IMAGE_COUNT, count);
        }
    }
}
