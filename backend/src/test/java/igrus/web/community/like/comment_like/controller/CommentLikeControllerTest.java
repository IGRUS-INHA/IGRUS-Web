package igrus.web.community.like.comment_like.controller;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.domain.BoardPermission;
import igrus.web.community.board.repository.BoardPermissionRepository;
import igrus.web.community.board.repository.BoardRepository;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.community.like.comment_like.domain.CommentLike;
import igrus.web.community.like.comment_like.repository.CommentLikeRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CommentLikeController 통합 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>CMT-040~042: 댓글 좋아요 추가/취소</li>
 * </ul>
 * </p>
 */
@AutoConfigureMockMvc
@DisplayName("CommentLikeController 통합 테스트")
class CommentLikeControllerTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardPermissionRepository boardPermissionRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    private static final String BASE_URL = "/api/v1/comments";

    private User memberUser;
    private User memberUser2;
    private User associateUser;

    private Board generalBoard;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        setUpBase();
        setupBoardData();
        memberUser = createAndSaveUser("20200001", "member@inha.edu", UserRole.MEMBER);
        memberUser2 = createAndSaveUser("20200003", "member2@inha.edu", UserRole.MEMBER);
        associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);
        setupPostAndCommentData();
    }

    private void setupBoardData() {
        generalBoard = Board.create(BoardCode.GENERAL, "자유게시판", "자유롭게 이야기를 나눌 수 있는 공간입니다.", true, true, 2);
        boardRepository.save(generalBoard);

        boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.ASSOCIATE, false, false));
        boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.MEMBER, true, true));
        boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.OPERATOR, true, true));
        boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.ADMIN, true, true));
    }

    private void setupPostAndCommentData() {
        post = Post.createPost(generalBoard, memberUser, "테스트 게시글", "테스트 내용");
        postRepository.save(post);

        comment = Comment.createComment(post, memberUser, "테스트 댓글", false);
        commentRepository.save(comment);
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

    private void createCommentLike(Comment comment, User user) {
        CommentLike like = CommentLike.create(comment, user);
        commentLikeRepository.save(like);
    }

    @Nested
    @DisplayName("댓글 좋아요 추가 테스트")
    class LikeCommentTest {

        @DisplayName("CMT-040: 댓글 좋아요 추가 성공")
        @Test
        void likeComment_Success() throws Exception {
            // when & then: memberUser2가 memberUser의 댓글에 좋아요
            mockMvc.perform(post(BASE_URL + "/" + comment.getId() + "/likes")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }

        @DisplayName("CMT-042: 본인 댓글 좋아요 시 400 Bad Request")
        @Test
        void likeComment_OwnComment_Returns400() throws Exception {
            // when & then: memberUser가 본인 댓글에 좋아요 시도
            mockMvc.perform(post(BASE_URL + "/" + comment.getId() + "/likes")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @DisplayName("이미 좋아요한 댓글에 중복 좋아요 시 400 Bad Request")
        @Test
        void likeComment_AlreadyLiked_Returns400() throws Exception {
            // given: 이미 좋아요가 존재
            createCommentLike(comment, memberUser2);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + comment.getId() + "/likes")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @DisplayName("존재하지 않는 댓글 좋아요 시 404 Not Found")
        @Test
        void likeComment_NonExistentComment_Returns404() throws Exception {
            // when & then
            mockMvc.perform(post(BASE_URL + "/99999/likes")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @DisplayName("준회원 댓글 좋아요 시 403 Forbidden")
        @Test
        void likeComment_AsAssociate_Returns403() throws Exception {
            // when & then
            mockMvc.perform(post(BASE_URL + "/" + comment.getId() + "/likes")
                            .with(withAuth(associateUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("댓글 좋아요 취소 테스트")
    class UnlikeCommentTest {

        @DisplayName("CMT-041: 댓글 좋아요 취소 성공")
        @Test
        void unlikeComment_Success() throws Exception {
            // given: 좋아요가 존재
            createCommentLike(comment, memberUser2);

            // when & then
            mockMvc.perform(delete(BASE_URL + "/" + comment.getId() + "/likes")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @DisplayName("좋아요하지 않은 댓글 취소 시 404 Not Found")
        @Test
        void unlikeComment_NotLiked_Returns404() throws Exception {
            // when & then
            mockMvc.perform(delete(BASE_URL + "/" + comment.getId() + "/likes")
                            .with(withAuth(memberUser2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @DisplayName("존재하지 않는 댓글 좋아요 취소 시 404 Not Found")
        @Test
        void unlikeComment_NonExistentComment_Returns404() throws Exception {
            // when & then
            mockMvc.perform(delete(BASE_URL + "/99999/likes")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @DisplayName("준회원 댓글 좋아요 취소 시 403 Forbidden")
        @Test
        void unlikeComment_AsAssociate_Returns403() throws Exception {
            // when & then
            mockMvc.perform(delete(BASE_URL + "/" + comment.getId() + "/likes")
                            .with(withAuth(associateUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }
}
