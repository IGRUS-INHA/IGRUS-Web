package igrus.web.survey.response.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitAnswerRequest(
        @NotNull Long questionId,
        String textValue,
        List<Long> selectedOptionIds,
        Integer numericValue,
        @Valid List<GridAnswerRequest> gridAnswers
) {

    public record GridAnswerRequest(
            @NotNull Long rowId,
            @NotEmpty List<Long> selectedOptionIds
    ) {
    }
}
