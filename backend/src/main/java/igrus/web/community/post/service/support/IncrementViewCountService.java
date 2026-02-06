package igrus.web.community.post.service.support;

import igrus.web.community.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 조회수를 증가시키는 서비스.
 * SQL 레벨에서 원자적으로 동작하므로 재시도가 필요하지 않습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IncrementViewCountService {

    private final PostRepository postRepository;

    /**
     * 조회수를 원자적으로 증가시킵니다.
     *
     * @param postId 게시글 ID
     */
    public void incrementViewCount(Long postId) {
        postRepository.incrementViewCount(postId);
    }
}
