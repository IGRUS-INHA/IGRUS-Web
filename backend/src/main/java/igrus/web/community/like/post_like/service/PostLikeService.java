package igrus.web.community.like.post_like.service;

import igrus.web.community.like.post_like.domain.PostLike;
import igrus.web.community.like.post_like.dto.response.PostLikeStatusResponse;
import igrus.web.community.like.post_like.dto.response.PostLikeToggleResponse;
import igrus.web.community.like.post_like.dto.response.LikedPostResponse;
import igrus.web.community.like.post_like.repository.PostLikeRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.exception.PostDeletedException;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 게시글 좋아요 서비스.
 * 게시글 좋아요 토글, 상태 조회, 목록 조회 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

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
    @Transactional
    public PostLikeToggleResponse toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (post.isDeleted()) {
            throw new PostDeletedException(postId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Optional<PostLike> existingLike = postLikeRepository.findByPostAndUser(post, user);

        if (existingLike.isPresent()) {
            // 좋아요 취소 (Hard Delete)
            postLikeRepository.delete(existingLike.get());
            post.decrementLikeCount();
            postRepository.save(post);

            log.info("게시글 좋아요 취소 - postId: {}, userId: {}, likeCount: {}", postId, userId, post.getLikeCount());
            return PostLikeToggleResponse.of(false, post.getLikeCount());
        } else {
            // 좋아요 추가
            PostLike postLike = PostLike.create(post, user);
            postLikeRepository.save(postLike);
            post.incrementLikeCount();
            postRepository.save(post);

            log.info("게시글 좋아요 추가 - postId: {}, userId: {}, likeCount: {}", postId, userId, post.getLikeCount());
            return PostLikeToggleResponse.of(true, post.getLikeCount());
        }
    }

    /**
     * 사용자가 게시글에 좋아요했는지 확인합니다.
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 좋아요 여부
     */
    public boolean isLikedByUser(Long postId, Long userId) {
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }

    /**
     * 게시글의 좋아요 상태를 조회합니다.
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 좋아요 상태 응답
     * @throws PostNotFoundException 게시글을 찾을 수 없는 경우
     */
    public PostLikeStatusResponse getLikeStatus(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        boolean liked = postLikeRepository.existsByPostIdAndUserId(postId, userId);
        return PostLikeStatusResponse.of(liked, post.getLikeCount());
    }

    /**
     * 사용자가 좋아요한 게시글 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 좋아요한 게시글 목록
     */
    public Page<LikedPostResponse> getMyLikes(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Page<PostLike> postLikes = postLikeRepository.findAllByUserOrderByCreatedAtDesc(user, pageable);
        return postLikes.map(LikedPostResponse::from);
    }
}
