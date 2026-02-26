package igrus.web.survey.service;

import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyResponseStatus;
import igrus.web.survey.repository.SurveyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 마감일 경과 설문 자동 마감 서비스.
 * 마감일이 경과한 OPEN 상태의 설문을 자동으로 CLOSED 처리합니다.
 *
 * <p>스케줄러({@link igrus.web.survey.scheduler.SurveyDeadlineCloseScheduler})에서 호출됩니다.</p>
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class SurveyDeadlineCloseService {

    private final SurveyRepository surveyRepository;

    /**
     * 마감일이 경과한 OPEN 상태의 활성 설문을 조회하여 자동 마감합니다.
     *
     * @return 마감 처리된 설문 수
     */
    public int closeExpiredSurveys() {
        List<Survey> expired = surveyRepository
                .findByResponseStatusAndDeletedFalseAndTrashedAtIsNullAndDeadlineBefore(
                        SurveyResponseStatus.OPEN, Instant.now());
        expired.forEach(Survey::closeResponse);
        return expired.size();
    }
}
