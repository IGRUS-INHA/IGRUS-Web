package igrus.web.community.like.post_like.service.read;

import igrus.web.community.like.post_like.dto.response.PostLikeStatusResponse;
import igrus.web.community.like.post_like.repository.PostLikeRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 좋아요 상태 조회 서비스.
 * 게시글의 좋아요 상태와 총 좋아요 수를 조회합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetPostLikeStatusService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;

    /**
     * 게시글의 좋아요 상태를 조회합니다.
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 좋아요 상태 응답
     * @throws PostNotFoundException 게시글을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public PostLikeStatusResponse getLikeStatus(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        boolean liked = postLikeRepository.existsByPostIdAndUserId(postId, userId);
        return PostLikeStatusResponse.of(liked, post.getLikeCount());
    }
}
