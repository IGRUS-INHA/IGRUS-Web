package igrus.web.community.like.comment_like.service.write;

import igrus.web.community.like.comment_like.repository.CommentLikeRepository;
import igrus.web.community.like.comment_like.service.support.CommentLikeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 좋아요 취소 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UnlikeCommentService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentLikeValidator commentLikeValidator;

    /**
     * 댓글 좋아요를 취소합니다.
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     */
    public void unlikeComment(Long commentId, Long userId) {
        commentLikeValidator.validateCommentExists(commentId);
        commentLikeValidator.validateLikeExists(commentId, userId);

        commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
    }
}
