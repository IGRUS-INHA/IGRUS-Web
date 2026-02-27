package igrus.web.survey.response.domain;

import igrus.web.survey.question.domain.SurveyQuestion;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 텍스트 유형 답변 엔티티.
 * SHORT_ANSWER, PARAGRAPH, DATE, TIME, FILE_UPLOAD 유형에서 사용합니다.
 */
@Entity
@DiscriminatorValue("TEXT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TextSurveyAnswer extends SurveyAnswer {

    public String getTextValue() {
        return textValue;
    }

    private TextSurveyAnswer(SurveyResponse response, SurveyQuestion question, String textValue) {
        super(response, question);
        this.textValue = textValue;
    }

    public static TextSurveyAnswer create(SurveyResponse response, SurveyQuestion question, String textValue) {
        return new TextSurveyAnswer(response, question, textValue);
    }
}
