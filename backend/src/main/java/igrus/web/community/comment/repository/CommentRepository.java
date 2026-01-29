package igrus.web.community.comment.repository;

import igrus.web.community.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 댓글 리포지토리.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 게시글의 모든 댓글을 등록순으로 조회합니다. (삭제된 댓글 포함)
     *
     * @param postId 게시글 ID
     * @return 댓글 목록
     */
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    /**
     * 게시글의 삭제되지 않은 댓글을 등록순으로 조회합니다.
     *
     * @param postId 게시글 ID
     * @return 댓글 목록
     */
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.deleted = false ORDER BY c.createdAt ASC")
    List<Comment> findByPostIdAndNotDeletedOrderByCreatedAtAsc(@Param("postId") Long postId);

    /**
     * 부모 댓글의 대댓글 목록을 조회합니다.
     *
     * @param parentCommentId 부모 댓글 ID
     * @return 대댓글 목록
     */
    List<Comment> findByParentCommentId(Long parentCommentId);

    /**
     * 게시글의 삭제되지 않은 댓글 수를 조회합니다.
     *
     * @param postId 게시글 ID
     * @return 댓글 수
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId AND c.deleted = false")
    long countByPostIdAndNotDeleted(@Param("postId") Long postId);

    /**
     * 특정 사용자가 작성한 댓글인지 확인합니다.
     *
     * @param id       댓글 ID
     * @param authorId 작성자 ID
     * @return 작성자가 맞으면 true
     */
    boolean existsByIdAndAuthorId(Long id, Long authorId);
}
