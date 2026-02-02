package igrus.web.community.post.service.support;

import igrus.web.community.post.repository.PostRepository;
import igrus.web.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 사용자가 현재 시간 기준으로 추가 작성 가능한 게시글 수를 조회하는 서비스.
 * <p>
 * 사용자당 시간당 최대 20개의 게시글 작성을 허용합니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetRemainingPostsService {

    private static final int MAX_POSTS_PER_HOUR = 20;

    private final PostRepository postRepository;

    /**
     * 사용자가 현재 시간 기준으로 추가 작성 가능한 게시글 수를 반환합니다.
     *
     * @param user 확인할 사용자
     * @return 남은 작성 가능 게시글 수 (0 이상)
     */
    @Transactional(readOnly = true)
    public int getRemainingPosts(User user) {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentPostCount = postRepository.countByAuthorAndCreatedAtAfter(user, oneHourAgo);
        return Math.max(0, MAX_POSTS_PER_HOUR - (int) recentPostCount);
    }
}
