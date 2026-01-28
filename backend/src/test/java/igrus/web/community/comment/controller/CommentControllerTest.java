package igrus.web.community.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.domain.BoardPermission;
import igrus.web.community.board.repository.BoardPermissionRepository;
import igrus.web.community.board.repository.BoardRepository;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.dto.request.CreateCommentRequest;
import igrus.web.community.comment.repository.CommentRepository;
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
 * CommentController 통합 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>CMT-001~007: 댓글 작성</li>
 *     <li>CMT-010~014: 대댓글 작성</li>
 *     <li>CMT-020~025: 댓글 조회</li>
 *     <li>CMT-030~036: 댓글 삭제</li>
 * </ul>
 * </p>
 */
@AutoConfigureMockMvc
@DisplayName("CommentController 통합 테스트")
class CommentControllerTest extends ServiceIntegrationTestBase {

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

    @Autowired
    private CommentRepository commentRepository;

    private static final String BASE_URL = "/api/v1/posts";

    private User memberUser;
    private User memberUser2;
    private User associateUser;
    private User operatorUser;

    private Board generalBoard;
    private Board insightBoard;

    private Post generalPost;
    private Post insightPost;

    @BeforeEach
    void setUp() {
        setUpBase();
        setupBoardData();
        memberUser = createAndSaveUser("20200001", "member@inha.edu", UserRole.MEMBER);
        memberUser2 = createAndSaveUser("20200003", "member2@inha.edu", UserRole.MEMBER);
        associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);
        operatorUser = createAndSaveUser("20200002", "operator@inha.edu", UserRole.OPERATOR);
        setupPostData();
    }

    private void setupBoardData() {
        // 게시판 생성
        generalBoard = Board.create(BoardCode.GENERAL, "자유게시판", "자유롭게 이야기를 나눌 수 있는 공간입니다.", true, true, 2);
        insightBoard = Board.create(BoardCode.INSIGHT, "정보공유", "유용한 정보를 공유하는 게시판입니다.", false, false, 3);

        boardRepository.save(generalBoard);
        boardRepository.save(insightBoard);

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

    private void setupPostData() {
        generalPost = Post.createPost(generalBoard, memberUser, "자유게시판 게시글", "자유게시판 내용");
        insightPost = Post.createPost(insightBoard, memberUser, "정보공유 게시글", "정보공유 내용");

        postRepository.save(generalPost);
        postRepository.save(insightPost);
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

    private Comment createAndSaveComment(Post post, User author, String content, boolean isAnonymous) {
        Comment comment = Comment.createComment(post, author, content, isAnonymous);
        return commentRepository.save(comment);
    }

    private Comment createAndSaveReply(Post post, Comment parentComment, User author, String content, boolean isAnonymous) {
        Comment reply = Comment.createReply(post, parentComment, author, content, isAnonymous);
        return commentRepository.save(reply);
    }

    @Nested
    @DisplayName("댓글 작성 테스트")
    class CreateCommentTest {

        @DisplayName("CMT-001: 정회원이 일반 댓글 작성 성공")
        @Test
        void createComment_AsMember_Success() throws Exception {
            // given
            CreateCommentRequest request = new CreateCommentRequest("테스트 댓글입니다.", false);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + generalPost.getId() + "/comments")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.postId").value(generalPost.getId()))
                    .andExpect(jsonPath("$.content").value("테스트 댓글입니다."))
                    .andExpect(jsonPath("$.anonymous").value(false))
                    .andExpect(jsonPath("$.authorName").isNotEmpty());
        }

        @DisplayName("CMT-002: 익명 댓글 작성 성공 (자유게시판)")
        @Test
        void createComment_Anonymous_Success() throws Exception {
            // given
            CreateCommentRequest request = new CreateCommentRequest("익명 댓글입니다.", true);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + generalPost.getId() + "/comments")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.anonymous").value(true))
                    .andExpect(jsonPath("$.authorName").value("익명"))
                    .andExpect(jsonPath("$.authorId").doesNotExist());
        }

        @DisplayName("CMT-003: 정보공유 게시판에서 익명 옵션 시 400 Bad Request")
        @Test
        void createComment_AnonymousInInsight_Returns400() throws Exception {
            // given
            CreateCommentRequest request = new CreateCommentRequest("익명 댓글 시도", true);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + insightPost.getId() + "/comments")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @DisplayName("CMT-004: 501자 댓글 작성 시 400 Bad Request")
        @Test
        void createComment_TooLongContent_Returns400() throws Exception {
            // given
            String longContent = "가".repeat(501);
            CreateCommentRequest request = new CreateCommentRequest(longContent, false);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + generalPost.getId() + "/comments")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @DisplayName("CMT-005: 500자 댓글 정상 작성")
        @Test
        void createComment_MaxLengthContent_Success() throws Exception {
            // given
            String maxContent = "가".repeat(500);
            CreateCommentRequest request = new CreateCommentRequest(maxContent, false);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + generalPost.getId() + "/comments")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.content").value(maxContent));
        }

        @DisplayName("CMT-006: 빈 내용 댓글 작성 시 400 Bad Request")
        @Test
        void createComment_EmptyContent_Returns400() throws Exception {
            // given
            CreateCommentRequest request = new CreateCommentRequest("", false);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + generalPost.getId() + "/comments")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @DisplayName("CMT-007: 삭제된 게시글에 댓글 작성 시 400 Bad Request")
        @Test
        void createComment_DeletedPost_Returns400() throws Exception {
            // given: 삭제된 게시글
            Post deletedPost = Post.createPost(generalBoard, memberUser, "삭제될 게시글", "내용");
            deletedPost = postRepository.save(deletedPost);
            deletedPost.delete(memberUser.getId());
            postRepository.save(deletedPost);

            CreateCommentRequest request = new CreateCommentRequest("댓글 시도", false);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + deletedPost.getId() + "/comments")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @DisplayName("존재하지 않는 게시글에 댓글 작성 시 404 Not Found")
        @Test
        void createComment_NonExistentPost_Returns404() throws Exception {
            // given
            CreateCommentRequest request = new CreateCommentRequest("댓글 시도", false);

            // when & then
            mockMvc.perform(post(BASE_URL + "/99999/comments")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("대댓글 작성 테스트")
    class CreateReplyTest {

        @DisplayName("CMT-010: 대댓글 작성 성공")
        @Test
        void createReply_Success() throws Exception {
            // given
            Comment parentComment = createAndSaveComment(generalPost, memberUser, "부모 댓글", false);
            CreateCommentRequest request = new CreateCommentRequest("대댓글입니다.", false);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + generalPost.getId() + "/comments/" + parentComment.getId() + "/replies")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.parentCommentId").value(parentComment.getId()))
                    .andExpect(jsonPath("$.content").value("대댓글입니다."));
        }

        @DisplayName("CMT-011: 대댓글에 대댓글 작성 시 400 Bad Request")
        @Test
        void createReply_ToReply_Returns400() throws Exception {
            // given
            Comment parentComment = createAndSaveComment(generalPost, memberUser, "부모 댓글", false);
            Comment reply = createAndSaveReply(generalPost, parentComment, memberUser, "대댓글", false);
            CreateCommentRequest request = new CreateCommentRequest("대댓글의 대댓글 시도", false);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + generalPost.getId() + "/comments/" + reply.getId() + "/replies")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @DisplayName("CMT-012: 대댓글 익명 작성 성공 (자유게시판)")
        @Test
        void createReply_Anonymous_Success() throws Exception {
            // given
            Comment parentComment = createAndSaveComment(generalPost, memberUser, "부모 댓글", false);
            CreateCommentRequest request = new CreateCommentRequest("익명 대댓글입니다.", true);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + generalPost.getId() + "/comments/" + parentComment.getId() + "/replies")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.anonymous").value(true))
                    .andExpect(jsonPath("$.authorName").value("익명"));
        }

        @DisplayName("CMT-013: 대댓글 501자 작성 시 400 Bad Request")
        @Test
        void createReply_TooLongContent_Returns400() throws Exception {
            // given
            Comment parentComment = createAndSaveComment(generalPost, memberUser, "부모 댓글", false);
            String longContent = "가".repeat(501);
            CreateCommentRequest request = new CreateCommentRequest(longContent, false);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + generalPost.getId() + "/comments/" + parentComment.getId() + "/replies")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @DisplayName("존재하지 않는 부모 댓글에 대댓글 작성 시 404 Not Found")
        @Test
        void createReply_NonExistentParent_Returns404() throws Exception {
            // given
            CreateCommentRequest request = new CreateCommentRequest("대댓글 시도", false);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + generalPost.getId() + "/comments/99999/replies")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("댓글 조회 테스트")
    class GetCommentsTest {

        @DisplayName("CMT-020: 댓글 계층 구조 조회 성공")
        @Test
        void getComments_HierarchicalStructure_Success() throws Exception {
            // given
            Comment comment1 = createAndSaveComment(generalPost, memberUser, "첫번째 댓글", false);
            Comment comment2 = createAndSaveComment(generalPost, memberUser, "두번째 댓글", false);
            createAndSaveReply(generalPost, comment1, memberUser, "첫번째 댓글의 대댓글", false);

            // when & then
            mockMvc.perform(get(BASE_URL + "/" + generalPost.getId() + "/comments")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(3))
                    .andExpect(jsonPath("$.comments").isArray())
                    .andExpect(jsonPath("$.comments.length()").value(2))
                    .andExpect(jsonPath("$.comments[0].replies").isArray());
        }

        @DisplayName("CMT-021: 삭제된 댓글 '삭제된 댓글입니다' 표시")
        @Test
        void getComments_DeletedComment_ShowsDeletedMessage() throws Exception {
            // given
            Comment comment = createAndSaveComment(generalPost, memberUser, "삭제될 댓글", false);
            comment.delete(memberUser.getId());
            commentRepository.save(comment);

            // when & then
            mockMvc.perform(get(BASE_URL + "/" + generalPost.getId() + "/comments")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comments[0].content").value("삭제된 댓글입니다"))
                    .andExpect(jsonPath("$.comments[0].deleted").value(true));
        }

        @DisplayName("CMT-022: 삭제된 댓글의 대댓글 유지")
        @Test
        void getComments_DeletedParent_KeepsReplies() throws Exception {
            // given
            Comment parentComment = createAndSaveComment(generalPost, memberUser, "삭제될 부모 댓글", false);
            createAndSaveReply(generalPost, parentComment, memberUser, "대댓글은 유지", false);
            parentComment.delete(memberUser.getId());
            commentRepository.save(parentComment);

            // when & then
            mockMvc.perform(get(BASE_URL + "/" + generalPost.getId() + "/comments")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comments[0].content").value("삭제된 댓글입니다"))
                    .andExpect(jsonPath("$.comments[0].deleted").value(true))
                    .andExpect(jsonPath("$.comments[0].replies[0].content").value("대댓글은 유지"))
                    .andExpect(jsonPath("$.comments[0].replies[0].deleted").value(false));
        }

        @DisplayName("CMT-023: 댓글 등록순 정렬")
        @Test
        void getComments_SortedByCreatedAt() throws Exception {
            // given
            createAndSaveComment(generalPost, memberUser, "첫번째 댓글", false);
            createAndSaveComment(generalPost, memberUser, "두번째 댓글", false);
            createAndSaveComment(generalPost, memberUser, "세번째 댓글", false);

            // when & then: 등록순 (오래된 순)
            mockMvc.perform(get(BASE_URL + "/" + generalPost.getId() + "/comments")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comments[0].content").value("첫번째 댓글"))
                    .andExpect(jsonPath("$.comments[1].content").value("두번째 댓글"))
                    .andExpect(jsonPath("$.comments[2].content").value("세번째 댓글"));
        }

        @DisplayName("CMT-025: 익명 댓글 작성자 비노출")
        @Test
        void getComments_AnonymousComment_HidesAuthor() throws Exception {
            // given
            createAndSaveComment(generalPost, memberUser, "익명 댓글", true);

            // when & then
            mockMvc.perform(get(BASE_URL + "/" + generalPost.getId() + "/comments")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comments[0].content").value("익명 댓글"))
                    .andExpect(jsonPath("$.comments[0].anonymous").value(true))
                    .andExpect(jsonPath("$.comments[0].authorName").value("익명"))
                    .andExpect(jsonPath("$.comments[0].authorId").doesNotExist());
        }

        @DisplayName("빈 댓글 목록 조회")
        @Test
        void getComments_Empty_ReturnsEmptyList() throws Exception {
            // when & then
            mockMvc.perform(get(BASE_URL + "/" + generalPost.getId() + "/comments")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(0))
                    .andExpect(jsonPath("$.comments").isEmpty());
        }
    }

    @Nested
    @DisplayName("댓글 삭제 테스트")
    class DeleteCommentTest {

        @DisplayName("CMT-030: 본인 댓글 삭제 성공 (Soft Delete)")
        @Test
        void deleteComment_ByAuthor_Success() throws Exception {
            // given
            Comment comment = createAndSaveComment(generalPost, memberUser, "삭제할 댓글", false);

            // when & then
            mockMvc.perform(delete(BASE_URL + "/" + generalPost.getId() + "/comments/" + comment.getId())
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNoContent());

            // 삭제 후 조회 시 "삭제된 댓글입니다" 표시 확인
            mockMvc.perform(get(BASE_URL + "/" + generalPost.getId() + "/comments")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comments[0].content").value("삭제된 댓글입니다"))
                    .andExpect(jsonPath("$.comments[0].deleted").value(true));
        }

        @DisplayName("CMT-033: 타인 댓글 삭제 시 403 Forbidden")
        @Test
        void deleteComment_ByOther_Returns403() throws Exception {
            // given: memberUser가 작성한 댓글
            Comment comment = createAndSaveComment(generalPost, memberUser, "타인 댓글", false);

            // when & then: memberUser2가 삭제 시도
            mockMvc.perform(delete(BASE_URL + "/" + generalPost.getId() + "/comments/" + comment.getId())
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @DisplayName("CMT-034: 관리자 타인 댓글 삭제 성공")
        @Test
        void deleteComment_ByOperator_Success() throws Exception {
            // given: memberUser가 작성한 댓글
            Comment comment = createAndSaveComment(generalPost, memberUser, "삭제할 댓글", false);

            // when & then: operatorUser가 삭제
            mockMvc.perform(delete(BASE_URL + "/" + generalPost.getId() + "/comments/" + comment.getId())
                            .with(withAuth(operatorUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @DisplayName("CMT-035: 익명 댓글 본인 삭제 성공")
        @Test
        void deleteComment_AnonymousByAuthor_Success() throws Exception {
            // given: 익명으로 작성한 댓글
            Comment anonymousComment = createAndSaveComment(generalPost, memberUser, "익명 댓글", true);

            // when & then: 본인이 삭제
            mockMvc.perform(delete(BASE_URL + "/" + generalPost.getId() + "/comments/" + anonymousComment.getId())
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @DisplayName("CMT-036: 익명 댓글 타인 삭제 시 403 Forbidden")
        @Test
        void deleteComment_AnonymousByOther_Returns403() throws Exception {
            // given: memberUser가 익명으로 작성한 댓글
            Comment anonymousComment = createAndSaveComment(generalPost, memberUser, "익명 댓글", true);

            // when & then: memberUser2가 삭제 시도
            mockMvc.perform(delete(BASE_URL + "/" + generalPost.getId() + "/comments/" + anonymousComment.getId())
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @DisplayName("존재하지 않는 댓글 삭제 시 404 Not Found")
        @Test
        void deleteComment_NonExistent_Returns404() throws Exception {
            // when & then
            mockMvc.perform(delete(BASE_URL + "/" + generalPost.getId() + "/comments/99999")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("인증/권한 테스트")
    class AuthenticationTest {

        @DisplayName("준회원 댓글 작성 시 403 Forbidden")
        @Test
        void createComment_AsAssociate_Returns403() throws Exception {
            // given
            CreateCommentRequest request = new CreateCommentRequest("댓글 시도", false);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + generalPost.getId() + "/comments")
                            .with(withAuth(associateUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @DisplayName("비인증 사용자 댓글 작성 시 401/403")
        @Test
        void createComment_Unauthenticated_ReturnsUnauthorized() throws Exception {
            // given
            CreateCommentRequest request = new CreateCommentRequest("댓글 시도", false);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + generalPost.getId() + "/comments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().is4xxClientError());
        }
    }
}
