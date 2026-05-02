package igrus.web.survey.response.service;

import igrus.web.survey.domain.Survey;
import igrus.web.survey.question.domain.*;
import igrus.web.survey.response.dto.request.SubmitAnswerRequest;
import igrus.web.survey.response.exception.SurveyResponseValidationException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 설문 응답 데이터 유효성 검증기.
 * 순수 검증 로직만 포함하며, Repository 의존 없음.
 */
@Component
public class SurveyAnswerValidator {

    /**
     * 제출된 답변 목록을 설문의 질문 구조와 대조하여 검증합니다.
     *
     * @param survey  대상 설문 (질문 포함)
     * @param answers 제출된 답변 목록
     * @throws SurveyResponseValidationException 검증 실패 시
     */
    public void validate(Survey survey, List<SubmitAnswerRequest> answers) {
        // 활성 질문만 추출 (archived 제외)
        List<SurveyQuestion> activeQuestions = survey.getQuestions().stream()
                .filter(q -> !q.isArchived())
                .toList();

        Map<Long, SurveyQuestion> questionMap = activeQuestions.stream()
                .collect(Collectors.toMap(SurveyQuestion::getId, q -> q));

        // 중복 questionId 검증
        Set<Long> answeredQuestionIds = new HashSet<>();
        for (SubmitAnswerRequest answer : answers) {
            if (!answeredQuestionIds.add(answer.questionId())) {
                throw new SurveyResponseValidationException(
                        "동일한 질문에 대한 중복 응답입니다: questionId=" + answer.questionId());
            }
        }

        // 필수 질문 응답 누락 검사
        for (SurveyQuestion question : activeQuestions) {
            if (question.isRequired() && !answeredQuestionIds.contains(question.getId())) {
                throw new SurveyResponseValidationException(
                        "필수 질문에 대한 응답이 누락되었습니다: questionId=" + question.getId());
            }
        }

        // 각 답변 검증
        for (SubmitAnswerRequest answer : answers) {
            SurveyQuestion question = questionMap.get(answer.questionId());
            if (question == null) {
                throw new SurveyResponseValidationException(
                        "존재하지 않는 질문에 대한 응답입니다: questionId=" + answer.questionId());
            }
            validateAnswer(question, answer);
        }
    }

    private void validateAnswer(SurveyQuestion question, SubmitAnswerRequest answer) {
        String category = question.getQuestionType().getCategory();
        switch (category) {
            case "TEXT" -> validateTextAnswer(question, answer);
            case "OPTION" -> validateOptionAnswer(question, answer);
            case "SCALE" -> validateScaleAnswer(question, answer);
            case "GRID" -> validateGridAnswer(question, answer);
            default -> throw new SurveyResponseValidationException(
                    "알 수 없는 질문 카테고리: " + category);
        }
    }

    private void validateTextAnswer(SurveyQuestion question, SubmitAnswerRequest answer) {
        if (question.isRequired() && isBlank(answer.textValue())) {
            throw new SurveyResponseValidationException(
                    "필수 텍스트 질문의 응답이 비어있습니다: questionId=" + question.getId());
        }
    }

    private void validateOptionAnswer(SurveyQuestion question, SubmitAnswerRequest answer) {
        OptionSurveyQuestion osq = (OptionSurveyQuestion) question;
        List<Long> selectedIds = answer.selectedOptionIds();

        // 유효한(활성) 옵션 ID 집합
        Set<Long> validOptionIds = osq.getOptions().stream()
                .filter(o -> !o.isArchived())
                .map(SurveyQuestionOption::getId)
                .collect(Collectors.toSet());

        SurveyQuestionType type = question.getQuestionType();

        if (type == SurveyQuestionType.MULTIPLE_CHOICE || type == SurveyQuestionType.DROPDOWN) {
            // 비필수이고 응답이 없으면 통과
            if (selectedIds == null || selectedIds.isEmpty()) {
                if (question.isRequired()) {
                    throw new SurveyResponseValidationException(
                            "필수 객관식/드롭다운 질문은 1개의 선택지를 선택해야 합니다: questionId=" + question.getId());
                }
                return;
            }
            // 정확히 1개 선택
            if (selectedIds.size() != 1) {
                throw new SurveyResponseValidationException(
                        "객관식/드롭다운 질문은 정확히 1개의 선택지를 선택해야 합니다: questionId=" + question.getId());
            }
            if (!validOptionIds.contains(selectedIds.getFirst())) {
                throw new SurveyResponseValidationException(
                        "유효하지 않은 선택지입니다: optionId=" + selectedIds.getFirst());
            }
        } else if (type == SurveyQuestionType.CHECKBOX) {
            // 비필수이고 응답이 없으면 통과
            if (selectedIds == null || selectedIds.isEmpty()) {
                if (question.isRequired()) {
                    throw new SurveyResponseValidationException(
                            "필수 체크박스 질문은 1개 이상의 선택지를 선택해야 합니다: questionId=" + question.getId());
                }
                return;
            }
            // 중복 optionId 검증
            if (selectedIds.size() != Set.copyOf(selectedIds).size()) {
                throw new SurveyResponseValidationException(
                        "중복된 선택지가 포함되어 있습니다: questionId=" + question.getId());
            }
            for (Long optionId : selectedIds) {
                if (!validOptionIds.contains(optionId)) {
                    throw new SurveyResponseValidationException(
                            "유효하지 않은 선택지입니다: optionId=" + optionId);
                }
            }
        }
    }

    private void validateScaleAnswer(SurveyQuestion question, SubmitAnswerRequest answer) {
        LinearScaleSurveyQuestion lsq = (LinearScaleSurveyQuestion) question;

        if (answer.numericValue() == null) {
            if (question.isRequired()) {
                throw new SurveyResponseValidationException(
                        "필수 선형 배율 질문의 응답이 비어있습니다: questionId=" + question.getId());
            }
            return;
        }

        int value = answer.numericValue();
        if (value < lsq.getScaleMin() || value > lsq.getScaleMax()) {
            throw new SurveyResponseValidationException(
                    "선형 배율 값이 범위를 벗어났습니다: questionId=" + question.getId()
                            + ", value=" + value + ", range=[" + lsq.getScaleMin() + "," + lsq.getScaleMax() + "]");
        }
    }

    private void validateGridAnswer(SurveyQuestion question, SubmitAnswerRequest answer) {
        GridSurveyQuestion gsq = (GridSurveyQuestion) question;

        // 유효한(활성) 옵션/행 ID 집합
        Set<Long> validOptionIds = gsq.getOptions().stream()
                .filter(o -> !o.isArchived())
                .map(SurveyQuestionOption::getId)
                .collect(Collectors.toSet());

        Set<Long> validRowIds = gsq.getRows().stream()
                .filter(r -> !r.isArchived())
                .map(SurveyQuestionRow::getId)
                .collect(Collectors.toSet());

        List<SubmitAnswerRequest.GridAnswerRequest> gridAnswers = answer.gridAnswers();

        if (gridAnswers == null || gridAnswers.isEmpty()) {
            if (question.isRequired()) {
                throw new SurveyResponseValidationException(
                        "필수 그리드 질문의 응답이 비어있습니다: questionId=" + question.getId());
            }
            return;
        }

        // 중복 rowId 검증
        Set<Long> answeredRowIds = new HashSet<>();
        for (SubmitAnswerRequest.GridAnswerRequest ga : gridAnswers) {
            if (!answeredRowIds.add(ga.rowId())) {
                throw new SurveyResponseValidationException(
                        "동일한 행에 대한 중복 응답입니다: questionId=" + question.getId() + ", rowId=" + ga.rowId());
            }
        }

        // 필수일 때 모든 유효 행에 대한 응답 필요
        if (question.isRequired()) {
            for (Long rowId : validRowIds) {
                if (!answeredRowIds.contains(rowId)) {
                    throw new SurveyResponseValidationException(
                            "필수 그리드 질문의 행 응답이 누락되었습니다: questionId=" + question.getId() + ", rowId=" + rowId);
                }
            }
        }

        SurveyQuestionType type = question.getQuestionType();

        for (SubmitAnswerRequest.GridAnswerRequest ga : gridAnswers) {
            // 행 유효성
            if (!validRowIds.contains(ga.rowId())) {
                throw new SurveyResponseValidationException(
                        "유효하지 않은 그리드 행입니다: rowId=" + ga.rowId());
            }

            // 옵션 유효성
            if (ga.selectedOptionIds() == null || ga.selectedOptionIds().isEmpty()) {
                throw new SurveyResponseValidationException(
                        "그리드 행에 선택된 옵션이 없습니다: rowId=" + ga.rowId());
            }

            // 행 내 중복 optionId 검증
            if (ga.selectedOptionIds().size() != Set.copyOf(ga.selectedOptionIds()).size()) {
                throw new SurveyResponseValidationException(
                        "그리드 행에 중복된 선택지가 포함되어 있습니다: rowId=" + ga.rowId());
            }

            for (Long optionId : ga.selectedOptionIds()) {
                if (!validOptionIds.contains(optionId)) {
                    throw new SurveyResponseValidationException(
                            "유효하지 않은 그리드 선택지입니다: optionId=" + optionId);
                }
            }

            // MC_GRID는 행당 정확히 1개
            if (type == SurveyQuestionType.MULTIPLE_CHOICE_GRID && ga.selectedOptionIds().size() != 1) {
                throw new SurveyResponseValidationException(
                        "객관식 그리드는 행당 정확히 1개의 선택지를 선택해야 합니다: rowId=" + ga.rowId());
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
