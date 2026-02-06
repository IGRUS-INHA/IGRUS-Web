package igrus.web.admin.service;

import igrus.web.admin.dto.response.DashboardResponse;
import igrus.web.admin.repository.VisitLogRepository;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.repository.InquiryRepository;
import igrus.web.user.domain.UserRole;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final VisitLogRepository visitLogRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final InquiryRepository inquiryRepository;

    /**
     * 관리자 대시보드 통계를 조회합니다.
     *
     * @return 대시보드 통계 응답
     */
    public DashboardResponse getDashboard() {
        // 오늘 KST 자정 ~ 내일 KST 자정
        Instant todayStart = LocalDate.now(KST).atStartOfDay(KST).toInstant();
        Instant tomorrowStart = LocalDate.now(KST).plusDays(1).atStartOfDay(KST).toInstant();

        // 이번 주 월요일 KST 자정
        Instant weekStart = LocalDate.now(KST)
                .with(DayOfWeek.MONDAY)
                .atStartOfDay(KST)
                .toInstant();

        long todayVisitors = visitLogRepository.countByVisitedAtBetween(todayStart, tomorrowStart);
        long todayPosts = postRepository.countByCreatedAtBetween(todayStart, tomorrowStart);
        long weeklyNewUsers = userRepository.countByCreatedAtAfter(weekStart);
        long pendingInquiries = inquiryRepository.countByStatus(InquiryStatus.PENDING);
        long pendingAssociates = userRepository.countByRole(UserRole.ASSOCIATE);

        return new DashboardResponse(
                todayVisitors,
                todayPosts,
                weeklyNewUsers,
                pendingInquiries,
                pendingAssociates
        );
    }
}
