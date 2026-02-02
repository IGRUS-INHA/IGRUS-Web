package igrus.web.community.comment.service.support;

import igrus.web.community.board.domain.Board;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.domain.CommentReport;
import igrus.web.community.comment.domain.ReportStatus;
import igrus.web.community.comment.dto.request.UpdateReportStatusRequest;
import igrus.web.community.comment.exception.CommentReportException;
import igrus.web.community.comment.repository.CommentReportRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * UpdateReportStatusService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>CMT-053: 관리자 신고 처리</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateReportStatusService 단위 테스트")
class UpdateReportStatusServiceTest {

    @Mock
    private CommentReportRepository commentReportRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UpdateReportStatusService updateReportStatusService;

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

    @Test
    @DisplayName("CMT-053: 관리자가 신고 처리 (승인) 성공")
    void updateReportStatus_resolved_success() {
        // given
        CommentReport report = withId(createCommentReport(targetComment, anotherMember), 1L);
        UpdateReportStatusRequest request = new UpdateReportStatusRequest(ReportStatus.RESOLVED);
        given(commentReportRepository.findById(1L)).willReturn(Optional.of(report));
        given(userRepository.findById(operatorUser.getId())).willReturn(Optional.of(operatorUser));

        // when
        updateReportStatusService.updateReportStatus(1L, request, operatorUser.getId());

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
        updateReportStatusService.updateReportStatus(1L, request, operatorUser.getId());

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
        assertThatThrownBy(() -> updateReportStatusService.updateReportStatus(999L, request, operatorUser.getId()))
                .isInstanceOf(CommentReportException.class);
    }
}
