package igrus.web.community.like.comment_like.service.support;

import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.exception.CommentNotFoundException;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.community.like.comment_like.exception.CommentLikeException;
import igrus.web.community.like.comment_like.repository.CommentLikeRepository;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 좋아요 관련 공통 검증 및 조회 로직.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class CommentLikeValidator {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;

    /**
     * 댓글을 조회합니다.
     *
     * @param commentId 댓글 ID
     * @return 댓글 엔티티
     * @throws CommentNotFoundException 댓글이 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    public Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
    }

    /**
     * 사용자를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 사용자 엔티티
     * @throws IllegalArgumentException 사용자가 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    /**
     * 댓글이 존재하는지 확인합니다.
     *
     * @param commentId 댓글 ID
     * @throws CommentNotFoundException 댓글이 존재하지 않는 경우
     */
    public void validateCommentExists(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new CommentNotFoundException(commentId);
        }
    }

    /**
     * 본인 댓글에 좋아요할 수 없는지 검증합니다.
     *
     * @param comment 댓글
     * @param user    사용자
     * @throws CommentLikeException 본인 댓글에 좋아요하는 경우
     */
    public void validateNotOwnComment(Comment comment, User user) {
        if (comment.isAuthor(user)) {
            throw CommentLikeException.cannotLikeOwnComment();
        }
    }

    /**
     * 이미 좋아요했는지 검증합니다.
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @throws CommentLikeException 이미 좋아요한 경우
     */
    public void validateNotAlreadyLiked(Long commentId, Long userId) {
        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            throw CommentLikeException.alreadyLiked();
        }
    }

    /**
     * 좋아요가 존재하는지 검증합니다.
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @throws CommentLikeException 좋아요가 존재하지 않는 경우
     */
    public void validateLikeExists(Long commentId, Long userId) {
        if (!commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            throw CommentLikeException.likeNotFound();
        }
    }
}
