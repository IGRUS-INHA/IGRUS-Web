package igrus.web.survey.response.domain;

import igrus.web.survey.question.domain.SurveyQuestion;
import igrus.web.survey.question.domain.SurveyQuestionOption;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 선택지 유형 답변 엔티티.
 * MULTIPLE_CHOICE, CHECKBOX, DROPDOWN 유형에서 사용합니다.
 * CHECKBOX의 경우 선택지당 1개의 OptionSurveyAnswer가 생성됩니다.
 */
@Entity
@DiscriminatorValue("OPTION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OptionSurveyAnswer extends SurveyAnswer {

    public SurveyQuestionOption getSelectedOption() {
        return selectedOption;
    }

    private OptionSurveyAnswer(SurveyResponse response, SurveyQuestion question,
                                SurveyQuestionOption selectedOption) {
        super(response, question);
        this.selectedOption = selectedOption;
    }

    public static OptionSurveyAnswer create(SurveyResponse response, SurveyQuestion question,
                                             SurveyQuestionOption selectedOption) {
        return new OptionSurveyAnswer(response, question, selectedOption);
    }
}
