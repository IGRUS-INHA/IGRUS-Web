package igrus.web.community.post.repository;

import igrus.web.community.board.domain.Board;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * 게시글 Repository.
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 삭제되지 않은 게시글을 ID로 조회합니다.
     *
     * @param id 게시글 ID
     * @return 게시글 Optional
     */
    Optional<Post> findByIdAndDeletedFalse(Long id);

    /**
     * 특정 게시판의 삭제되지 않은 게시글을 최신순으로 페이징 조회합니다.
     *
     * @param board    게시판
     * @param pageable 페이징 정보
     * @return 게시글 페이지
     */
    Page<Post> findByBoardAndDeletedFalseOrderByCreatedAtDesc(Board board, Pageable pageable);

    /**
     * 특정 게시판의 질문 태그가 달린 삭제되지 않은 게시글을 페이징 조회합니다.
     *
     * @param board    게시판
     * @param pageable 페이징 정보
     * @return 질문 게시글 페이지
     */
    Page<Post> findByBoardAndIsQuestionTrueAndDeletedFalse(Board board, Pageable pageable);

    /**
     * 특정 작성자가 지정된 시간 이후에 작성한 게시글 수를 조회합니다.
     * (도배 방지 등의 목적으로 사용)
     *
     * @param author 작성자
     * @param after  기준 시간
     * @return 게시글 수
     */
    long countByAuthorAndCreatedAtAfter(User author, Instant after);

    /**
     * 특정 게시판에서 제목 또는 내용에 키워드가 포함된 삭제되지 않은 게시글을 검색합니다.
     *
     * @param board    게시판
     * @param keyword  검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 게시글 페이지
     */
    @Query("SELECT p FROM Post p WHERE p.board = :board " +
           "AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
           "AND p.deleted = false " +
           "ORDER BY p.createdAt DESC")
    Page<Post> searchByTitleOrContent(@Param("board") Board board,
                                      @Param("keyword") String keyword,
                                      Pageable pageable);

    /**
     * 특정 게시판에서 질문 태그가 달린 삭제되지 않은 게시글을 최신순으로 페이징 조회합니다.
     *
     * @param board    게시판
     * @param pageable 페이징 정보
     * @return 질문 게시글 페이지
     */
    @Query("SELECT p FROM Post p WHERE p.board = :board " +
           "AND p.isQuestion = true " +
           "AND p.deleted = false " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findQuestionsByBoard(@Param("board") Board board, Pageable pageable);

    /**
     * 특정 게시판에서 질문 태그가 달리고 키워드가 포함된 삭제되지 않은 게시글을 검색합니다.
     *
     * @param board    게시판
     * @param keyword  검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 질문 게시글 페이지
     */
    @Query("SELECT p FROM Post p WHERE p.board = :board " +
           "AND p.isQuestion = true " +
           "AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
           "AND p.deleted = false " +
           "ORDER BY p.createdAt DESC")
    Page<Post> searchQuestionsByTitleOrContent(@Param("board") Board board,
                                               @Param("keyword") String keyword,
                                               Pageable pageable);

    /**
     * 공지사항 게시판에서 준회원에게 공개된 삭제되지 않은 게시글을 최신순으로 페이징 조회합니다.
     *
     * @param board    게시판
     * @param pageable 페이징 정보
     * @return 준회원 공개 게시글 페이지
     */
    @Query("SELECT p FROM Post p WHERE p.board = :board " +
           "AND p.isVisibleToAssociate = true " +
           "AND p.deleted = false " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findVisibleToAssociateByBoard(@Param("board") Board board, Pageable pageable);

    /**
     * 공지사항 게시판에서 준회원에게 공개되고 키워드가 포함된 삭제되지 않은 게시글을 검색합니다.
     *
     * @param board    게시판
     * @param keyword  검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 준회원 공개 게시글 페이지
     */
    @Query("SELECT p FROM Post p WHERE p.board = :board " +
           "AND p.isVisibleToAssociate = true " +
           "AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
           "AND p.deleted = false " +
           "ORDER BY p.createdAt DESC")
    Page<Post> searchVisibleToAssociateByTitleOrContent(@Param("board") Board board,
                                                        @Param("keyword") String keyword,
                                                        Pageable pageable);

    /**
     * 특정 게시판에서 특정 게시글 ID로 삭제되지 않은 게시글을 조회합니다.
     *
     * @param board  게시판
     * @param postId 게시글 ID
     * @return 게시글 Optional
     */
    Optional<Post> findByBoardAndIdAndDeletedFalse(Board board, Long postId);
}
