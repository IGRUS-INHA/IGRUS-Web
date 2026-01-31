package igrus.web.community.like.post_like.controller;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.domain.BoardPermission;
import igrus.web.community.board.repository.BoardPermissionRepository;
import igrus.web.community.board.repository.BoardRepository;
import igrus.web.community.like.post_like.domain.PostLike;
import igrus.web.community.like.post_like.repository.PostLikeRepository;
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
 * PostLikeController 통합 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>LKB-001~006: 게시글 좋아요 토글</li>
 *     <li>LKB-030~034: 좋아요 목록 조회</li>
 *     <li>LKB-040: 삭제된 게시글 좋아요</li>
 * </ul>
 * </p>
 */
@AutoConfigureMockMvc
@DisplayName("PostLikeController 통합 테스트")
class PostLikeControllerTest extends ServiceIntegrationTestBase {

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

    private void createLike(Post post, User user) {
        PostLike like = PostLike.create(post, user);
        postLikeRepository.save(like);
        post.incrementLikeCount();
        postRepository.save(post);
    }

    @Nested
    @DisplayName("게시글 좋아요 토글 테스트")
    class ToggleLikeTest {

        @DisplayName("LKB-001: 게시글 좋아요 추가 성공")
        @Test
        void toggleLike_AddLike_Success() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/likes")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(true))
                    .andExpect(jsonPath("$.likeCount").value(1));
        }

        @DisplayName("LKB-002: 게시글 좋아요 취소 성공")
        @Test
        void toggleLike_RemoveLike_Success() throws Exception {
            // given: 좋아요가 이미 존재
            createLike(post, memberUser2);

            // when & then
            mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/likes")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(false))
                    .andExpect(jsonPath("$.likeCount").value(0));
        }

        @DisplayName("LKB-003: 본인 게시글 좋아요 가능")
        @Test
        void toggleLike_OwnPost_Success() throws Exception {
            // when & then: 작성자 본인이 좋아요
            mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/likes")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(true));
        }

        @DisplayName("LKB-006: 좋아요 토글 반복 (중복 호출 시 토글 동작)")
        @Test
        void toggleLike_Repeat_TogglesCorrectly() throws Exception {
            // 첫번째 호출: 좋아요 추가
            mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/likes")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(true));

            // 두번째 호출: 좋아요 취소
            mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/likes")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(false));

            // 세번째 호출: 다시 좋아요 추가
            mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/likes")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(true));
        }

        @DisplayName("LKB-040: 삭제된 게시글 좋아요 시 410 Gone")
        @Test
        void toggleLike_DeletedPost_Returns410() throws Exception {
            // given: 삭제된 게시글
            Post deletedPost = Post.createPost(generalBoard, memberUser, "삭제된 게시글", "내용");
            deletedPost = postRepository.save(deletedPost);
            deletedPost.delete(memberUser.getId());
            postRepository.save(deletedPost);

            // when & then
            mockMvc.perform(post("/api/v1/posts/" + deletedPost.getId() + "/likes")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isGone());
        }

        @DisplayName("존재하지 않는 게시글 좋아요 시 404 Not Found")
        @Test
        void toggleLike_NonExistentPost_Returns404() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/posts/99999/likes")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @DisplayName("준회원 좋아요 시 403 Forbidden")
        @Test
        void toggleLike_AsAssociate_Returns403() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/likes")
                            .with(withAuth(associateUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("게시글 좋아요 상태 조회 테스트")
    class GetLikeStatusTest {

        @DisplayName("좋아요 상태 조회 - liked=true")
        @Test
        void getLikeStatus_Liked_ReturnsTrue() throws Exception {
            // given
            createLike(post, memberUser);

            // when & then
            mockMvc.perform(get("/api/v1/posts/" + post.getId() + "/likes/status")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(true))
                    .andExpect(jsonPath("$.likeCount").value(1));
        }

        @DisplayName("좋아요 상태 조회 - liked=false")
        @Test
        void getLikeStatus_NotLiked_ReturnsFalse() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/posts/" + post.getId() + "/likes/status")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.liked").value(false))
                    .andExpect(jsonPath("$.likeCount").value(0));
        }

        @DisplayName("존재하지 않는 게시글 상태 조회 시 404 Not Found")
        @Test
        void getLikeStatus_NonExistentPost_Returns404() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/posts/99999/likes/status")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("내 좋아요 목록 조회 테스트")
    class GetMyLikesTest {

        @DisplayName("LKB-030: 내 좋아요 목록 조회 성공")
        @Test
        void getMyLikes_Success() throws Exception {
            // given: 여러 게시글에 좋아요
            Post post2 = Post.createPost(generalBoard, memberUser, "게시글2", "내용2");
            postRepository.save(post2);
            createLike(post, memberUser2);
            createLike(post2, memberUser2);

            // when & then
            mockMvc.perform(get("/api/v1/users/me/likes")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }

        @DisplayName("LKB-032: 좋아요 목록 페이지네이션")
        @Test
        void getMyLikes_Pagination_Success() throws Exception {
            // given: 25개 게시글에 좋아요
            for (int i = 0; i < 25; i++) {
                Post newPost = Post.createPost(generalBoard, memberUser, "게시글 " + i, "내용 " + i);
                newPost = postRepository.save(newPost);
                createLike(newPost, memberUser2);
            }

            // when & then: 기본 페이지 크기 20개
            mockMvc.perform(get("/api/v1/users/me/likes")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(25))
                    .andExpect(jsonPath("$.content.length()").value(20))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }

        @DisplayName("LKB-034: 빈 좋아요 목록 조회")
        @Test
        void getMyLikes_Empty_ReturnsEmptyList() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/users/me/likes")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @DisplayName("LKB-090: 삭제된 게시글 좋아요 목록에 표시")
        @Test
        void getMyLikes_DeletedPost_ShowsDeletedMessage() throws Exception {
            // given: 좋아요 생성 (createLike 헬퍼 미사용, 버전 충돌 방지)
            PostLike like = PostLike.create(post, memberUser2);
            postLikeRepository.save(like);
            post.incrementLikeCount();
            post.delete(memberUser.getId());
            postRepository.save(post);

            // when & then
            mockMvc.perform(get("/api/v1/users/me/likes")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].isDeleted").value(true))
                    .andExpect(jsonPath("$.content[0].deletedMessage").value("삭제된 게시글입니다"));
        }

        @DisplayName("준회원 좋아요 목록 조회 시 403 Forbidden")
        @Test
        void getMyLikes_AsAssociate_Returns403() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/users/me/likes")
                            .with(withAuth(associateUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }
}
