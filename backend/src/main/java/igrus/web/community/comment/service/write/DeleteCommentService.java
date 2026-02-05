package igrus.web.community.comment.service.write;

import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.service.support.CommentFinder;
import igrus.web.community.comment.service.support.CommentValidator;
import igrus.web.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 삭제 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteCommentService {

    private final CommentFinder commentFinder;
    private final CommentValidator commentValidator;

    /**
     * 댓글을 삭제합니다 (Soft Delete).
     *
     * @param postId    게시글 ID
     * @param commentId 댓글 ID
     * @param userId    삭제 요청자 ID
     */
    public void deleteComment(Long postId, Long commentId, Long userId) {
        Comment comment = commentFinder.findCommentById(commentId);
        User user = commentFinder.findUserById(userId);

        commentValidator.validateCommentBelongsToPost(comment, postId);
        commentValidator.validateCanDelete(comment, user);

        comment.delete(userId);
    }
}
