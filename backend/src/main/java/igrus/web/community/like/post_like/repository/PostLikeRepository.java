package igrus.web.community.like.post_like.repository;

import igrus.web.community.like.post_like.domain.PostLike;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 게시글 좋아요 레포지토리.
 */
@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    /**
     * 특정 게시글과 사용자의 좋아요를 조회합니다.
     */
    Optional<PostLike> findByPostAndUser(Post post, User user);

    /**
     * 특정 게시글과 사용자의 좋아요 존재 여부를 확인합니다.
     */
    boolean existsByPostAndUser(Post post, User user);

    /**
     * 특정 게시글과 사용자의 좋아요를 삭제합니다.
     */
    @Modifying
    @Query("DELETE FROM PostLike l WHERE l.post = :post AND l.user = :user")
    void deleteByPostAndUser(@Param("post") Post post, @Param("user") User user);

    /**
     * 사용자가 좋아요한 게시글 목록을 최신순으로 조회합니다.
     */
    @EntityGraph(attributePaths = {"post", "post.board", "post.author"})
    @Query("SELECT l FROM PostLike l WHERE l.user = :user ORDER BY l.createdAt DESC")
    Page<PostLike> findAllByUserOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);

    /**
     * 특정 게시글의 좋아요 수를 조회합니다.
     */
    long countByPost(Post post);

    /**
     * 특정 게시글 ID와 사용자 ID로 좋아요 존재 여부를 확인합니다.
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM PostLike l WHERE l.post.id = :postId AND l.user.id = :userId")
    boolean existsByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);
}
