package igrus.web.community.comment.service;

import igrus.web.community.board.domain.Board;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.domain.CommentReport;
import igrus.web.community.comment.domain.ReportStatus;
import igrus.web.community.comment.dto.request.CreateCommentReportRequest;
import igrus.web.community.comment.dto.request.UpdateReportStatusRequest;
import igrus.web.community.comment.dto.response.CommentReportResponse;
import igrus.web.community.comment.exception.CommentNotFoundException;
import igrus.web.community.comment.exception.CommentReportException;
import igrus.web.community.comment.repository.CommentReportRepository;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static igrus.web.common.fixture.TestEntityIdAssigner.withId;
import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.CommentTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * CommentReportService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>CMT-050: 댓글 신고 접수</li>
 *     <li>CMT-051: 동일 댓글 중복 신고 방지</li>
 *     <li>CMT-052: 관리자 신고 검토 대기열 확인</li>
 *     <li>CMT-053: 관리자 신고 처리</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentReportService 단위 테스트")
class CommentReportServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentReportRepository commentReportRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentReportService commentReportService;

    private Board generalBoard;
    private User memberUser;
    private User anotherMember;
    private User operatorUser;
    private Post post;
    private Comment targetComment;

    @BeforeEach
    void setUp() {
        generalBoard = generalBoard();
        memberUser = createMemberWithId();
        anotherMember = createAnotherMemberWithId();
        operatorUser = createOperatorWithId();
        post = normalPost(generalBoard, memberUser);
        targetComment = comment(post, memberUser);
    }

    @Nested
    @DisplayName("댓글 신고")
    class ReportComment {

        @Test
        @DisplayName("CMT-050: 댓글 신고 성공")
        void reportComment_success() {
            // given
            CreateCommentReportRequest request = new CreateCommentReportRequest("신고 사유입니다.");
            given(commentRepository.findById(targetComment.getId())).willReturn(Optional.of(targetComment));
            given(userRepository.findById(anotherMember.getId())).willReturn(Optional.of(anotherMember));
            given(commentReportRepository.existsByCommentIdAndReporterId(targetComment.getId(), anotherMember.getId()))
                    .willReturn(false);
            given(commentReportRepository.save(any(CommentReport.class))).willAnswer(invocation -> {
                CommentReport report = invocation.getArgument(0);
                return withId(report, 1L);
            });

            // when
            CommentReportResponse response = commentReportService.reportComment(
                    targetComment.getId(), request, anotherMember.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getReason()).isEqualTo("신고 사유입니다.");
            assertThat(response.getStatus()).isEqualTo(ReportStatus.PENDING);
            verify(commentReportRepository).save(any(CommentReport.class));
        }

        @Test
        @DisplayName("CMT-051: 중복 신고 시 CommentReportException 발생")
        void reportComment_duplicate_fails() {
            // given
            CreateCommentReportRequest request = new CreateCommentReportRequest("신고 사유입니다.");
            given(commentRepository.findById(targetComment.getId())).willReturn(Optional.of(targetComment));
            given(userRepository.findById(anotherMember.getId())).willReturn(Optional.of(anotherMember));
            given(commentReportRepository.existsByCommentIdAndReporterId(targetComment.getId(), anotherMember.getId()))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> commentReportService.reportComment(
                    targetComment.getId(), request, anotherMember.getId()))
                    .isInstanceOf(CommentReportException.class);
        }

        @Test
        @DisplayName("존재하지 않는 댓글 신고 시 CommentNotFoundException 발생")
        void reportComment_commentNotFound() {
            // given
            CreateCommentReportRequest request = new CreateCommentReportRequest("신고 사유입니다.");
            given(commentRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentReportService.reportComment(999L, request, anotherMember.getId()))
                    .isInstanceOf(CommentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("신고 목록 조회")
    class GetPendingReports {

        @Test
        @DisplayName("CMT-052: 대기 중인 신고 목록 조회 성공")
        void getPendingReports_success() {
            // given
            CommentReport report1 = withId(createCommentReport(targetComment, anotherMember), 1L);
            CommentReport report2 = withId(createCommentReport(targetComment, anotherMember), 2L);
            given(commentReportRepository.findByStatus(ReportStatus.PENDING))
                    .willReturn(List.of(report1, report2));

            // when
            List<CommentReportResponse> responses = commentReportService.getPendingReports();

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses).allMatch(r -> r.getStatus() == ReportStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("신고 처리")
    class UpdateReportStatus {

        @Test
        @DisplayName("CMT-053: 관리자가 신고 처리 (승인) 성공")
        void updateReportStatus_resolved_success() {
            // given
            CommentReport report = withId(createCommentReport(targetComment, anotherMember), 1L);
            UpdateReportStatusRequest request = new UpdateReportStatusRequest(ReportStatus.RESOLVED);
            given(commentReportRepository.findById(1L)).willReturn(Optional.of(report));
            given(userRepository.findById(operatorUser.getId())).willReturn(Optional.of(operatorUser));

            // when
            commentReportService.updateReportStatus(1L, request, operatorUser.getId());

            // then
            assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
            assertThat(report.getResolvedBy()).isEqualTo(operatorUser);
        }

        @Test
        @DisplayName("관리자가 신고 반려 성공")
        void updateReportStatus_dismissed_success() {
            // given
            CommentReport report = withId(createCommentReport(targetComment, anotherMember), 1L);
            UpdateReportStatusRequest request = new UpdateReportStatusRequest(ReportStatus.DISMISSED);
            given(commentReportRepository.findById(1L)).willReturn(Optional.of(report));
            given(userRepository.findById(operatorUser.getId())).willReturn(Optional.of(operatorUser));

            // when
            commentReportService.updateReportStatus(1L, request, operatorUser.getId());

            // then
            assertThat(report.getStatus()).isEqualTo(ReportStatus.DISMISSED);
        }

        @Test
        @DisplayName("존재하지 않는 신고 처리 시 CommentReportException 발생")
        void updateReportStatus_notFound() {
            // given
            UpdateReportStatusRequest request = new UpdateReportStatusRequest(ReportStatus.RESOLVED);
            given(commentReportRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentReportService.updateReportStatus(999L, request, operatorUser.getId()))
                    .isInstanceOf(CommentReportException.class);
        }
    }
}
