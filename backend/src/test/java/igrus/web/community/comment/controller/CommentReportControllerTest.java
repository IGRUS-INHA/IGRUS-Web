package igrus.web.community.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.domain.BoardPermission;
import igrus.web.community.board.repository.BoardPermissionRepository;
import igrus.web.community.board.repository.BoardRepository;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.domain.CommentReport;
import igrus.web.community.comment.domain.ReportStatus;
import igrus.web.community.comment.dto.request.CreateCommentReportRequest;
import igrus.web.community.comment.dto.request.UpdateReportStatusRequest;
import igrus.web.community.comment.repository.CommentReportRepository;
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
 * CommentReportController 통합 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>CMT-RPT-001~003: 댓글 신고</li>
 *     <li>CMT-RPT-004~008: 관리자 신고 관리</li>
 * </ul>
 * </p>
 */
@AutoConfigureMockMvc
@DisplayName("CommentReportController 통합 테스트")
class CommentReportControllerTest extends ServiceIntegrationTestBase {

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

    @Autowired
    private CommentReportRepository commentReportRepository;

    private static final String REPORT_URL = "/api/v1/comments/{commentId}/reports";
    private static final String ADMIN_REPORTS_URL = "/api/v1/admin/comment-reports";
    private static final String ADMIN_REPORT_URL = "/api/v1/admin/comment-reports/{reportId}";

    private User memberUser;
    private User memberUser2;
    private User operatorUser;

    private Board generalBoard;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        setUpBase();
        setupBoardData();
        memberUser = createAndSaveUser("20200001", "member@inha.edu", UserRole.MEMBER);
        memberUser2 = createAndSaveUser("20200003", "member2@inha.edu", UserRole.MEMBER);
        operatorUser = createAndSaveUser("20200002", "operator@inha.edu", UserRole.OPERATOR);
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

    private CommentReport createAndSaveReport(Comment comment, User reporter, String reason) {
        CommentReport report = CommentReport.create(comment, reporter, reason);
        return commentReportRepository.save(report);
    }

    @Nested
    @DisplayName("댓글 신고 테스트")
    class ReportCommentTest {

        @DisplayName("CMT-RPT-001: 댓글 신고 성공")
        @Test
        void reportComment_Success() throws Exception {
            // given
            CreateCommentReportRequest request = new CreateCommentReportRequest("부적절한 내용입니다.");

            // when & then
            mockMvc.perform(post(REPORT_URL, comment.getId())
                            .with(withAuth(memberUser2))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.commentId").value(comment.getId()))
                    .andExpect(jsonPath("$.reason").value("부적절한 내용입니다."))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @DisplayName("CMT-RPT-002: 중복 신고 시 400 Bad Request")
        @Test
        void reportComment_DuplicateReport_Returns400() throws Exception {
            // given: 이미 신고가 존재
            createAndSaveReport(comment, memberUser2, "첫 번째 신고");

            CreateCommentReportRequest request = new CreateCommentReportRequest("중복 신고 시도");

            // when & then
            mockMvc.perform(post(REPORT_URL, comment.getId())
                            .with(withAuth(memberUser2))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @DisplayName("CMT-RPT-003: 신고 사유 누락 시 400 Bad Request")
        @Test
        void reportComment_EmptyReason_Returns400() throws Exception {
            // given
            CreateCommentReportRequest request = new CreateCommentReportRequest("");

            // when & then
            mockMvc.perform(post(REPORT_URL, comment.getId())
                            .with(withAuth(memberUser2))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @DisplayName("존재하지 않는 댓글 신고 시 404 Not Found")
        @Test
        void reportComment_NonExistentComment_Returns404() throws Exception {
            // given
            CreateCommentReportRequest request = new CreateCommentReportRequest("신고 사유");

            // when & then
            mockMvc.perform(post(REPORT_URL, 99999L)
                            .with(withAuth(memberUser2))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @DisplayName("비인증 사용자 신고 시 401/403")
        @Test
        void reportComment_Unauthenticated_ReturnsUnauthorized() throws Exception {
            // given
            CreateCommentReportRequest request = new CreateCommentReportRequest("신고 사유");

            // when & then
            mockMvc.perform(post(REPORT_URL, comment.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("관리자 신고 목록 조회 테스트")
    class GetPendingReportsTest {

        @DisplayName("CMT-RPT-004: 관리자 신고 목록 조회 성공")
        @Test
        void getPendingReports_AsOperator_Success() throws Exception {
            // given: 대기 중인 신고 생성
            createAndSaveReport(comment, memberUser2, "첫 번째 신고");

            Comment comment2 = Comment.createComment(post, memberUser2, "다른 댓글", false);
            commentRepository.save(comment2);
            createAndSaveReport(comment2, memberUser, "두 번째 신고");

            // when & then
            mockMvc.perform(get(ADMIN_REPORTS_URL)
                            .with(withAuth(operatorUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @DisplayName("CMT-RPT-005: 정회원 관리자 API 접근 시 403 Forbidden")
        @Test
        void getPendingReports_AsMember_Returns403() throws Exception {
            // when & then
            mockMvc.perform(get(ADMIN_REPORTS_URL)
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @DisplayName("비인증 사용자 관리자 API 접근 시 401/403")
        @Test
        void getPendingReports_Unauthenticated_ReturnsUnauthorized() throws Exception {
            // when & then
            mockMvc.perform(get(ADMIN_REPORTS_URL)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("관리자 신고 처리 테스트")
    class UpdateReportStatusTest {

        @DisplayName("CMT-RPT-006: 신고 처리 RESOLVED 성공")
        @Test
        void updateReportStatus_Resolved_Success() throws Exception {
            // given
            CommentReport report = createAndSaveReport(comment, memberUser2, "신고 사유");
            UpdateReportStatusRequest request = new UpdateReportStatusRequest(ReportStatus.RESOLVED);

            // when & then
            mockMvc.perform(patch(ADMIN_REPORT_URL, report.getId())
                            .with(withAuth(operatorUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @DisplayName("CMT-RPT-007: 신고 처리 DISMISSED 성공")
        @Test
        void updateReportStatus_Dismissed_Success() throws Exception {
            // given
            CommentReport report = createAndSaveReport(comment, memberUser2, "신고 사유");
            UpdateReportStatusRequest request = new UpdateReportStatusRequest(ReportStatus.DISMISSED);

            // when & then
            mockMvc.perform(patch(ADMIN_REPORT_URL, report.getId())
                            .with(withAuth(operatorUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @DisplayName("CMT-RPT-008: 존재하지 않는 신고 처리 시 404 Not Found")
        @Test
        void updateReportStatus_NonExistentReport_Returns404() throws Exception {
            // given
            UpdateReportStatusRequest request = new UpdateReportStatusRequest(ReportStatus.RESOLVED);

            // when & then
            mockMvc.perform(patch(ADMIN_REPORT_URL, 99999L)
                            .with(withAuth(operatorUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @DisplayName("정회원 신고 처리 시 403 Forbidden")
        @Test
        void updateReportStatus_AsMember_Returns403() throws Exception {
            // given
            CommentReport report = createAndSaveReport(comment, memberUser2, "신고 사유");
            UpdateReportStatusRequest request = new UpdateReportStatusRequest(ReportStatus.RESOLVED);

            // when & then
            mockMvc.perform(patch(ADMIN_REPORT_URL, report.getId())
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @DisplayName("상태 누락 시 400 Bad Request")
        @Test
        void updateReportStatus_NullStatus_Returns400() throws Exception {
            // given
            CommentReport report = createAndSaveReport(comment, memberUser2, "신고 사유");
            String requestJson = "{}";

            // when & then
            mockMvc.perform(patch(ADMIN_REPORT_URL, report.getId())
                            .with(withAuth(operatorUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }
}
