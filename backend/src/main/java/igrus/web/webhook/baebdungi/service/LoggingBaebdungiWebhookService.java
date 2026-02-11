package igrus.web.webhook.baebdungi.service;

import igrus.web.webhook.baebdungi.dto.BaebdungiSubmissionRequest;
import igrus.web.user.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 로컬/테스트 환경용 뱁둥이봇 웹훅 서비스 구현체.
 * 실제 HTTP 호출 없이 로그만 출력합니다.
 */
@Slf4j
@Service
@Profile("!prod")
public class LoggingBaebdungiWebhookService implements BaebdungiWebhookService {

    @Override
    public void sendSubmission(User user) {
        BaebdungiSubmissionRequest request = BaebdungiSubmissionRequest.fromUser(user);
        log.info("===== [로컬] 뱁둥이봇 웹훅 호출 =====");
        log.info("학번: {}", request.studentId());
        log.info("이름: {}", request.name());
        log.info("이메일: {}", request.email());
        log.info("학과: {}", request.department());
        log.info("성별: {}", request.gender());
        log.info("학년: {}", request.grade());
        log.info("====================================");
    }
}
