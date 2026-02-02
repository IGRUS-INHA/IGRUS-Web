package igrus.web.community.comment.service.support;

import igrus.web.community.board.domain.Board;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.domain.CommentReport;
import igrus.web.community.comment.domain.ReportStatus;
import igrus.web.community.comment.dto.request.CreateCommentReportRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * ReportCommentService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>CMT-050: 댓글 신고 접수</li>
 *     <li>CMT-051: 동일 댓글 중복 신고 방지</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReportCommentService 단위 테스트")
class ReportCommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentReportRepository commentReportRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReportCommentService reportCommentService;

    private Board generalBoard;
    private User memberUser;
    private User anotherMember;
    private Post post;
    private Comment targetComment;

    @BeforeEach
    void setUp() {
        generalBoard = generalBoard();
        memberUser = createMemberWithId();
        anotherMember = createAnotherMemberWithId();
        post = normalPost(generalBoard, memberUser);
        targetComment = comment(post, memberUser);
    }

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
        CommentReportResponse response = reportCommentService.reportComment(
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
        assertThatThrownBy(() -> reportCommentService.reportComment(
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
        assertThatThrownBy(() -> reportCommentService.reportComment(999L, request, anotherMember.getId()))
                .isInstanceOf(CommentNotFoundException.class);
    }
}
