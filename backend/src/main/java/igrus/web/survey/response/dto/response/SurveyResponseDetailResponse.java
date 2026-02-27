package igrus.web.survey.response.dto.response;

import igrus.web.survey.question.domain.SurveyQuestion;
import igrus.web.survey.question.domain.SurveyQuestionType;
import igrus.web.survey.response.domain.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SurveyResponseDetailResponse(
        Long responseId,
        Long surveyId,
        Long userId,
        List<AnswerResponse> answers,
        Instant createdAt
) {

    public static SurveyResponseDetailResponse from(SurveyResponse response) {
        // 질문별로 답변을 그룹핑 (순서 보존)
        Map<Long, List<SurveyAnswer>> answersByQuestion = new LinkedHashMap<>();

        for (SurveyAnswer answer : response.getAnswers()) {
            // 삭제된 질문의 답변은 제외
            if (answer.getQuestion().isDeleted()) {
                continue;
            }
            answersByQuestion
                    .computeIfAbsent(answer.getQuestion().getId(), k -> new ArrayList<>())
                    .add(answer);
        }

        List<AnswerResponse> answerResponses = answersByQuestion.values().stream()
                .map(AnswerResponse::from)
                .toList();

        return new SurveyResponseDetailResponse(
                response.getId(),
                response.getSurvey().getId(),
                response.getUser() != null ? response.getUser().getId() : null,
                answerResponses,
                response.getCreatedAt()
        );
    }

    public record AnswerResponse(
            Long questionId,
            SurveyQuestionType questionType,
            String textValue,
            List<Long> selectedOptionIds,
            Integer numericValue,
            List<GridAnswerResponse> gridAnswers
    ) {

        static AnswerResponse from(List<SurveyAnswer> answers) {
            SurveyAnswer first = answers.getFirst();
            SurveyQuestion question = first.getQuestion();
            SurveyQuestionType type = question.getQuestionType();
            String category = type.getCategory();

            return switch (category) {
                case "TEXT" -> new AnswerResponse(
                        question.getId(), type,
                        ((TextSurveyAnswer) first).getTextValue(),
                        null, null, null
                );
                case "OPTION" -> new AnswerResponse(
                        question.getId(), type,
                        null,
                        answers.stream()
                                .map(a -> ((OptionSurveyAnswer) a).getSelectedOption().getId())
                                .toList(),
                        null, null
                );
                case "SCALE" -> new AnswerResponse(
                        question.getId(), type,
                        null, null,
                        ((NumericSurveyAnswer) first).getNumericValue(),
                        null
                );
                case "GRID" -> {
                    // 행별로 그룹핑
                    Map<Long, List<Long>> optionsByRow = new LinkedHashMap<>();
                    for (SurveyAnswer a : answers) {
                        GridSurveyAnswer ga = (GridSurveyAnswer) a;
                        optionsByRow
                                .computeIfAbsent(ga.getSelectedRow().getId(), k -> new ArrayList<>())
                                .add(ga.getSelectedOption().getId());
                    }
                    List<GridAnswerResponse> gridAnswers = optionsByRow.entrySet().stream()
                            .map(e -> new GridAnswerResponse(e.getKey(), e.getValue()))
                            .toList();
                    yield new AnswerResponse(
                            question.getId(), type,
                            null, null, null, gridAnswers
                    );
                }
                default -> throw new IllegalStateException("Unknown question category: " + category);
            };
        }
    }

    public record GridAnswerResponse(
            Long rowId,
            List<Long> selectedOptionIds
    ) {
    }
}
