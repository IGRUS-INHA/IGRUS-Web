package igrus.web.survey.response.dto.response;

import igrus.web.survey.question.domain.SurveyQuestion;
import igrus.web.survey.question.domain.SurveyQuestionOption;
import igrus.web.survey.question.domain.SurveyQuestionRow;
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

    /**
     * 설문 응답의 답변 목록을 질문별로 그룹핑하여 AnswerResponse 목록으로 변환합니다.
     * archived 질문의 답변도 포함되어 과거 응답이 그대로 표시됩니다.
     *
     * @param answers 설문 답변 목록
     * @return 질문별로 그룹핑된 AnswerResponse 목록
     */
    public static List<AnswerResponse> groupAnswersByQuestion(List<SurveyAnswer> answers) {
        Map<Long, List<SurveyAnswer>> answersByQuestion = new LinkedHashMap<>();

        for (SurveyAnswer answer : answers) {
            answersByQuestion
                    .computeIfAbsent(answer.getQuestion().getId(), k -> new ArrayList<>())
                    .add(answer);
        }

        return answersByQuestion.values().stream()
                .map(AnswerResponse::from)
                .toList();
    }

    public static SurveyResponseDetailResponse from(SurveyResponse response) {
        List<AnswerResponse> answerResponses = groupAnswersByQuestion(response.getAnswers());

        return new SurveyResponseDetailResponse(
                response.getId(),
                response.getSurvey().getId(),
                response.getUser() != null ? response.getUser().getId() : null,
                answerResponses,
                response.getCreatedAt()
        );
    }

    /**
     * 응답에 선택된 옵션 (id + 표시용 text).
     * archived 옵션도 응답 보존을 위해 텍스트가 채워집니다.
     */
    public record SelectedOptionResponse(Long id, String text) {
        public static SelectedOptionResponse from(SurveyQuestionOption option) {
            return new SelectedOptionResponse(option.getId(), option.getText());
        }
    }

    /**
     * 응답에 선택된 그리드 행 (id + 표시용 label).
     * archived 행도 응답 보존을 위해 라벨이 채워집니다.
     */
    public record SelectedRowResponse(Long id, String label) {
        public static SelectedRowResponse from(SurveyQuestionRow row) {
            return new SelectedRowResponse(row.getId(), row.getLabel());
        }
    }

    public record AnswerResponse(
            Long questionId,
            SurveyQuestionType questionType,
            String textValue,
            List<SelectedOptionResponse> selectedOptions,
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
                                .map(a -> SelectedOptionResponse.from(((OptionSurveyAnswer) a).getSelectedOption()))
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
                    // 행별로 그룹핑 (행 ID 기준, 행 엔티티는 첫 답변에서 추출)
                    Map<Long, SurveyQuestionRow> rowsById = new LinkedHashMap<>();
                    Map<Long, List<SelectedOptionResponse>> optionsByRow = new LinkedHashMap<>();
                    for (SurveyAnswer a : answers) {
                        GridSurveyAnswer ga = (GridSurveyAnswer) a;
                        SurveyQuestionRow row = ga.getSelectedRow();
                        rowsById.putIfAbsent(row.getId(), row);
                        optionsByRow
                                .computeIfAbsent(row.getId(), k -> new ArrayList<>())
                                .add(SelectedOptionResponse.from(ga.getSelectedOption()));
                    }
                    List<GridAnswerResponse> gridAnswers = optionsByRow.entrySet().stream()
                            .map(e -> new GridAnswerResponse(
                                    SelectedRowResponse.from(rowsById.get(e.getKey())),
                                    e.getValue()
                            ))
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
            SelectedRowResponse row,
            List<SelectedOptionResponse> selectedOptions
    ) {
    }
}
