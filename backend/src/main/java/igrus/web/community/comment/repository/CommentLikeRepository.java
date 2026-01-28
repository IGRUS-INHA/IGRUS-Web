package igrus.web.community.comment.repository;

import igrus.web.community.comment.domain.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 댓글 좋아요 리포지토리.
 */
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    /**
     * 특정 사용자가 특정 댓글에 누른 좋아요를 조회합니다.
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @return 좋아요 정보
     */
    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    /**
     * 특정 사용자가 특정 댓글에 좋아요를 눌렀는지 확인합니다.
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @return 좋아요 여부
     */
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    /**
     * 특정 댓글의 좋아요 수를 조회합니다.
     *
     * @param commentId 댓글 ID
     * @return 좋아요 수
     */
    long countByCommentId(Long commentId);

    /**
     * 특정 사용자가 특정 댓글에 누른 좋아요를 삭제합니다.
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     */
    void deleteByCommentIdAndUserId(Long commentId, Long userId);
}
