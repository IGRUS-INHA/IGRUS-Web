package igrus.web.community.post.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.domain.BoardPermission;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.dto.request.CreatePostRequest;
import igrus.web.community.post.dto.request.UpdatePostRequest;
import igrus.web.community.board.repository.BoardPermissionRepository;
import igrus.web.community.board.repository.BoardRepository;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.common.exception.ErrorCode;
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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Post 통합 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>BRD-070~071: 관리자 익명 게시글 조회 (현재 미구현, 테스트 스킵)</li>
 *     <li>BRD-082: 빈 게시판 조회</li>
 *     <li>BRD-083: 삭제된 게시글 직접 접근</li>
 *     <li>BRD-084: 익명 게시글 권한 없는 사용자 수정 시도</li>
 * </ul>
 * </p>
 */
@AutoConfigureMockMvc
@DisplayName("Post 통합 테스트")
class PostIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardPermissionRepository boardPermissionRepository;

    @Autowired
    private PostRepository postRepository;

    private static final String BASE_URL = "/api/v1/boards";

    private User memberUser;
    private User memberUser2;
    private User associateUser;
    private User operatorUser;

    private Board noticesBoard;
    private Board generalBoard;
    private Board insightBoard;

    @BeforeEach
    void setUp() {
        setUpBase();
        setupBoardData();
        memberUser = createAndSaveUser("20200001", "member@inha.edu", UserRole.MEMBER);
        memberUser2 = createAndSaveUser("20200002", "member2@inha.edu", UserRole.MEMBER);
        associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);
        operatorUser = createAndSaveUser("20200003", "operator@inha.edu", UserRole.OPERATOR);
    }

    private void setupBoardData() {
        // 게시판 생성
        noticesBoard = Board.create(BoardCode.NOTICES, "공지사항", "동아리 공지사항을 확인할 수 있습니다.", false, false, 1);
        generalBoard = Board.create(BoardCode.GENERAL, "자유게시판", "자유롭게 이야기를 나눌 수 있는 공간입니다.", true, true, 2);
        insightBoard = Board.create(BoardCode.INSIGHT, "정보공유", "유용한 정보를 공유하는 게시판입니다.", false, false, 3);

        boardRepository.save(noticesBoard);
        boardRepository.save(generalBoard);
        boardRepository.save(insightBoard);

        // 권한 설정 - notices
        boardPermissionRepository.save(BoardPermission.create(noticesBoard, UserRole.ASSOCIATE, true, false));
        boardPermissionRepository.save(BoardPermission.create(noticesBoard, UserRole.MEMBER, true, false));
        boardPermissionRepository.save(BoardPermission.create(noticesBoard, UserRole.OPERATOR, true, true));
        boardPermissionRepository.save(BoardPermission.create(noticesBoard, UserRole.ADMIN, true, true));

        // 권한 설정 - general
        boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.ASSOCIATE, false, false));
        boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.MEMBER, true, true));
        boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.OPERATOR, true, true));
        boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.ADMIN, true, true));

        // 권한 설정 - insight
        boardPermissionRepository.save(BoardPermission.create(insightBoard, UserRole.ASSOCIATE, false, false));
        boardPermissionRepository.save(BoardPermission.create(insightBoard, UserRole.MEMBER, true, true));
        boardPermissionRepository.save(BoardPermission.create(insightBoard, UserRole.OPERATOR, true, true));
        boardPermissionRepository.save(BoardPermission.create(insightBoard, UserRole.ADMIN, true, true));
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

    private Post createAndSavePost(Board board, User author, String title, String content) {
        Post post = Post.createPost(board, author, title, content);
        return postRepository.save(post);
    }

    private Post createAndSaveAnonymousPost(Board board, User author, String title, String content) {
        Post post = Post.createAnonymousPost(board, author, title, content);
        return postRepository.save(post);
    }

    @Nested
    @DisplayName("관리자 익명 게시글 조회 테스트")
    class AdminAnonymousViewTest {

        @DisplayName("BRD-070: 운영진이 익명 게시글 조회 시 작성자 '익명'으로 표시 (현재 구현 상태)")
        @Test
        void getPostDetail_AsOperator_AnonymousPost_ShowsAnonymous() throws Exception {
            // given: 익명 게시글 존재
            Post anonymousPost = createAndSaveAnonymousPost(generalBoard, memberUser, "익명 게시글", "익명 내용");

            // when & then: 운영진이 조회해도 현재 구현에서는 작성자가 "익명"으로 표시
            // Note: 테스트 케이스 BRD-070에서는 운영진이 실제 작성자를 볼 수 있어야 하나, 현재 미구현
            mockMvc.perform(get(BASE_URL + "/general/posts/" + anonymousPost.getId())
                            .with(withAuth(operatorUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authorName").value("익명"))
                    .andExpect(jsonPath("$.authorId").isEmpty())
                    .andExpect(jsonPath("$.isAnonymous").value(true));
        }

        @DisplayName("BRD-071: 정회원이 익명 게시글 조회 시 작성자 '익명'으로 표시")
        @Test
        void getPostDetail_AsMember_AnonymousPost_ShowsAnonymous() throws Exception {
            // given: 익명 게시글 존재
            Post anonymousPost = createAndSaveAnonymousPost(generalBoard, memberUser, "익명 게시글", "익명 내용");

            // when & then: 정회원이 조회 시 작성자 "익명"으로 표시
            mockMvc.perform(get(BASE_URL + "/general/posts/" + anonymousPost.getId())
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authorName").value("익명"))
                    .andExpect(jsonPath("$.authorId").isEmpty())
                    .andExpect(jsonPath("$.isAnonymous").value(true));
        }
    }

    @Nested
    @DisplayName("Edge Cases 테스트")
    class EdgeCasesTest {

        @DisplayName("BRD-082: 빈 게시판 조회 시 빈 목록 반환")
        @Test
        void getPostList_EmptyBoard_ReturnsEmptyList() throws Exception {
            // given: 게시글이 없는 상태

            // when & then
            mockMvc.perform(get(BASE_URL + "/general/posts")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.posts").isEmpty());
        }

        @DisplayName("BRD-083: 삭제된 게시글 직접 접근 시 404 반환")
        @Test
        void getPostDetail_DeletedPost_ReturnsNotFound() throws Exception {
            // given: 삭제된 게시글 생성
            Post post = createAndSavePost(generalBoard, memberUser, "삭제될 게시글", "내용");
            post.delete(memberUser.getId());
            postRepository.save(post);

            // when & then
            mockMvc.perform(get(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.POST_NOT_FOUND.getCode()));
        }

        @DisplayName("BRD-084: 익명 게시글 권한 없는 사용자 수정 시도 시 403 반환")
        @Test
        void updatePost_AnonymousPost_ByNonAuthor_ReturnsForbidden() throws Exception {
            // given: 타인의 익명 게시글 존재
            Post anonymousPost = createAndSaveAnonymousPost(generalBoard, memberUser, "익명 게시글", "익명 내용");

            UpdatePostRequest request = new UpdatePostRequest(
                    "수정된 제목",
                    "수정된 내용",
                    false,
                    List.of()
            );

            // when & then: 타인(memberUser2)이 수정 시도
            mockMvc.perform(put(BASE_URL + "/general/posts/" + anonymousPost.getId())
                            .with(withAuth(memberUser2))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.POST_ACCESS_DENIED.getCode()));
        }

        @DisplayName("삭제된 게시글 수정 시도 시 실패")
        @Test
        void updatePost_DeletedPost_ReturnsError() throws Exception {
            // given: 삭제된 게시글
            Post post = createAndSavePost(generalBoard, memberUser, "삭제될 게시글", "내용");
            post.delete(memberUser.getId());
            postRepository.save(post);

            UpdatePostRequest request = new UpdatePostRequest(
                    "수정된 제목",
                    "수정된 내용",
                    false,
                    List.of()
            );

            // when & then
            mockMvc.perform(put(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isGone());
        }

        @DisplayName("이미 삭제된 게시글 삭제 시도 시 실패")
        @Test
        void deletePost_AlreadyDeleted_ReturnsError() throws Exception {
            // given: 이미 삭제된 게시글
            Post post = createAndSavePost(generalBoard, memberUser, "삭제된 게시글", "내용");
            post.delete(memberUser.getId());
            postRepository.save(post);

            // when & then
            mockMvc.perform(delete(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isGone());
        }
    }

    @Nested
    @DisplayName("권한 검증 테스트")
    class PermissionTest {

        @DisplayName("준회원이 자유게시판 접근 시 403 반환")
        @Test
        void getPostList_AsAssociate_InGeneral_ReturnsForbidden() throws Exception {
            // when & then
            mockMvc.perform(get(BASE_URL + "/general/posts")
                            .with(withAuth(associateUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @DisplayName("작성자가 본인 게시글 수정 성공")
        @Test
        void updatePost_ByAuthor_Success() throws Exception {
            // given
            Post post = createAndSavePost(generalBoard, memberUser, "원본 제목", "원본 내용");

            UpdatePostRequest request = new UpdatePostRequest(
                    "수정된 제목",
                    "수정된 내용",
                    false,
                    List.of()
            );

            // when & then
            mockMvc.perform(put(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("수정된 제목"));
        }

        @DisplayName("작성자가 본인 게시글 삭제 성공")
        @Test
        void deletePost_ByAuthor_Success() throws Exception {
            // given
            Post post = createAndSavePost(generalBoard, memberUser, "삭제할 게시글", "내용");

            // when & then
            mockMvc.perform(delete(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @DisplayName("운영자가 타인의 게시글 삭제 성공")
        @Test
        void deletePost_ByOperator_Success() throws Exception {
            // given: memberUser가 작성한 게시글
            Post post = createAndSavePost(generalBoard, memberUser, "삭제할 게시글", "내용");

            // when & then: operatorUser가 삭제
            mockMvc.perform(delete(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(operatorUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @DisplayName("일반 회원이 타인의 게시글 삭제 시도 시 403 반환")
        @Test
        void deletePost_ByOtherMember_ReturnsForbidden() throws Exception {
            // given: memberUser가 작성한 게시글
            Post post = createAndSavePost(generalBoard, memberUser, "삭제할 게시글", "내용");

            // when & then: memberUser2가 삭제 시도
            mockMvc.perform(delete(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("조회수 테스트")
    class ViewCountTest {

        @DisplayName("게시글 상세 조회 시 조회수 증가")
        @Test
        void getPostDetail_IncreasesViewCount() throws Exception {
            // given
            Post post = createAndSavePost(generalBoard, memberUser, "제목", "내용");
            int initialViewCount = post.getViewCount();

            // when: 조회
            mockMvc.perform(get(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.viewCount").value(initialViewCount + 1));
        }
    }

    // ============================================================
    // PST 테스트 케이스 (post-test-cases.md 기준)
    // ============================================================

    @Nested
    @DisplayName("PST: 익명 게시글 통합 테스트")
    class PstAnonymousPostTest {

        @DisplayName("PST-002: 익명 게시글 작성")
        @Test
        void pst002_CreateAnonymousPost_Success() throws Exception {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "익명 게시글 제목",
                    "익명 게시글 내용입니다.",
                    true,  // isAnonymous
                    false,
                    false,
                    List.of()
            );

            // when
            String response = mockMvc.perform(post(BASE_URL + "/general/posts")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            Long postId = objectMapper.readTree(response).get("postId").asLong();

            // then: 조회 시 익명으로 표시
            mockMvc.perform(get(BASE_URL + "/general/posts/" + postId)
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isAnonymous").value(true))
                    .andExpect(jsonPath("$.authorName").value("익명"));
        }

        @DisplayName("PST-022: 익명 게시글 작성자 비노출")
        @Test
        void pst022_AnonymousPost_AuthorHidden() throws Exception {
            // given: 익명 게시글 생성
            Post anonymousPost = createAndSaveAnonymousPost(generalBoard, memberUser, "익명 게시글", "내용");

            // when & then: 타인이 조회 시 작성자 비노출
            mockMvc.perform(get(BASE_URL + "/general/posts/" + anonymousPost.getId())
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authorName").value("익명"))
                    .andExpect(jsonPath("$.authorId").isEmpty())
                    .andExpect(jsonPath("$.isAnonymous").value(true));
        }

        @DisplayName("PST-035: 익명 게시글 본인 수정")
        @Test
        void pst035_UpdateAnonymousPost_ByAuthor_Success() throws Exception {
            // given: 본인이 작성한 익명 게시글
            Post anonymousPost = createAndSaveAnonymousPost(generalBoard, memberUser, "원본 제목", "원본 내용");

            UpdatePostRequest request = new UpdatePostRequest(
                    "수정된 제목",
                    "수정된 내용",
                    false,
                    List.of()
            );

            // when & then: 본인이 수정 성공
            mockMvc.perform(put(BASE_URL + "/general/posts/" + anonymousPost.getId())
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("수정된 제목"));
        }

        @DisplayName("PST-036: 익명 게시글 타인 수정 시도")
        @Test
        void pst036_UpdateAnonymousPost_ByOther_Returns403() throws Exception {
            // given: 타인이 작성한 익명 게시글
            Post anonymousPost = createAndSaveAnonymousPost(generalBoard, memberUser, "익명 제목", "익명 내용");

            UpdatePostRequest request = new UpdatePostRequest(
                    "타인 수정 시도",
                    "타인 수정 내용",
                    false,
                    List.of()
            );

            // when & then: 타인이 수정 시도 시 403
            mockMvc.perform(put(BASE_URL + "/general/posts/" + anonymousPost.getId())
                            .with(withAuth(memberUser2))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @DisplayName("PST-046: 익명 게시글 본인 삭제")
        @Test
        void pst046_DeleteAnonymousPost_ByAuthor_Success() throws Exception {
            // given: 본인이 작성한 익명 게시글
            Post anonymousPost = createAndSaveAnonymousPost(generalBoard, memberUser, "삭제할 익명 게시글", "내용");

            // when & then: 본인이 삭제 성공
            mockMvc.perform(delete(BASE_URL + "/general/posts/" + anonymousPost.getId())
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @DisplayName("PST-047: 익명 게시글 타인 삭제 시도")
        @Test
        void pst047_DeleteAnonymousPost_ByOther_Returns403() throws Exception {
            // given: 타인이 작성한 익명 게시글
            Post anonymousPost = createAndSaveAnonymousPost(generalBoard, memberUser, "익명 게시글", "내용");

            // when & then: 타인(일반 회원)이 삭제 시도 시 403
            mockMvc.perform(delete(BASE_URL + "/general/posts/" + anonymousPost.getId())
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PST: 게시글 삭제 통합 테스트")
    class PstDeleteIntegrationTest {

        @DisplayName("PST-040: 본인 게시글 삭제 (Soft Delete)")
        @Test
        void pst040_DeletePost_ByAuthor_SoftDelete() throws Exception {
            // given
            Post post = createAndSavePost(generalBoard, memberUser, "삭제할 게시글", "내용");

            // when: 삭제
            mockMvc.perform(delete(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            // then: 삭제된 게시글 조회 시 404 (Soft Delete 적용됨)
            mockMvc.perform(get(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @DisplayName("PST-041: 삭제된 게시글 목록 제외")
        @Test
        void pst041_DeletedPost_ExcludedFromList() throws Exception {
            // given: 게시글 3개 생성 후 1개 삭제
            Post post1 = createAndSavePost(generalBoard, memberUser, "게시글 1", "내용");
            Post post2 = createAndSavePost(generalBoard, memberUser, "게시글 2", "내용");
            Post post3 = createAndSavePost(generalBoard, memberUser, "게시글 3", "내용");

            // 1개 삭제
            post2.delete(memberUser.getId());
            postRepository.save(post2);

            // when & then: 목록 조회 시 삭제된 게시글 제외 (2개만 표시)
            mockMvc.perform(get(BASE_URL + "/general/posts")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @DisplayName("PST-043: 관리자 타인 게시글 삭제")
        @Test
        void pst043_DeletePost_ByOperator_Success() throws Exception {
            // given: memberUser가 작성한 게시글
            Post post = createAndSavePost(generalBoard, memberUser, "타인 게시글", "내용");

            // when & then: operatorUser가 삭제 (OPERATOR는 타인 게시글 삭제 가능)
            mockMvc.perform(delete(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(operatorUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("PST: Edge Cases 테스트")
    class PstEdgeCasesTest {

        @DisplayName("PST-092: 삭제된 게시글 수정 시도")
        @Test
        void pst092_UpdateDeletedPost_ReturnsError() throws Exception {
            // given: 삭제된 게시글
            Post post = createAndSavePost(generalBoard, memberUser, "삭제된 게시글", "내용");
            post.delete(memberUser.getId());
            postRepository.save(post);

            UpdatePostRequest request = new UpdatePostRequest(
                    "수정 시도",
                    "수정 내용",
                    false,
                    List.of()
            );

            // when & then: 삭제된 게시글 수정 시도 시 에러 응답
            mockMvc.perform(put(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isGone());
        }
    }
}
