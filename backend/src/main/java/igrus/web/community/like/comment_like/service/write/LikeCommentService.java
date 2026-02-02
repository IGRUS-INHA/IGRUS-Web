package igrus.web.community.like.comment_like.service.write;

import igrus.web.community.comment.domain.Comment;
import igrus.web.community.like.comment_like.domain.CommentLike;
import igrus.web.community.like.comment_like.repository.CommentLikeRepository;
import igrus.web.community.like.comment_like.service.support.CommentLikeValidator;
import igrus.web.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 좋아요 추가 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LikeCommentService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentLikeValidator commentLikeValidator;

    /**
     * 댓글에 좋아요를 추가합니다.
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     */
    public void likeComment(Long commentId, Long userId) {
        Comment comment = commentLikeValidator.findCommentById(commentId);
        User user = commentLikeValidator.findUserById(userId);

        commentLikeValidator.validateNotOwnComment(comment, user);
        commentLikeValidator.validateNotAlreadyLiked(commentId, userId);

        CommentLike like = CommentLike.create(comment, user);
        commentLikeRepository.save(like);
    }
}
