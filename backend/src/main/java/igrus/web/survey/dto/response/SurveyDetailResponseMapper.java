package igrus.web.survey.dto.response;

import igrus.web.generated.model.GetSurveyDetail200Response;
import igrus.web.generated.model.GetSurveyDetail200ResponseQuestionsInner;
import igrus.web.generated.model.GetSurveyDetail200ResponseQuestionsInnerOptionsInner;
import igrus.web.generated.model.GetSurveyDetail200ResponseQuestionsInnerRowsInner;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SurveyDetailResponse → OpenAPI 생성 모델 매핑 유틸리티.
 * 여러 컨트롤러에서 공통으로 사용하는 매핑 로직을 제공합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SurveyDetailResponseMapper {

    public static GetSurveyDetail200Response toApiResponse(SurveyDetailResponse response) {
        return new GetSurveyDetail200Response()
                .id(response.id())
                .title(response.title())
                .description(response.description())
                .visibility(response.visibility() != null
                        ? GetSurveyDetail200Response.VisibilityEnum.fromValue(response.visibility().name()) : null)
                .responseStatus(response.responseStatus() != null
                        ? GetSurveyDetail200Response.ResponseStatusEnum.fromValue(response.responseStatus().name()) : null)
                .accessLevel(response.accessLevel() != null
                        ? GetSurveyDetail200Response.AccessLevelEnum.fromValue(response.accessLevel().name()) : null)
                .deadline(response.deadline())
                .createdAt(response.createdAt())
                .updatedAt(response.updatedAt())
                .questions(response.questions() != null
                        ? response.questions().stream()
                                .map(SurveyDetailResponseMapper::toQuestionInner)
                                .toList()
                        : List.of());
    }

    public static GetSurveyDetail200ResponseQuestionsInner toQuestionInner(SurveyDetailResponse.QuestionResponse q) {
        return new GetSurveyDetail200ResponseQuestionsInner()
                .id(q.id())
                .questionType(q.questionType() != null
                        ? GetSurveyDetail200ResponseQuestionsInner.QuestionTypeEnum.fromValue(q.questionType().name()) : null)
                .title(q.title())
                .description(q.description())
                .required(q.required())
                .displayOrder(q.displayOrder())
                .scaleMin(q.scaleMin())
                .scaleMax(q.scaleMax())
                .options(q.options() != null
                        ? q.options().stream()
                                .map(SurveyDetailResponseMapper::toOptionInner)
                                .toList()
                        : List.of())
                .rows(q.rows() != null
                        ? q.rows().stream()
                                .map(SurveyDetailResponseMapper::toRowInner)
                                .toList()
                        : List.of());
    }

    private static GetSurveyDetail200ResponseQuestionsInnerOptionsInner toOptionInner(SurveyDetailResponse.OptionResponse o) {
        return new GetSurveyDetail200ResponseQuestionsInnerOptionsInner()
                .id(o.id())
                .text(o.text())
                .displayOrder(o.displayOrder());
    }

    private static GetSurveyDetail200ResponseQuestionsInnerRowsInner toRowInner(SurveyDetailResponse.RowResponse r) {
        return new GetSurveyDetail200ResponseQuestionsInnerRowsInner()
                .id(r.id())
                .label(r.label())
                .displayOrder(r.displayOrder());
    }
}
