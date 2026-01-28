package igrus.web.community.comment.service;

import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.domain.CommentLike;
import igrus.web.community.comment.exception.CommentLikeException;
import igrus.web.community.comment.exception.CommentNotFoundException;
import igrus.web.community.comment.repository.CommentLikeRepository;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 좋아요 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentLikeService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;

    /**
     * 댓글에 좋아요를 추가합니다.
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     */
    @Transactional
    public void likeComment(Long commentId, Long userId) {
        Comment comment = findCommentById(commentId);
        User user = findUserById(userId);

        validateNotOwnComment(comment, user);
        validateNotAlreadyLiked(commentId, userId);

        CommentLike like = CommentLike.create(comment, user);
        commentLikeRepository.save(like);
    }

    /**
     * 댓글 좋아요를 취소합니다.
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     */
    @Transactional
    public void unlikeComment(Long commentId, Long userId) {
        if (!commentRepository.existsById(commentId)) {
            throw new CommentNotFoundException(commentId);
        }

        if (!commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            throw CommentLikeException.likeNotFound();
        }

        commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
    }

    /**
     * 댓글의 좋아요 수를 조회합니다.
     *
     * @param commentId 댓글 ID
     * @return 좋아요 수
     */
    public long getLikeCount(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new CommentNotFoundException(commentId);
        }
        return commentLikeRepository.countByCommentId(commentId);
    }

    /**
     * 사용자가 댓글에 좋아요했는지 확인합니다.
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @return 좋아요 여부
     */
    public boolean hasUserLiked(Long commentId, Long userId) {
        return commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
    }

    // === Private Helper Methods ===

    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    private void validateNotOwnComment(Comment comment, User user) {
        if (comment.isAuthor(user)) {
            throw CommentLikeException.cannotLikeOwnComment();
        }
    }

    private void validateNotAlreadyLiked(Long commentId, Long userId) {
        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            throw CommentLikeException.alreadyLiked();
        }
    }
}
