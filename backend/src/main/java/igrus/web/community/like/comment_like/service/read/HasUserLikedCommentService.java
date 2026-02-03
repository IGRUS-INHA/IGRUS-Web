package igrus.web.community.like.comment_like.service.read;

import igrus.web.community.like.comment_like.repository.CommentLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자의 댓글 좋아요 여부 확인 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class HasUserLikedCommentService {

    private final CommentLikeRepository commentLikeRepository;

    /**
     * 사용자가 댓글에 좋아요했는지 확인합니다.
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @return 좋아요 여부
     */
    @Transactional(readOnly = true)
    public boolean hasUserLiked(Long commentId, Long userId) {
        return commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
    }
}
