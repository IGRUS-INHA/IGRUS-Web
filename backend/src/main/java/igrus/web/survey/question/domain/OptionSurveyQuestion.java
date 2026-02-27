package igrus.web.survey.question.domain;

import igrus.web.survey.domain.Survey;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 선택지 유형 질문 엔티티.
 * MULTIPLE_CHOICE, CHECKBOX, DROPDOWN 유형에서 사용합니다.
 */
@Entity
@DiscriminatorValue("OPTION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OptionSurveyQuestion extends SurveyQuestion {

    public List<SurveyQuestionOption> getOptions() {
        return options;
    }

    /**
     * 선택지를 추가합니다.
     *
     * @param option 추가할 선택지
     */
    public void addOption(SurveyQuestionOption option) {
        this.options.add(option);
    }

    private OptionSurveyQuestion(Survey survey, SurveyQuestionType questionType,
                                  String title, String description,
                                  boolean required, int displayOrder) {
        super(survey, questionType, title, description, required, displayOrder);
    }

    public static OptionSurveyQuestion create(Survey survey, SurveyQuestionType questionType,
                                               String title, String description,
                                               boolean required, int displayOrder) {
        return new OptionSurveyQuestion(survey, questionType, title, description, required, displayOrder);
    }
}
