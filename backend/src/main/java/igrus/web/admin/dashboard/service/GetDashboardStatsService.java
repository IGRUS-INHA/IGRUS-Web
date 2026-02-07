package igrus.web.admin.dashboard.service;

import igrus.web.admin.dashboard.dto.DashboardStatsResponse;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.repository.InquiryRepository;
import igrus.web.user.domain.UserRole;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.repository.UserRoleHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/**
 * 관리자 대시보드 통계 조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetDashboardStatsService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final UserRoleHistoryRepository userRoleHistoryRepository;
    private final InquiryRepository inquiryRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        Instant todayStart = calculateTodayStart();
        Instant weekStart = calculateThisWeekMondayStart();

        long todayPostCount = postRepository.countByDeletedFalseAndCreatedAtAfter(todayStart);
        long todayCommentCount = commentRepository.countByDeletedFalseAndCreatedAtAfter(todayStart);
        long weeklyApprovedMemberCount = userRoleHistoryRepository.countByNewRoleAndCreatedAtAfter(UserRole.MEMBER, weekStart);
        long pendingInquiryCount = inquiryRepository.countByStatus(InquiryStatus.PENDING);
        long pendingAssociateCount = userRepository.countByRole(UserRole.ASSOCIATE);

        return new DashboardStatsResponse(
                todayPostCount,
                todayCommentCount,
                weeklyApprovedMemberCount,
                pendingInquiryCount,
                pendingAssociateCount
        );
    }

    private Instant calculateTodayStart() {
        return LocalDate.now(KST)
                .atStartOfDay(KST)
                .toInstant();
    }

    private Instant calculateThisWeekMondayStart() {
        return LocalDate.now(KST)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(KST)
                .toInstant();
    }
}
