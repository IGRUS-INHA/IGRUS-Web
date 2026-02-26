package igrus.web.survey.scheduler;

import igrus.web.survey.service.SurveyDeadlineCloseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 마감일 경과 설문 자동 마감 스케줄러.
 * 1분 간격으로 마감일이 경과한 OPEN 상태의 설문을 자동 마감합니다.
 *
 * <p>참고: {@code @Scheduled}와 {@code @Transactional}을 같은 메서드에 사용하면
 * 프록시 기반 AOP에서 정상 동작하지 않으므로 비즈니스 로직은
 * {@link SurveyDeadlineCloseService}에 위임합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SurveyDeadlineCloseScheduler {

    private final SurveyDeadlineCloseService surveyDeadlineCloseService;

    /**
     * 매분 정각에 마감일 경과 설문을 자동 마감합니다.
     */
    @Scheduled(cron = "0 * * * * *")
    public void closeExpiredSurveys() {
        int count = surveyDeadlineCloseService.closeExpiredSurveys();
        if (count > 0) {
            log.info("마감일 경과 설문 자동 마감 완료: {}건 처리", count);
        }
    }
}
