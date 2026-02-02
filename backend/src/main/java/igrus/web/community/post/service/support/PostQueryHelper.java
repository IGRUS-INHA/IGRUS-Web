package igrus.web.community.post.service.support;

import igrus.web.community.board.domain.Board;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 목록 조회 시 사용되는 쿼리 헬퍼.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class PostQueryHelper {

    private final PostRepository postRepository;

    /**
     * 준회원이 공지사항 게시판에서 조회 가능한 게시글을 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<Post> getPostsForAssociateInNotices(Board board, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return postRepository.searchVisibleToAssociateByTitleOrContent(board, keyword, pageable);
        }
        return postRepository.findVisibleToAssociateByBoard(board, pageable);
    }

    /**
     * 질문 게시글만 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<Post> getQuestionPosts(Board board, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return postRepository.searchQuestionsByTitleOrContent(board, keyword, pageable);
        }
        return postRepository.findQuestionsByBoard(board, pageable);
    }

    /**
     * 일반 게시글을 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<Post> getRegularPosts(Board board, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return postRepository.searchByTitleOrContent(board, keyword, pageable);
        }
        return postRepository.findByBoardAndDeletedFalseOrderByCreatedAtDesc(board, pageable);
    }
}
