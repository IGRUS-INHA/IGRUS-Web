package igrus.web.community.like.post_like.service.read;

import igrus.web.community.like.post_like.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 좋아요 여부 확인 서비스.
 * 사용자가 특정 게시글에 좋아요했는지 확인합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CheckPostLikeService {

    private final PostLikeRepository postLikeRepository;

    /**
     * 사용자가 게시글에 좋아요했는지 확인합니다.
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 좋아요 여부
     */
    @Transactional(readOnly = true)
    public boolean isLikedByUser(Long postId, Long userId) {
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }
}
