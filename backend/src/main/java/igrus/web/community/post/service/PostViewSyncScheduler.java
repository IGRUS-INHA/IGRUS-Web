package igrus.web.community.post.service;

import igrus.web.community.post.domain.Post;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.community.post.repository.PostViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 게시글 조회수 동기화 스케줄러.
 * PostView 테이블의 실제 조회 수와 Post.viewCount 필드를 주기적으로 동기화합니다.
 * 낙관적 락 충돌로 인해 viewCount 업데이트가 실패한 경우를 보정합니다.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class PostViewSyncScheduler {

    private final PostRepository postRepository;
    private final PostViewRepository postViewRepository;

    /**
     * 조회수를 10분마다 동기화합니다.
     * 모든 게시글의 viewCount를 PostView COUNT와 비교하여 불일치 시 업데이트합니다.
     */
    @Scheduled(fixedRate = 600000) // 10분마다
    @Transactional
    public void syncViewCounts() {
        log.debug("조회수 동기화 시작");

        List<Post> allPosts = postRepository.findAll();
        int syncedCount = 0;

        for (Post post : allPosts) {
            long actualCount = postViewRepository.countByPost(post);
            int currentCount = post.getViewCount();

            if (currentCount != actualCount) {
                post.syncViewCount((int) actualCount);
                syncedCount++;
                log.debug("조회수 동기화: postId={}, {} -> {}", post.getId(), currentCount, actualCount);
            }
        }

        if (syncedCount > 0) {
            log.info("조회수 동기화 완료: {}건 업데이트", syncedCount);
        } else {
            log.debug("조회수 동기화 완료: 업데이트 없음");
        }
    }
}
