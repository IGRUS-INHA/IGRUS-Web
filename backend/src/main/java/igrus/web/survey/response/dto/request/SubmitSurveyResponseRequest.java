package igrus.web.survey.response.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitSurveyResponseRequest(
        @Valid @NotEmpty List<SubmitAnswerRequest> answers
) {
}
