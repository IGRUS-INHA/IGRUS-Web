package igrus.web.community.comment.service.support;

import igrus.web.community.board.domain.Board;
import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.domain.CommentReport;
import igrus.web.community.comment.domain.ReportStatus;
import igrus.web.community.comment.dto.response.CommentReportResponse;
import igrus.web.community.comment.repository.CommentReportRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static igrus.web.common.fixture.TestEntityIdAssigner.withId;
import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.CommentTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * GetPendingReportsService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>CMT-052: 관리자 신고 검토 대기열 확인</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetPendingReportsService 단위 테스트")
class GetPendingReportsServiceTest {

    @Mock
    private CommentReportRepository commentReportRepository;

    @InjectMocks
    private GetPendingReportsService getPendingReportsService;

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
    @DisplayName("CMT-052: 대기 중인 신고 목록 조회 성공")
    void getPendingReports_success() {
        // given
        CommentReport report1 = withId(createCommentReport(targetComment, anotherMember), 1L);
        CommentReport report2 = withId(createCommentReport(targetComment, anotherMember), 2L);
        given(commentReportRepository.findByStatus(ReportStatus.PENDING))
                .willReturn(List.of(report1, report2));

        // when
        List<CommentReportResponse> responses = getPendingReportsService.getPendingReports();

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses).allMatch(r -> r.getStatus() == ReportStatus.PENDING);
    }
}
