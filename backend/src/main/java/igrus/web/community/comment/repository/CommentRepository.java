package igrus.web.community.comment.repository;

import igrus.web.community.comment.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    /**
     * 특정 사용자가 작성한 삭제되지 않은 댓글을 최신순으로 페이징 조회합니다.
     *
     * @param authorId 작성자 ID
     * @param pageable 페이징 정보
     * @return 댓글 페이지
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.post WHERE c.author.id = :authorId AND c.deleted = false ORDER BY c.createdAt DESC")
    Page<Comment> findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(@Param("authorId") Long authorId, Pageable pageable);

    /**
     * 특정 시각 이후에 생성된 삭제되지 않은 댓글 수를 조회합니다.
     *
     * @param startTime 기준 시각
     * @return 댓글 수
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.deleted = false AND c.createdAt >= :startTime")
    long countByDeletedFalseAndCreatedAtAfter(@Param("startTime") Instant startTime);

    /**
     * 특정 게시글의 삭제되지 않은 댓글을 일괄 soft delete합니다.
     *
     * @param postId    게시글 ID
     * @param deletedBy 삭제 수행자 ID
     * @param now       삭제 시각
     * @return 삭제된 댓글 수
     */
    @Modifying
    @Query("UPDATE Comment c SET c.deleted = true, c.deletedAt = :now, c.deletedBy = :deletedBy " +
            "WHERE c.post.id = :postId AND c.deleted = false")
    int softDeleteByPostId(@Param("postId") Long postId, @Param("deletedBy") Long deletedBy, @Param("now") Instant now);
}
