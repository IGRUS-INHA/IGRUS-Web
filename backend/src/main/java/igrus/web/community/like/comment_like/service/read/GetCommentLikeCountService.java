package igrus.web.community.like.comment_like.service.read;

import igrus.web.community.like.comment_like.repository.CommentLikeRepository;
import igrus.web.community.like.comment_like.service.support.CommentLikeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 좋아요 수 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetCommentLikeCountService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentLikeValidator commentLikeValidator;

    /**
     * 댓글의 좋아요 수를 조회합니다.
     *
     * @param commentId 댓글 ID
     * @return 좋아요 수
     */
    @Transactional(readOnly = true)
    public long getLikeCount(Long commentId) {
        commentLikeValidator.validateCommentExists(commentId);
        return commentLikeRepository.countByCommentId(commentId);
    }
}
