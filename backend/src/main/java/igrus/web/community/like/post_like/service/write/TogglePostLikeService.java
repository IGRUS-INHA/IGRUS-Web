package igrus.web.community.like.post_like.service.write;

import igrus.web.community.like.post_like.domain.PostLike;
import igrus.web.community.like.post_like.dto.response.PostLikeToggleResponse;
import igrus.web.community.like.post_like.repository.PostLikeRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.exception.PostDeletedException;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.community.post.service.support.PostAccessChecker;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 게시글 좋아요 토글 서비스.
 * 좋아요가 없으면 추가하고, 있으면 취소합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TogglePostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostAccessChecker postAccessChecker;

    /**
     * 좋아요를 토글합니다.
     * 좋아요가 없으면 추가하고, 있으면 취소합니다.
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 좋아요 토글 결과
     * @throws PostNotFoundException 게시글을 찾을 수 없는 경우
     * @throws PostDeletedException 삭제된 게시글인 경우
     */
    public PostLikeToggleResponse toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (post.isDeleted()) {
            throw new PostDeletedException(postId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        postAccessChecker.checkPostAccess(post, user);

        Optional<PostLike> existingLike = postLikeRepository.findByPostAndUser(post, user);

        if (existingLike.isPresent()) {
            // 좋아요 취소 (Hard Delete)
            postLikeRepository.delete(existingLike.get());
            int updated = postRepository.decrementLikeCount(postId);
            if (updated == 0) {
                log.warn("좋아요 카운터 감소 실패 - 게시글 없음 또는 카운트 이미 0: postId={}", postId);
            }

            // 원자적 UPDATE 후 영속성 컨텍스트가 초기화되므로 재조회
            post = postRepository.findById(postId)
                    .orElseThrow(() -> new PostNotFoundException(postId));
            log.info("게시글 좋아요 취소 - postId: {}, userId: {}, likeCount: {}", postId, userId, post.getLikeCount());
            return PostLikeToggleResponse.of(false, post.getLikeCount());
        } else {
            // 좋아요 추가
            PostLike postLike = PostLike.create(post, user);
            postLikeRepository.save(postLike);
            int updated = postRepository.incrementLikeCount(postId);
            if (updated == 0) {
                log.warn("좋아요 카운터 증가 실패 - 게시글 없음: postId={}", postId);
            }

            // 원자적 UPDATE 후 영속성 컨텍스트가 초기화되므로 재조회
            post = postRepository.findById(postId)
                    .orElseThrow(() -> new PostNotFoundException(postId));
            log.info("게시글 좋아요 추가 - postId: {}, userId: {}, likeCount: {}", postId, userId, post.getLikeCount());
            return PostLikeToggleResponse.of(true, post.getLikeCount());
        }
    }
}
