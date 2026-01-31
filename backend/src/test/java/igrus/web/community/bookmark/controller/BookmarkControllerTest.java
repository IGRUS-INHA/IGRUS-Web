package igrus.web.community.bookmark.controller;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.domain.BoardPermission;
import igrus.web.community.board.repository.BoardPermissionRepository;
import igrus.web.community.board.repository.BoardRepository;
import igrus.web.community.bookmark.domain.Bookmark;
import igrus.web.community.bookmark.repository.BookmarkRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.common.ServiceIntegrationTestBase;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BookmarkController 통합 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>LKB-010~013: 북마크 토글</li>
 *     <li>LKB-020~024: 북마크 목록 조회</li>
 *     <li>LKB-041: 삭제된 게시글 북마크</li>
 * </ul>
 * </p>
 */
@AutoConfigureMockMvc
@DisplayName("BookmarkController 통합 테스트")
class BookmarkControllerTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardPermissionRepository boardPermissionRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    private User memberUser;
    private User memberUser2;
    private User associateUser;

    private Board generalBoard;
    private Post post;

    @BeforeEach
    void setUp() {
        setUpBase();
        setupBoardData();
        memberUser = createAndSaveUser("20200001", "member@inha.edu", UserRole.MEMBER);
        memberUser2 = createAndSaveUser("20200003", "member2@inha.edu", UserRole.MEMBER);
        associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);
        setupPostData();
    }

    private void setupBoardData() {
        generalBoard = Board.create(BoardCode.GENERAL, "자유게시판", "자유롭게 이야기를 나눌 수 있는 공간입니다.", true, true, 2);
        boardRepository.save(generalBoard);

        boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.ASSOCIATE, false, false));
        boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.MEMBER, true, true));
        boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.OPERATOR, true, true));
        boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.ADMIN, true, true));
    }

    private void setupPostData() {
        post = Post.createPost(generalBoard, memberUser, "테스트 게시글", "테스트 내용");
        postRepository.save(post);
    }

    private RequestPostProcessor withAuth(User user) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.getId(),
                user.getStudentId(),
                user.getRole().name()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        return authentication(auth);
    }

    private void createBookmark(Post post, User user) {
        Bookmark bookmark = Bookmark.create(post, user);
        bookmarkRepository.save(bookmark);
    }

    @Nested
    @DisplayName("북마크 토글 테스트")
    class ToggleBookmarkTest {

        @DisplayName("LKB-010: 북마크 추가 성공")
        @Test
        void toggleBookmark_AddBookmark_Success() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/bookmarks")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookmarked").value(true));
        }

        @DisplayName("LKB-011: 북마크 취소 성공")
        @Test
        void toggleBookmark_RemoveBookmark_Success() throws Exception {
            // given: 북마크가 이미 존재
            createBookmark(post, memberUser2);

            // when & then
            mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/bookmarks")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookmarked").value(false));
        }

        @DisplayName("LKB-013: 북마크 토글 반복 (중복 호출 시 토글 동작)")
        @Test
        void toggleBookmark_Repeat_TogglesCorrectly() throws Exception {
            // 첫번째 호출: 북마크 추가
            mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/bookmarks")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookmarked").value(true));

            // 두번째 호출: 북마크 취소
            mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/bookmarks")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookmarked").value(false));

            // 세번째 호출: 다시 북마크 추가
            mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/bookmarks")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookmarked").value(true));
        }

        @DisplayName("LKB-041: 삭제된 게시글 북마크 시 410 Gone")
        @Test
        void toggleBookmark_DeletedPost_Returns410() throws Exception {
            // given: 삭제된 게시글
            Post deletedPost = Post.createPost(generalBoard, memberUser, "삭제된 게시글", "내용");
            deletedPost = postRepository.save(deletedPost);
            deletedPost.delete(memberUser.getId());
            postRepository.save(deletedPost);

            // when & then
            mockMvc.perform(post("/api/v1/posts/" + deletedPost.getId() + "/bookmarks")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isGone());
        }

        @DisplayName("존재하지 않는 게시글 북마크 시 404 Not Found")
        @Test
        void toggleBookmark_NonExistentPost_Returns404() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/posts/99999/bookmarks")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @DisplayName("준회원 북마크 시 403 Forbidden")
        @Test
        void toggleBookmark_AsAssociate_Returns403() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/bookmarks")
                            .with(withAuth(associateUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("북마크 상태 조회 테스트")
    class GetBookmarkStatusTest {

        @DisplayName("북마크 상태 조회 - bookmarked=true")
        @Test
        void getBookmarkStatus_Bookmarked_ReturnsTrue() throws Exception {
            // given
            createBookmark(post, memberUser);

            // when & then
            mockMvc.perform(get("/api/v1/posts/" + post.getId() + "/bookmarks/status")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookmarked").value(true));
        }

        @DisplayName("북마크 상태 조회 - bookmarked=false")
        @Test
        void getBookmarkStatus_NotBookmarked_ReturnsFalse() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/posts/" + post.getId() + "/bookmarks/status")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookmarked").value(false));
        }

        @DisplayName("존재하지 않는 게시글 상태 조회 시 404 Not Found")
        @Test
        void getBookmarkStatus_NonExistentPost_Returns404() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/posts/99999/bookmarks/status")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("내 북마크 목록 조회 테스트")
    class GetMyBookmarksTest {

        @DisplayName("LKB-020: 내 북마크 목록 조회 성공")
        @Test
        void getMyBookmarks_Success() throws Exception {
            // given: 여러 게시글에 북마크
            Post post2 = Post.createPost(generalBoard, memberUser, "게시글2", "내용2");
            postRepository.save(post2);
            createBookmark(post, memberUser2);
            createBookmark(post2, memberUser2);

            // when & then
            mockMvc.perform(get("/api/v1/users/me/bookmarks")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }

        @DisplayName("LKB-022: 북마크 목록 페이지네이션")
        @Test
        void getMyBookmarks_Pagination_Success() throws Exception {
            // given: 25개 게시글에 북마크
            for (int i = 0; i < 25; i++) {
                Post newPost = Post.createPost(generalBoard, memberUser, "게시글 " + i, "내용 " + i);
                newPost = postRepository.save(newPost);
                createBookmark(newPost, memberUser2);
            }

            // when & then: 기본 페이지 크기 20개
            mockMvc.perform(get("/api/v1/users/me/bookmarks")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(25))
                    .andExpect(jsonPath("$.content.length()").value(20))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }

        @DisplayName("LKB-024: 빈 북마크 목록 조회")
        @Test
        void getMyBookmarks_Empty_ReturnsEmptyList() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/users/me/bookmarks")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @DisplayName("LKB-091: 삭제된 게시글 북마크 목록에 표시")
        @Test
        void getMyBookmarks_DeletedPost_ShowsDeletedMessage() throws Exception {
            // given: 북마크 후 게시글 삭제
            createBookmark(post, memberUser2);
            post.delete(memberUser.getId());
            postRepository.save(post);

            // when & then
            mockMvc.perform(get("/api/v1/users/me/bookmarks")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].isDeleted").value(true))
                    .andExpect(jsonPath("$.content[0].deletedMessage").value("삭제된 게시글입니다"));
        }

        @DisplayName("준회원 북마크 목록 조회 시 403 Forbidden")
        @Test
        void getMyBookmarks_AsAssociate_Returns403() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/users/me/bookmarks")
                            .with(withAuth(associateUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }
}
