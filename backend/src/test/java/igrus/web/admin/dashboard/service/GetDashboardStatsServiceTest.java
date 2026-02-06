package igrus.web.admin.dashboard.service;

import igrus.web.admin.dashboard.dto.DashboardStatsResponse;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.repository.InquiryRepository;
import igrus.web.user.domain.UserRole;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.repository.UserRoleHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetDashboardStatsService 단위 테스트")
class GetDashboardStatsServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleHistoryRepository userRoleHistoryRepository;

    @Mock
    private InquiryRepository inquiryRepository;

    @InjectMocks
    private GetDashboardStatsService getDashboardStatsService;

    @Nested
    @DisplayName("통계 조회")
    class GetStatsTest {

        @Test
        @DisplayName("모든 통계가 정상 반환됨")
        void getDashboardStats_ReturnsAllStats() {
            // given
            given(postRepository.countByDeletedFalseAndCreatedAtAfter(any(Instant.class))).willReturn(5L);
            given(commentRepository.countByDeletedFalseAndCreatedAtAfter(any(Instant.class))).willReturn(10L);
            given(userRoleHistoryRepository.countByNewRoleAndCreatedAtAfter(eq(UserRole.MEMBER), any(Instant.class))).willReturn(3L);
            given(inquiryRepository.countByStatus(InquiryStatus.PENDING)).willReturn(2L);
            given(userRepository.countByRole(UserRole.ASSOCIATE)).willReturn(7L);

            // when
            DashboardStatsResponse response = getDashboardStatsService.getDashboardStats();

            // then
            assertThat(response.todayPostCount()).isEqualTo(5L);
            assertThat(response.todayCommentCount()).isEqualTo(10L);
            assertThat(response.newMemberCount()).isEqualTo(3L);
            assertThat(response.pendingInquiryCount()).isEqualTo(2L);
            assertThat(response.pendingAssociateCount()).isEqualTo(7L);
        }

        @Test
        @DisplayName("데이터가 없으면 모든 통계가 0으로 반환됨")
        void getDashboardStats_WithNoData_ReturnsZeros() {
            // given
            given(postRepository.countByDeletedFalseAndCreatedAtAfter(any(Instant.class))).willReturn(0L);
            given(commentRepository.countByDeletedFalseAndCreatedAtAfter(any(Instant.class))).willReturn(0L);
            given(userRoleHistoryRepository.countByNewRoleAndCreatedAtAfter(eq(UserRole.MEMBER), any(Instant.class))).willReturn(0L);
            given(inquiryRepository.countByStatus(InquiryStatus.PENDING)).willReturn(0L);
            given(userRepository.countByRole(UserRole.ASSOCIATE)).willReturn(0L);

            // when
            DashboardStatsResponse response = getDashboardStatsService.getDashboardStats();

            // then
            assertThat(response.todayPostCount()).isZero();
            assertThat(response.todayCommentCount()).isZero();
            assertThat(response.newMemberCount()).isZero();
            assertThat(response.pendingInquiryCount()).isZero();
            assertThat(response.pendingAssociateCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Repository 호출 검증")
    class RepositoryCallTest {

        @Test
        @DisplayName("문의는 PENDING 상태로, 준회원은 ASSOCIATE 역할로 조회함")
        void getDashboardStats_CallsWithCorrectEnums() {
            // given
            given(postRepository.countByDeletedFalseAndCreatedAtAfter(any())).willReturn(0L);
            given(commentRepository.countByDeletedFalseAndCreatedAtAfter(any())).willReturn(0L);
            given(userRoleHistoryRepository.countByNewRoleAndCreatedAtAfter(any(), any())).willReturn(0L);
            given(inquiryRepository.countByStatus(any())).willReturn(0L);
            given(userRepository.countByRole(any())).willReturn(0L);

            // when
            getDashboardStatsService.getDashboardStats();

            // then
            verify(inquiryRepository).countByStatus(InquiryStatus.PENDING);
            verify(userRepository).countByRole(UserRole.ASSOCIATE);
            verify(userRoleHistoryRepository).countByNewRoleAndCreatedAtAfter(eq(UserRole.MEMBER), any(Instant.class));
        }

        @Test
        @DisplayName("게시글/댓글에는 오늘 시작 시각을, 신규 회원에는 이번 주 월요일 시각을 전달함")
        void getDashboardStats_PassesCorrectTimeToRepositories() {
            // given
            given(postRepository.countByDeletedFalseAndCreatedAtAfter(any())).willReturn(0L);
            given(commentRepository.countByDeletedFalseAndCreatedAtAfter(any())).willReturn(0L);
            given(userRoleHistoryRepository.countByNewRoleAndCreatedAtAfter(any(), any())).willReturn(0L);
            given(inquiryRepository.countByStatus(any())).willReturn(0L);
            given(userRepository.countByRole(any())).willReturn(0L);

            Instant expectedTodayStart = LocalDate.now(KST).atStartOfDay(KST).toInstant();
            Instant expectedWeekStart = LocalDate.now(KST)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay(KST).toInstant();

            // when
            getDashboardStatsService.getDashboardStats();

            // then
            ArgumentCaptor<Instant> postTimeCaptor = ArgumentCaptor.forClass(Instant.class);
            ArgumentCaptor<Instant> commentTimeCaptor = ArgumentCaptor.forClass(Instant.class);
            ArgumentCaptor<Instant> memberTimeCaptor = ArgumentCaptor.forClass(Instant.class);

            verify(postRepository).countByDeletedFalseAndCreatedAtAfter(postTimeCaptor.capture());
            verify(commentRepository).countByDeletedFalseAndCreatedAtAfter(commentTimeCaptor.capture());
            verify(userRoleHistoryRepository).countByNewRoleAndCreatedAtAfter(any(), memberTimeCaptor.capture());

            assertThat(postTimeCaptor.getValue()).isEqualTo(expectedTodayStart);
            assertThat(commentTimeCaptor.getValue()).isEqualTo(expectedTodayStart);
            assertThat(memberTimeCaptor.getValue()).isEqualTo(expectedWeekStart);
        }
    }
}
