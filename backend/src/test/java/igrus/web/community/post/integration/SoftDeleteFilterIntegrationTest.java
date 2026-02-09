package igrus.web.community.post.integration;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.domain.BoardPermission;
import igrus.web.community.board.repository.BoardPermissionRepository;
import igrus.web.community.board.repository.BoardRepository;
import igrus.web.community.bookmark.domain.Bookmark;
import igrus.web.community.bookmark.repository.BookmarkRepository;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.community.like.post_like.domain.PostLike;
import igrus.web.community.like.post_like.repository.PostLikeRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 게시글 삭제 시 좋아요/북마크/댓글 필터링 통합 테스트.
 *
 * <p>게시글이 soft delete되면:
 * <ul>
 *     <li>좋아요 목록에서 해당 게시글이 제외되는지 검증</li>
 *     <li>북마크 목록에서 해당 게시글이 제외되는지 검증</li>
 *     <li>내 댓글 목록에서 삭제된 게시글의 댓글이 제외되는지 검증</li>
 * </ul>
 */
@AutoConfigureMockMvc
@DisplayName("게시글 soft delete 시 마이페이지 필터링 통합 테스트")
class SoftDeleteFilterIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardPermissionRepository boardPermissionRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private CommentRepository commentRepository;

    private User postAuthor;
    private User interactingUser;
    private Board generalBoard;
    private Post post;

    @BeforeEach
    void setUp() {
        setUpBase();

        generalBoard = Board.create(BoardCode.GENERAL, "자유게시판", "자유롭게 이야기를 나눌 수 있는 공간입니다.", true, true, 2);
        boardRepository.save(generalBoard);
        boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.MEMBER, true, true));

        postAuthor = createAndSaveUser("20200001", "author@inha.edu", UserRole.MEMBER);
        interactingUser = createAndSaveUser("20200002", "user@inha.edu", UserRole.MEMBER);

        post = Post.createPost(generalBoard, postAuthor, "테스트 게시글", "테스트 내용");
        postRepository.save(post);
    }

    private RequestPostProcessor withAuth(User user) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.getId(), user.getStudentId(), user.getRole().name()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(
                authenticatedUser, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        return authentication(auth);
    }

    @Nested
    @DisplayName("삭제된 게시글 좋아요 필터링")
    class PostLikeSoftDeleteFilterTest {

        @DisplayName("삭제된 게시글의 좋아요는 내 좋아요 목록에서 제외된다")
        @Test
        void getMyLikes_WhenPostDeleted_ExcludesFromList() throws Exception {
            // given: 좋아요 후 게시글 삭제
            Long postId = post.getId();
            transactionTemplate.executeWithoutResult(status -> {
                PostLike like = PostLike.create(post, interactingUser);
                postLikeRepository.save(like);
                postRepository.incrementLikeCount(postId);
                Post freshPost = postRepository.findById(postId).orElseThrow();
                freshPost.delete(postAuthor.getId());
                postRepository.save(freshPost);
            });

            // when & then
            mockMvc.perform(get("/api/v1/users/me/likes")
                            .with(withAuth(interactingUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.posts").isEmpty());
        }
    }

    @Nested
    @DisplayName("삭제된 게시글 북마크 필터링")
    class BookmarkSoftDeleteFilterTest {

        @DisplayName("삭제된 게시글의 북마크는 내 북마크 목록에서 제외된다")
        @Test
        void getMyBookmarks_WhenPostDeleted_ExcludesFromList() throws Exception {
            // given: 북마크 후 게시글 삭제
            transactionTemplate.executeWithoutResult(status -> {
                Bookmark bookmark = Bookmark.create(post, interactingUser);
                bookmarkRepository.save(bookmark);
                Post freshPost = postRepository.findById(post.getId()).orElseThrow();
                freshPost.delete(postAuthor.getId());
                postRepository.save(freshPost);
            });

            // when & then
            mockMvc.perform(get("/api/v1/users/me/bookmarks")
                            .with(withAuth(interactingUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.posts").isEmpty());
        }
    }

    @Nested
    @DisplayName("삭제된 게시글 댓글 필터링")
    class CommentSoftDeleteFilterTest {

        @DisplayName("게시글 삭제 시 soft delete된 댓글은 내 댓글 목록에서 제외된다")
        @Test
        void getMyComments_WhenPostDeletedAndCommentsSoftDeleted_ExcludesFromList() throws Exception {
            // given: 댓글 작성 후 게시글 삭제 (댓글도 함께 soft delete)
            transactionTemplate.executeWithoutResult(status -> {
                Comment comment = Comment.createComment(post, interactingUser, "테스트 댓글", false);
                commentRepository.save(comment);
                Post freshPost = postRepository.findById(post.getId()).orElseThrow();
                freshPost.delete(postAuthor.getId());
                postRepository.save(freshPost);
                commentRepository.softDeleteByPostId(post.getId(), postAuthor.getId(), java.time.Instant.now());
            });

            // when & then
            mockMvc.perform(get("/api/v1/mypage/comments")
                            .with(withAuth(interactingUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.comments").isEmpty());
        }
    }
}
