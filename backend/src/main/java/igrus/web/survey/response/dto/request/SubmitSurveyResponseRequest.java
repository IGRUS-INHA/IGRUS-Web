package igrus.web.survey.response.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitSurveyResponseRequest(
        @Valid @NotNull List<SubmitAnswerRequest> answers
) {
}
