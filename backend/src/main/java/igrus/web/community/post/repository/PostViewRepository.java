package igrus.web.community.post.repository;

import igrus.web.community.post.domain.Post;
import igrus.web.community.post.domain.PostView;
import igrus.web.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 게시글 조회 기록 Repository.
 */
@Repository
public interface PostViewRepository extends JpaRepository<PostView, Long> {

    /**
     * 특정 게시글의 총 조회 수를 조회합니다.
     *
     * @param post 게시글
     * @return 총 조회 수
     */
    long countByPost(Post post);

    /**
     * 특정 게시글의 고유 조회자 수를 조회합니다.
     *
     * @param post 게시글
     * @return 고유 조회자 수
     */
    @Query("SELECT COUNT(DISTINCT pv.viewer.id) FROM PostView pv WHERE pv.post = :post")
    long countDistinctViewersByPost(@Param("post") Post post);

    /**
     * 특정 사용자가 조회한 게시글 수를 조회합니다.
     *
     * @param viewer 조회자
     * @return 조회한 게시글 수
     */
    long countByViewer(User viewer);

    /**
     * 특정 게시글의 조회 기록을 최신순으로 페이징 조회합니다.
     *
     * @param post     게시글
     * @param pageable 페이징 정보
     * @return 조회 기록 페이지
     */
    @Query("SELECT pv FROM PostView pv JOIN FETCH pv.viewer WHERE pv.post = :post ORDER BY pv.viewedAt DESC")
    Page<PostView> findByPostWithViewer(@Param("post") Post post, Pageable pageable);

    /**
     * 특정 게시글 ID의 조회 수를 조회합니다.
     *
     * @param postId 게시글 ID
     * @return 조회 수
     */
    @Query("SELECT COUNT(pv) FROM PostView pv WHERE pv.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);
}
