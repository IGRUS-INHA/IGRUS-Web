package igrus.web.community.bookmark.repository;

import igrus.web.community.bookmark.domain.Bookmark;
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
 * 북마크 레포지토리.
 */
@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /**
     * 특정 게시글과 사용자의 북마크를 조회합니다.
     */
    Optional<Bookmark> findByPostAndUser(Post post, User user);

    /**
     * 특정 게시글과 사용자의 북마크 존재 여부를 확인합니다.
     */
    boolean existsByPostAndUser(Post post, User user);

    /**
     * 특정 게시글과 사용자의 북마크를 삭제합니다.
     */
    @Modifying
    @Query("DELETE FROM Bookmark b WHERE b.post = :post AND b.user = :user")
    void deleteByPostAndUser(@Param("post") Post post, @Param("user") User user);

    /**
     * 사용자가 북마크한 게시글 목록을 최신순으로 조회합니다.
     */
    @EntityGraph(attributePaths = {"post", "post.board", "post.author"})
    @Query("SELECT b FROM Bookmark b WHERE b.user = :user ORDER BY b.createdAt DESC")
    Page<Bookmark> findAllByUserOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);

    /**
     * 특정 게시글 ID와 사용자 ID로 북마크 존재 여부를 확인합니다.
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Bookmark b WHERE b.post.id = :postId AND b.user.id = :userId")
    boolean existsByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);
}
