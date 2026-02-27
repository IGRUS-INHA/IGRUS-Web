package igrus.web.survey.question.domain;

import igrus.web.survey.domain.Survey;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 텍스트 유형 질문 엔티티.
 * SHORT_ANSWER, PARAGRAPH, DATE, TIME, FILE_UPLOAD 유형에서 사용합니다.
 */
@Entity
@DiscriminatorValue("TEXT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TextSurveyQuestion extends SurveyQuestion {

    private TextSurveyQuestion(Survey survey, SurveyQuestionType questionType,
                                String title, String description,
                                boolean required, int displayOrder) {
        super(survey, questionType, title, description, required, displayOrder);
    }

    public static TextSurveyQuestion create(Survey survey, SurveyQuestionType questionType,
                                             String title, String description,
                                             boolean required, int displayOrder) {
        return new TextSurveyQuestion(survey, questionType, title, description, required, displayOrder);
    }
}
