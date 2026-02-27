package igrus.web.survey.response.domain;

import igrus.web.survey.question.domain.SurveyQuestion;
import igrus.web.survey.question.domain.SurveyQuestionOption;
import igrus.web.survey.question.domain.SurveyQuestionRow;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 그리드 유형 답변 엔티티.
 * MULTIPLE_CHOICE_GRID, CHECKBOX_GRID 유형에서 사용합니다.
 * 행x선택지 조합당 1개의 GridSurveyAnswer가 생성됩니다.
 */
@Entity
@DiscriminatorValue("GRID")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GridSurveyAnswer extends SurveyAnswer {

    public SurveyQuestionOption getSelectedOption() {
        return selectedOption;
    }

    public SurveyQuestionRow getSelectedRow() {
        return selectedRow;
    }

    private GridSurveyAnswer(SurveyResponse response, SurveyQuestion question,
                              SurveyQuestionRow selectedRow, SurveyQuestionOption selectedOption) {
        super(response, question);
        this.selectedRow = selectedRow;
        this.selectedOption = selectedOption;
    }

    public static GridSurveyAnswer create(SurveyResponse response, SurveyQuestion question,
                                           SurveyQuestionRow selectedRow, SurveyQuestionOption selectedOption) {
        return new GridSurveyAnswer(response, question, selectedRow, selectedOption);
    }
}
