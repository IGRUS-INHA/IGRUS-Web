package igrus.web.community.post.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.service.read.GetBoardEntityService;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.dto.response.PostViewHistoryResponse;
import igrus.web.community.post.exception.PostAccessDeniedException;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.community.post.repository.PostViewRepository;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
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
 * 게시글 조회 기록 목록 조회 서비스.
 * OPERATOR 이상만 조회 가능.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetPostViewHistoryService {

    private final UserRepository userRepository;
    private final GetBoardEntityService getBoardEntityService;
    private final PostRepository postRepository;
    private final PostViewRepository postViewRepository;

    /**
     * 게시글 조회 기록 목록을 조회합니다.
     *
     * @param boardCode 게시판 코드
     * @param postId 게시글 ID
     * @param user 인증된 사용자 정보
     * @param pageable 페이징 정보
     * @return 조회 기록 페이지
     */
    @Transactional(readOnly = true)
    public Page<PostViewHistoryResponse> getPostViewHistory(String boardCode, Long postId,
                                                             AuthenticatedUser user, Pageable pageable) {
        User currentUser = userRepository.findById(user.userId())
                .orElseThrow(UserNotFoundException::new);

        // OPERATOR 이상만 조회 가능
        if (!currentUser.isOperatorOrAbove()) {
            throw new PostAccessDeniedException();
        }

        Board board = getBoardEntityService.getBoardEntity(boardCode);
        Post post = postRepository.findByBoardAndIdAndDeletedFalse(board, postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        return postViewRepository.findByPostWithViewer(post, pageable)
                .map(PostViewHistoryResponse::from);
    }
}
