package igrus.web.survey.domain;

import igrus.web.survey.exception.SurveyQuestionValidationException;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 선형 배율 유형 질문 엔티티.
 * LINEAR_SCALE 유형에서 사용합니다.
 */
@Entity
@DiscriminatorValue("SCALE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LinearScaleSurveyQuestion extends SurveyQuestion {

    public Integer getScaleMin() {
        return scaleMin;
    }

    public Integer getScaleMax() {
        return scaleMax;
    }

    /**
     * 선형 배율 범위를 설정합니다.
     *
     * @param min 최솟값
     * @param max 최댓값
     * @throws SurveyQuestionValidationException min이 max 이상인 경우
     */
    public void setScaleRange(int min, int max) {
        if (min >= max) {
            throw new SurveyQuestionValidationException("최솟값은 최댓값보다 작아야 합니다.");
        }
        this.scaleMin = min;
        this.scaleMax = max;
    }

    private LinearScaleSurveyQuestion(Survey survey, SurveyQuestionType questionType,
                                       String title, String description,
                                       boolean required, int displayOrder) {
        super(survey, questionType, title, description, required, displayOrder);
    }

    public static LinearScaleSurveyQuestion create(Survey survey, SurveyQuestionType questionType,
                                                     String title, String description,
                                                     boolean required, int displayOrder) {
        return new LinearScaleSurveyQuestion(survey, questionType, title, description, required, displayOrder);
    }
}
