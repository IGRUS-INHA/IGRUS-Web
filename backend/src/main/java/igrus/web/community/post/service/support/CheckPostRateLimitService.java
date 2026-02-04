package igrus.web.community.post.service.support;

import igrus.web.community.post.exception.PostRateLimitExceededException;
import igrus.web.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 작성 속도 제한을 확인하는 서비스.
 * <p>
 * 사용자당 시간당 최대 20개의 게시글 작성을 허용합니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CheckPostRateLimitService {

    private final GetRemainingPostsService getRemainingPostsService;

    /**
     * 사용자의 게시글 작성 속도 제한을 확인합니다.
     *
     * @param user 확인할 사용자
     * @throws PostRateLimitExceededException 시간당 20회 초과 시
     */
    @Transactional(readOnly = true)
    public void checkRateLimit(User user) {
        int remaining = getRemainingPostsService.getRemainingPosts(user);
        if (remaining <= 0) {
            throw new PostRateLimitExceededException();
        }
    }
}
