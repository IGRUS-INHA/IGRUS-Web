package igrus.web.survey.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 그리드 유형 질문 엔티티.
 * MULTIPLE_CHOICE_GRID, CHECKBOX_GRID 유형에서 사용합니다.
 */
@Entity
@DiscriminatorValue("GRID")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GridSurveyQuestion extends SurveyQuestion {

    public List<SurveyQuestionOption> getOptions() {
        return options;
    }

    public List<SurveyQuestionRow> getRows() {
        return rows;
    }

    /**
     * 선택지를 추가합니다.
     *
     * @param option 추가할 선택지
     */
    public void addOption(SurveyQuestionOption option) {
        this.options.add(option);
    }

    /**
     * 그리드 행을 추가합니다.
     *
     * @param row 추가할 행
     */
    public void addRow(SurveyQuestionRow row) {
        this.rows.add(row);
    }

    private GridSurveyQuestion(Survey survey, SurveyQuestionType questionType,
                                String title, String description,
                                boolean required, int displayOrder) {
        super(survey, questionType, title, description, required, displayOrder);
    }

    public static GridSurveyQuestion create(Survey survey, SurveyQuestionType questionType,
                                             String title, String description,
                                             boolean required, int displayOrder) {
        return new GridSurveyQuestion(survey, questionType, title, description, required, displayOrder);
    }
}
