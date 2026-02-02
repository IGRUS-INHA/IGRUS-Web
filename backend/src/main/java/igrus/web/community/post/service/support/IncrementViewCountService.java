package igrus.web.community.post.service.support;

import igrus.web.community.post.domain.Post;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 조회수를 증가시키는 서비스.
 * 낙관적 락 충돌 시 재시도합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IncrementViewCountService {

    private final PostRepository postRepository;

    /**
     * 조회수를 증가시킵니다. 낙관적 락 충돌 시 재시도합니다.
     *
     * @param post       게시글
     * @param maxRetries 최대 재시도 횟수
     */
    public void incrementViewCountWithRetry(Post post, int maxRetries) {
        Long postId = post.getId();
        Post currentPost = post;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                currentPost.incrementViewCount();
                postRepository.saveAndFlush(currentPost);
                return;
            } catch (OptimisticLockException e) {
                if (attempt == maxRetries) {
                    log.warn("조회수 증가 실패 ({}회 재시도 후): postId={}", maxRetries, postId);
                    return;
                }
                // 엔티티 새로고침 후 재시도
                currentPost = postRepository.findById(postId)
                        .orElseThrow(() -> new PostNotFoundException(postId));
            }
        }
    }
}
