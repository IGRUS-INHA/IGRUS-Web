package igrus.web.community.pinnedpost.service.write;

import igrus.web.community.pinnedpost.domain.PinnedPost;
import igrus.web.community.pinnedpost.dto.request.CreatePinnedPostRequest;
import igrus.web.community.pinnedpost.dto.response.PinnedPostDetailResponse;
import igrus.web.community.pinnedpost.repository.PinnedPostRepository;
import igrus.web.community.pinnedpost.service.support.ValidatePinnedPostService;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreatePinnedPostService {

    private final PinnedPostRepository pinnedPostRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ValidatePinnedPostService validatePinnedPostService;

    /**
     * 게시글을 메인 페이지에 고정합니다.
     *
     * @param request           고정 게시글 생성 요청
     * @param authenticatedUser 인증된 사용자 (OPERATOR 이상)
     * @return 생성된 고정 게시글 응답
     */
    public PinnedPostDetailResponse createPinnedPost(
            CreatePinnedPostRequest request,
            AuthenticatedUser authenticatedUser
    ) {
        Post post = postRepository.findByIdAndDeletedFalse(request.postId())
                .orElseThrow(() -> new PostNotFoundException(request.postId()));

        validatePinnedPostService.validateNotAlreadyPinned(post.getId());

        User pinnedBy = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(UserNotFoundException::new);

        PinnedPost pinnedPost = PinnedPost.create(post, pinnedBy, request.displayOrder());
        PinnedPost saved = pinnedPostRepository.save(pinnedPost);

        log.info("게시글 고정 완료 - postId: {}, displayOrder: {}, pinnedBy: {}",
                post.getId(), request.displayOrder(), pinnedBy.getId());

        return PinnedPostDetailResponse.from(saved);
    }
}
