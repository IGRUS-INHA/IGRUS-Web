package igrus.web.community.post.repository;

import igrus.web.community.post.domain.Post;
import igrus.web.community.post.domain.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 게시글 이미지 Repository.
 */
@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    /**
     * 특정 게시글의 이미지를 표시 순서대로 조회합니다.
     *
     * @param post 게시글
     * @return 이미지 목록
     */
    List<PostImage> findByPostOrderByDisplayOrderAsc(Post post);

    /**
     * 특정 게시글의 모든 이미지를 삭제합니다.
     *
     * @param post 게시글
     */
    void deleteByPost(Post post);

    /**
     * 특정 게시글의 이미지 수를 조회합니다.
     *
     * @param post 게시글
     * @return 이미지 수
     */
    long countByPost(Post post);
}
