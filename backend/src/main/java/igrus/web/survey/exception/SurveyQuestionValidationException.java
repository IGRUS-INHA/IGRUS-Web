package igrus.web.survey.exception;

import igrus.web.common.exception.CustomBaseException;

/** 질문 유효성 검증 실패 시 발생합니다. (예: 선형 배율 범위 오류, 알 수 없는 질문 카테고리) */
public class SurveyQuestionValidationException extends CustomBaseException {

    public SurveyQuestionValidationException() {
        super(SurveyErrorCode.SURVEY_QUESTION_VALIDATION_FAILED);
    }

    public SurveyQuestionValidationException(String message) {
        super(SurveyErrorCode.SURVEY_QUESTION_VALIDATION_FAILED, message);
    }
}
