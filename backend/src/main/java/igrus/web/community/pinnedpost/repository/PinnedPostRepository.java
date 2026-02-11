package igrus.web.community.pinnedpost.repository;

import igrus.web.community.pinnedpost.domain.PinnedPost;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PinnedPostRepository extends JpaRepository<PinnedPost, Long> {

    /**
     * 삭제되지 않은 고정 게시글을 ID로 조회합니다.
     */
    Optional<PinnedPost> findByIdAndDeletedFalse(Long id);

    /**
     * 삭제되지 않은 고정 게시글을 표시 순서대로 조회합니다.
     * 연관된 게시글이 삭제된 경우도 제외합니다.
     * Post, Author, Board, PinnedBy 정보를 함께 로드합니다.
     */
    @EntityGraph(attributePaths = {"post", "post.author", "post.board", "pinnedBy"})
    @Query("SELECT pp FROM PinnedPost pp JOIN pp.post p WHERE pp.deleted = false AND p.deleted = false ORDER BY pp.displayOrder ASC")
    List<PinnedPost> findAllActiveWithActivePost();

    /**
     * 특정 게시글이 이미 고정되어 있는지 확인합니다 (삭제되지 않은 것만).
     */
    boolean existsByPostIdAndDeletedFalse(Long postId);
}
