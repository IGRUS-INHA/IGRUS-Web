package igrus.web.community.pinnedpost.controller;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.repository.BoardRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PinnedPostController 통합 테스트.
 *
 * <p>고정 게시글 API의 인증/인가 및 정상 동작을 검증합니다.</p>
 *
 * <p>테스트 케이스:</p>
 * <ul>
 *     <li>고정 게시글 목록 조회는 공개 API (200)</li>
 *     <li>비인증 사용자 고정 생성/수정/삭제 차단 (401)</li>
 *     <li>일반 회원 고정 생성/수정/삭제 차단 (403)</li>
 *     <li>OPERATOR 고정 게시글 생성 허용 (201)</li>
 * </ul>
 */
@AutoConfigureMockMvc
@DisplayName("PinnedPostController 통합 테스트")
class PinnedPostControllerIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PostRepository postRepository;

    private User operatorUser;
    private User memberUser;
    private User associateUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        setUpBase();
        transactionTemplate.execute(status -> {
            operatorUser = createAndSaveUser("20230001", "operator@inha.edu", UserRole.OPERATOR);
            memberUser = createAndSaveUser("20230002", "member@inha.edu", UserRole.MEMBER);
            associateUser = createAndSaveUser("20230003", "associate@inha.edu", UserRole.ASSOCIATE);

            Board board = Board.create(BoardCode.NOTICES, "공지사항", "공지사항 게시판", false, false, 1);
            boardRepository.save(board);

            testPost = Post.createNotice(board, operatorUser, "테스트 게시글", "내용", true);
            postRepository.save(testPost);

            return null;
        });
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

    // ==================== 공개 API ====================

    @Nested
    @DisplayName("공개 API 접근")
    class PublicApiAccessTest {

        @Test
        @DisplayName("비인증 사용자 고정 게시글 목록 조회 -> 200")
        void getPinnedPostList_WithoutAuth_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/pinned-posts"))
                    .andExpect(status().isOk());
        }
    }

    // ==================== 비인증 사용자 (401) ====================

    @Nested
    @DisplayName("비인증 사용자 접근 차단 (401)")
    class UnauthenticatedAccessTest {

        @Test
        @DisplayName("비인증 사용자 게시글 고정 -> 401")
        void createPinnedPost_Unauthenticated_Returns401() throws Exception {
            String requestBody = String.format(
                    "{\"postId\":%d,\"displayOrder\":1}", testPost.getId());

            mockMvc.perform(post("/api/v1/pinned-posts")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("비인증 사용자 고정 게시글 순서 변경 -> 401")
        void updateDisplayOrder_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(put("/api/v1/pinned-posts/1/display-order")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"displayOrder\":2}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("비인증 사용자 고정 해제 -> 401")
        void deletePinnedPost_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(delete("/api/v1/pinned-posts/1")
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 일반 회원 접근 제한 (403) ====================

    @Nested
    @DisplayName("일반 회원 접근 제한 (403)")
    class MemberAccessRestrictionTest {

        @Test
        @DisplayName("MEMBER 게시글 고정 -> 403")
        void createPinnedPost_AsMember_Returns403() throws Exception {
            String requestBody = String.format(
                    "{\"postId\":%d,\"displayOrder\":1}", testPost.getId());

            mockMvc.perform(post("/api/v1/pinned-posts")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ASSOCIATE 게시글 고정 -> 403")
        void createPinnedPost_AsAssociate_Returns403() throws Exception {
            String requestBody = String.format(
                    "{\"postId\":%d,\"displayOrder\":1}", testPost.getId());

            mockMvc.perform(post("/api/v1/pinned-posts")
                            .with(withAuth(associateUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("MEMBER 고정 해제 -> 403")
        void deletePinnedPost_AsMember_Returns403() throws Exception {
            mockMvc.perform(delete("/api/v1/pinned-posts/1")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== 관리자 접근 허용 ====================

    @Nested
    @DisplayName("관리자 접근 허용 (OPERATOR/ADMIN)")
    class AdminAccessPermissionTest {

        @Test
        @DisplayName("OPERATOR 게시글 고정 -> 201")
        void createPinnedPost_AsOperator_Returns201() throws Exception {
            String requestBody = String.format(
                    "{\"postId\":%d,\"displayOrder\":1}", testPost.getId());

            mockMvc.perform(post("/api/v1/pinned-posts")
                            .with(withAuth(operatorUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.postId").value(testPost.getId()))
                    .andExpect(jsonPath("$.displayOrder").value(1));
        }
    }
}
