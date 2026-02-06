package igrus.web.admin.service;

import igrus.web.admin.domain.VisitLog;
import igrus.web.admin.repository.VisitLogRepository;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VisitLogService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final VisitLogRepository visitLogRepository;
    private final UserRepository userRepository;

    /**
     * 회원 방문 기록을 저장합니다.
     * UserRepository.getReferenceById()로 프록시 객체를 사용하여 DB 조회 없이 FK만 설정합니다.
     *
     * @param userId 방문한 사용자 ID
     */
    public void recordVisit(Long userId) {
        User userRef = userRepository.getReferenceById(userId);
        VisitLog visitLog = VisitLog.ofMember(userRef);
        visitLogRepository.save(visitLog);
        log.info("방문 기록 저장: userId={}", userId);
    }

    /**
     * 오늘(KST 기준) 방문자 수를 조회합니다.
     *
     * @return 오늘 방문자 수
     */
    @Transactional(readOnly = true)
    public long getTodayVisitorCount() {
        Instant todayStart = LocalDate.now(KST).atStartOfDay(KST).toInstant();
        Instant tomorrowStart = LocalDate.now(KST).plusDays(1).atStartOfDay(KST).toInstant();
        return visitLogRepository.countByVisitedAtBetween(todayStart, tomorrowStart);
    }
}
