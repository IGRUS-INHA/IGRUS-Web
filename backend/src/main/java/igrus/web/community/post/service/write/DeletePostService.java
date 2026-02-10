package igrus.web.community.post.service.write;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.service.read.GetBoardEntityService;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.exception.PostAccessDeniedException;
import igrus.web.community.post.exception.PostDeletedException;
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

import java.time.Instant;

/**
 * 게시글 삭제 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeletePostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final GetBoardEntityService getBoardEntityService;

    /**
     * 게시글을 삭제합니다 (Soft Delete).
     *
     * @param boardCode 게시판 코드
     * @param postId 게시글 ID
     * @param authenticatedUser 인증된 사용자 정보
     */
    public void deletePost(String boardCode, Long postId, AuthenticatedUser authenticatedUser) {
        // 사용자 조회
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(UserNotFoundException::new);

        // 게시판 조회
        Board board = getBoardEntityService.getBoardEntity(boardCode);

        // 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        // 이미 삭제된 게시글인지 확인
        if (post.isDeleted()) {
            throw new PostDeletedException(postId);
        }

        // 게시글이 해당 게시판에 속하는지 확인
        if (!post.getBoard().getId().equals(board.getId())) {
            throw new PostNotFoundException(postId);
        }

        // 삭제 권한 확인 (작성자 본인 또는 OPERATOR 이상)
        if (!post.canDelete(user)) {
            throw new PostAccessDeniedException("게시글 삭제 권한이 없습니다");
        }

        // Soft Delete 적용
        post.delete(user.getId());

        // 게시글에 달린 댓글도 함께 Soft Delete
        int deletedCommentCount = commentRepository.softDeleteByPostId(postId, user.getId(), Instant.now());
        log.info("게시글 삭제 - postId: {}, 함께 삭제된 댓글 수: {}", postId, deletedCommentCount);
    }
}
