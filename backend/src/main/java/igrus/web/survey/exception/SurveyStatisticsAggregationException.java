package igrus.web.survey.exception;

import igrus.web.common.exception.CustomBaseException;

public class SurveyStatisticsAggregationException extends CustomBaseException {

    public SurveyStatisticsAggregationException(String message) {
        super(SurveyErrorCode.SURVEY_STATISTICS_AGGREGATION_FAILED, message);
    }
}
