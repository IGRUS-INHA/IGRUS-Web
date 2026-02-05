package igrus.web.community.post.service.support;

import igrus.web.community.post.repository.PostViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 특정 게시글의 실제 조회 수를 조회하는 서비스.
 * PostView 테이블의 COUNT를 반환합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetActualViewCountService {

    private final PostViewRepository postViewRepository;

    /**
     * 특정 게시글 ID의 실제 조회 수를 조회합니다.
     *
     * @param postId 게시글 ID
     * @return 실제 조회 수
     */
    @Transactional(readOnly = true)
    public long getActualViewCount(Long postId) {
        return postViewRepository.countByPostId(postId);
    }
}
