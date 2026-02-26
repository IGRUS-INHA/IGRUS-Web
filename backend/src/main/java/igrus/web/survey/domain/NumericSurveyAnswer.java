package igrus.web.survey.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 숫자 유형 답변 엔티티.
 * LINEAR_SCALE 유형에서 사용합니다.
 */
@Entity
@DiscriminatorValue("NUMERIC")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NumericSurveyAnswer extends SurveyAnswer {

    public Integer getNumericValue() {
        return numericValue;
    }

    private NumericSurveyAnswer(SurveyResponse response, SurveyQuestion question, int numericValue) {
        super(response, question);
        this.numericValue = numericValue;
    }

    public static NumericSurveyAnswer create(SurveyResponse response, SurveyQuestion question, int numericValue) {
        return new NumericSurveyAnswer(response, question, numericValue);
    }
}
