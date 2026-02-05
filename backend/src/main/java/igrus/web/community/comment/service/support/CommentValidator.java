package igrus.web.community.comment.service.support;

import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.exception.CommentAccessDeniedException;
import igrus.web.community.comment.exception.CommentNotFoundException;
import igrus.web.community.comment.exception.InvalidCommentException;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 검증 로직.
 */
@Component
@Transactional
public class CommentValidator {

    public void validatePostNotDeleted(Post post) {
        if (post.isDeleted()) {
            throw InvalidCommentException.postDeletedCannotComment();
        }
    }

    public void validateAnonymousOption(Post post, boolean isAnonymous) {
        if (isAnonymous && !post.getBoard().getAllowsAnonymous()) {
            throw InvalidCommentException.anonymousNotAllowed();
        }
    }

    public void validateParentCommentBelongsToPost(Comment parentComment, Long postId) {
        if (!parentComment.getPost().getId().equals(postId)) {
            throw new CommentNotFoundException(parentComment.getId());
        }
    }

    public void validateCommentBelongsToPost(Comment comment, Long postId) {
        if (!comment.getPost().getId().equals(postId)) {
            throw new CommentNotFoundException(comment.getId());
        }
    }

    public void validateCanReplyTo(Comment parentComment) {
        if (!parentComment.canReplyTo()) {
            throw InvalidCommentException.replyToReplyNotAllowed();
        }
    }

    public void validateCanDelete(Comment comment, User user) {
        if (!comment.canDelete(user)) {
            throw new CommentAccessDeniedException();
        }
    }
}
