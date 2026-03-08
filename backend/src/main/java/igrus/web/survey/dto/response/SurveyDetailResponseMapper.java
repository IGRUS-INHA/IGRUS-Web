package igrus.web.survey.dto.response;

import igrus.web.generated.model.ApiOptionResponse;
import igrus.web.generated.model.ApiQuestionResponse;
import igrus.web.generated.model.ApiRowResponse;
import igrus.web.generated.model.ApiSurveyDetailResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SurveyDetailResponse → OpenAPI 생성 모델 매핑 유틸리티.
 * 여러 컨트롤러에서 공통으로 사용하는 매핑 로직을 제공합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SurveyDetailResponseMapper {

    public static ApiSurveyDetailResponse toApiResponse(SurveyDetailResponse response) {
        return new ApiSurveyDetailResponse()
                .id(response.id())
                .title(response.title())
                .description(response.description())
                .visibility(response.visibility() != null
                        ? ApiSurveyDetailResponse.VisibilityEnum.fromValue(response.visibility().name()) : null)
                .responseStatus(response.responseStatus() != null
                        ? ApiSurveyDetailResponse.ResponseStatusEnum.fromValue(response.responseStatus().name()) : null)
                .accessLevel(response.accessLevel() != null
                        ? ApiSurveyDetailResponse.AccessLevelEnum.fromValue(response.accessLevel().name()) : null)
                .deadline(response.deadline())
                .createdAt(response.createdAt())
                .updatedAt(response.updatedAt())
                .questions(response.questions() != null
                        ? response.questions().stream()
                                .map(SurveyDetailResponseMapper::toQuestionInner)
                                .toList()
                        : List.of())
                .responseCount(response.responseCount());
    }

    public static ApiQuestionResponse toQuestionInner(SurveyDetailResponse.QuestionResponse q) {
        return new ApiQuestionResponse()
                .id(q.id())
                .questionType(q.questionType() != null
                        ? ApiQuestionResponse.QuestionTypeEnum.fromValue(q.questionType().name()) : null)
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

    private static ApiOptionResponse toOptionInner(SurveyDetailResponse.OptionResponse o) {
        return new ApiOptionResponse()
                .id(o.id())
                .text(o.text())
                .displayOrder(o.displayOrder());
    }

    private static ApiRowResponse toRowInner(SurveyDetailResponse.RowResponse r) {
        return new ApiRowResponse()
                .id(r.id())
                .label(r.label())
                .displayOrder(r.displayOrder());
    }
}
