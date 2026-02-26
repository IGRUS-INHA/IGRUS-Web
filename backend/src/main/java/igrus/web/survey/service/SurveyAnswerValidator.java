package igrus.web.survey.service;

import igrus.web.survey.domain.*;
import igrus.web.survey.dto.request.SubmitAnswerRequest;
import igrus.web.survey.dto.request.SubmitAnswerRequest.GridAnswerRequest;
import igrus.web.survey.exception.SurveyResponseValidationException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 설문 답변 유효성 검증기.
 * 질문 유형별로 답변의 형식과 값을 검증합니다.
 */
@Component
public class SurveyAnswerValidator {

    /**
     * 전체 답변 목록을 검증합니다.
     * - 활성 질문에 대한 답변만 허용
     * - 필수 질문의 답변 누락 검증
     * - 질문 유형별 값 검증
     *
     * @param activeQuestions 활성 질문 목록
     * @param answers        제출된 답변 목록
     */
    public void validate(List<SurveyQuestion> activeQuestions, List<SubmitAnswerRequest> answers) {
        Map<Long, SurveyQuestion> questionMap = activeQuestions.stream()
                .collect(Collectors.toMap(SurveyQuestion::getId, Function.identity()));

        Set<Long> answeredQuestionIds = answers.stream()
                .map(SubmitAnswerRequest::questionId)
                .collect(Collectors.toSet());

        // 동일 질문에 대한 중복 답변 거부
        Set<Long> seenQuestionIds = new HashSet<>();
        for (SubmitAnswerRequest answer : answers) {
            if (!seenQuestionIds.add(answer.questionId())) {
                throw new SurveyResponseValidationException(
                        "동일 질문에 대해 중복 답변이 존재합니다: questionId=" + answer.questionId());
            }
        }

        // 존재하지 않거나 삭제된 질문에 대한 답변 거부
        for (SubmitAnswerRequest answer : answers) {
            if (!questionMap.containsKey(answer.questionId())) {
                throw new SurveyResponseValidationException(
                        "존재하지 않는 질문에 대한 답변입니다: questionId=" + answer.questionId());
            }
        }

        // 필수 질문 답변 누락 검증
        for (SurveyQuestion question : activeQuestions) {
            if (question.isRequired() && !answeredQuestionIds.contains(question.getId())) {
                throw new SurveyResponseValidationException(
                        String.format("필수 질문 '%s'에 대한 답변이 누락되었습니다.", question.getTitle()));
            }
        }

        // 질문 유형별 값 검증
        for (SubmitAnswerRequest answer : answers) {
            SurveyQuestion question = questionMap.get(answer.questionId());
            validateAnswer(question, answer);
        }
    }

    /**
     * 개별 답변을 질문 유형에 따라 검증합니다.
     */
    private void validateAnswer(SurveyQuestion question, SubmitAnswerRequest answer) {
        switch (question.getQuestionType()) {
            case SHORT_ANSWER -> validateTextAnswer(question, answer);
            case PARAGRAPH -> validateTextAnswer(question, answer);
            case MULTIPLE_CHOICE -> validateSingleOptionAnswer(question, answer);
            case DROPDOWN -> validateSingleOptionAnswer(question, answer);
            case CHECKBOX -> validateMultipleOptionAnswer(question, answer);
            case LINEAR_SCALE -> validateNumericAnswer(question, answer);
            case MULTIPLE_CHOICE_GRID -> validateGridAnswer(question, answer, true);
            case CHECKBOX_GRID -> validateGridAnswer(question, answer, false);
            case DATE -> validateDateAnswer(question, answer);
            case TIME -> validateTimeAnswer(question, answer);
            case FILE_UPLOAD -> validateTextAnswer(question, answer);
        }
    }

    private void validateTextAnswer(SurveyQuestion question, SubmitAnswerRequest answer) {
        if (question.isRequired() && isBlank(answer.textValue())) {
            throw new SurveyResponseValidationException(
                    String.format("필수 질문 '%s'의 텍스트 답변이 비어있습니다.", question.getTitle()));
        }
    }

    private void validateSingleOptionAnswer(SurveyQuestion question, SubmitAnswerRequest answer) {
        if (question.isRequired() && answer.selectedOptionId() == null) {
            throw new SurveyResponseValidationException(
                    String.format("필수 질문 '%s'의 선택지가 지정되지 않았습니다.", question.getTitle()));
        }
        if (answer.selectedOptionId() != null) {
            validateOptionBelongsToQuestion(question, answer.selectedOptionId());
        }
    }

    private void validateMultipleOptionAnswer(SurveyQuestion question, SubmitAnswerRequest answer) {
        if (question.isRequired() && (answer.selectedOptionIds() == null || answer.selectedOptionIds().isEmpty())) {
            throw new SurveyResponseValidationException(
                    String.format("필수 질문 '%s'에 최소 1개 이상의 선택지를 선택해야 합니다.", question.getTitle()));
        }
        if (answer.selectedOptionIds() != null) {
            // 중복 선택지 거부
            Set<Long> uniqueOptionIds = new HashSet<>(answer.selectedOptionIds());
            if (uniqueOptionIds.size() != answer.selectedOptionIds().size()) {
                throw new SurveyResponseValidationException(
                        String.format("질문 '%s'에 중복된 선택지가 존재합니다.", question.getTitle()));
            }
            for (Long optionId : answer.selectedOptionIds()) {
                validateOptionBelongsToQuestion(question, optionId);
            }
        }
    }

    private void validateNumericAnswer(SurveyQuestion question, SubmitAnswerRequest answer) {
        if (question.isRequired() && answer.numericValue() == null) {
            throw new SurveyResponseValidationException(
                    String.format("필수 질문 '%s'의 숫자 답변이 지정되지 않았습니다.", question.getTitle()));
        }
        if (answer.numericValue() != null && question instanceof LinearScaleSurveyQuestion scaleQ) {
            Integer min = scaleQ.getScaleMin();
            Integer max = scaleQ.getScaleMax();
            if (min != null && max != null) {
                if (answer.numericValue() < min || answer.numericValue() > max) {
                    throw new SurveyResponseValidationException(
                            String.format("질문 '%s'의 값은 %d~%d 범위여야 합니다.",
                                    question.getTitle(), min, max));
                }
            }
        }
    }

    private void validateGridAnswer(SurveyQuestion question, SubmitAnswerRequest answer, boolean singleSelect) {
        GridSurveyQuestion gridQ = (GridSurveyQuestion) question;
        List<SurveyQuestionRow> activeRows = gridQ.getRows().stream()
                .filter(r -> !r.isDeleted())
                .toList();

        if (question.isRequired()) {
            if (answer.gridAnswers() == null || answer.gridAnswers().isEmpty()) {
                throw new SurveyResponseValidationException(
                        String.format("필수 질문 '%s'의 그리드 답변이 누락되었습니다.", question.getTitle()));
            }
            // 모든 활성 행에 대한 답변이 있는지 검증
            Set<Long> answeredRowIds = answer.gridAnswers().stream()
                    .map(GridAnswerRequest::rowId)
                    .collect(Collectors.toSet());
            for (SurveyQuestionRow row : activeRows) {
                if (!answeredRowIds.contains(row.getId())) {
                    throw new SurveyResponseValidationException(
                            String.format("질문 '%s'의 행 '%s'에 대한 답변이 누락되었습니다.",
                                    question.getTitle(), row.getLabel()));
                }
            }
        }

        if (answer.gridAnswers() != null) {
            Set<Long> activeRowIds = activeRows.stream()
                    .map(SurveyQuestionRow::getId)
                    .collect(Collectors.toSet());

            // 중복 행 답변 거부
            Set<Long> seenRowIds = new HashSet<>();
            for (GridAnswerRequest gridAnswer : answer.gridAnswers()) {
                if (!seenRowIds.add(gridAnswer.rowId())) {
                    throw new SurveyResponseValidationException(
                            String.format("질문 '%s'에 동일 행에 대한 중복 답변이 존재합니다: rowId=%d",
                                    question.getTitle(), gridAnswer.rowId()));
                }
            }

            for (GridAnswerRequest gridAnswer : answer.gridAnswers()) {
                // 행이 질문에 속하는지 검증
                if (!activeRowIds.contains(gridAnswer.rowId())) {
                    throw new SurveyResponseValidationException(
                            String.format("질문 '%s'에 속하지 않는 행입니다: rowId=%d",
                                    question.getTitle(), gridAnswer.rowId()));
                }
                // 옵션 목록 검증
                if (gridAnswer.selectedOptionIds() == null || gridAnswer.selectedOptionIds().isEmpty()) {
                    throw new SurveyResponseValidationException(
                            String.format("질문 '%s'의 그리드 행에 최소 1개 이상의 선택지를 선택해야 합니다.",
                                    question.getTitle()));
                }
                // 단일 선택 그리드인 경우 정확히 1개만 허용
                if (singleSelect && gridAnswer.selectedOptionIds().size() > 1) {
                    throw new SurveyResponseValidationException(
                            String.format("질문 '%s'의 각 행에서는 1개의 선택지만 선택할 수 있습니다.",
                                    question.getTitle()));
                }
                // 중복 선택지 거부
                Set<Long> uniqueGridOptionIds = new HashSet<>(gridAnswer.selectedOptionIds());
                if (uniqueGridOptionIds.size() != gridAnswer.selectedOptionIds().size()) {
                    throw new SurveyResponseValidationException(
                            String.format("질문 '%s'의 행에 중복된 선택지가 존재합니다.", question.getTitle()));
                }
                // 옵션이 질문에 속하는지 검증
                for (Long optionId : gridAnswer.selectedOptionIds()) {
                    validateOptionBelongsToQuestion(question, optionId);
                }
            }
        }
    }

    private void validateDateAnswer(SurveyQuestion question, SubmitAnswerRequest answer) {
        if (question.isRequired() && isBlank(answer.textValue())) {
            throw new SurveyResponseValidationException(
                    String.format("필수 질문 '%s'의 날짜 답변이 비어있습니다.", question.getTitle()));
        }
        if (!isBlank(answer.textValue()) && !answer.textValue().matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new SurveyResponseValidationException(
                    String.format("질문 '%s'의 날짜 형식이 올바르지 않습니다. (yyyy-MM-dd)", question.getTitle()));
        }
    }

    private void validateTimeAnswer(SurveyQuestion question, SubmitAnswerRequest answer) {
        if (question.isRequired() && isBlank(answer.textValue())) {
            throw new SurveyResponseValidationException(
                    String.format("필수 질문 '%s'의 시간 답변이 비어있습니다.", question.getTitle()));
        }
        if (!isBlank(answer.textValue()) && !answer.textValue().matches("\\d{2}:\\d{2}")) {
            throw new SurveyResponseValidationException(
                    String.format("질문 '%s'의 시간 형식이 올바르지 않습니다. (HH:mm)", question.getTitle()));
        }
    }

    /**
     * 선택지가 해당 질문의 활성 옵션에 속하는지 검증합니다.
     */
    private void validateOptionBelongsToQuestion(SurveyQuestion question, Long optionId) {
        List<SurveyQuestionOption> questionOptions;
        if (question instanceof GridSurveyQuestion gridQ) {
            questionOptions = gridQ.getOptions();
        } else if (question instanceof OptionSurveyQuestion optionQ) {
            questionOptions = optionQ.getOptions();
        } else {
            questionOptions = List.of();
        }

        boolean belongs = questionOptions.stream()
                .filter(o -> !o.isDeleted())
                .map(SurveyQuestionOption::getId)
                .anyMatch(id -> id.equals(optionId));

        if (!belongs) {
            throw new SurveyResponseValidationException(
                    String.format("질문 '%s'에 속하지 않는 선택지입니다: optionId=%d",
                            question.getTitle(), optionId));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
