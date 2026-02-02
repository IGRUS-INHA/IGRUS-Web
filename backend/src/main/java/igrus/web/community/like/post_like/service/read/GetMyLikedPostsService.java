package igrus.web.community.like.post_like.service.read;

import igrus.web.community.like.post_like.domain.PostLike;
import igrus.web.community.like.post_like.dto.response.LikedPostResponse;
import igrus.web.community.like.post_like.repository.PostLikeRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 좋아요 게시글 목록 조회 서비스.
 * 사용자가 좋아요한 게시글 목록을 조회합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetMyLikedPostsService {

    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;

    /**
     * 사용자가 좋아요한 게시글 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 좋아요한 게시글 목록
     */
    @Transactional(readOnly = true)
    public Page<LikedPostResponse> getMyLikes(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Page<PostLike> postLikes = postLikeRepository.findAllByUserOrderByCreatedAtDesc(user, pageable);
        return postLikes.map(LikedPostResponse::from);
    }
}
