package igrus.web.survey.response.service;

import igrus.web.survey.domain.Survey;
import igrus.web.survey.question.domain.*;
import igrus.web.survey.response.domain.*;
import igrus.web.survey.response.dto.request.SubmitAnswerRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 설문 답변 엔티티 생성을 담당하는 컴포넌트.
 * SurveyResponseService에서 추출된 답변 생성 로직을 독립 컴포넌트로 분리하여,
 * EventRegistrationService 등 다른 서비스에서도 재사용할 수 있도록 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SurveyAnswerFactory {

    /**
     * 제출된 답변 목록으로부터 질문 유형별 SurveyAnswer 엔티티를 생성합니다.
     */
    public void createAnswers(SurveyResponse response, Survey survey, List<SubmitAnswerRequest> answers) {
        Map<Long, SurveyQuestion> questionMap = survey.getQuestions().stream()
                .filter(q -> !q.isArchived())
                .collect(Collectors.toMap(SurveyQuestion::getId, q -> q));

        for (SubmitAnswerRequest answerReq : answers) {
            SurveyQuestion question = questionMap.get(answerReq.questionId());
            if (question == null) {
                log.warn("설문 답변 생성 시 질문을 찾을 수 없음 - questionId: {}, surveyId: {}",
                        answerReq.questionId(), survey.getId());
                continue;
            }

            String category = question.getQuestionType().getCategory();
            switch (category) {
                case "TEXT" -> {
                    TextSurveyAnswer textAnswer = TextSurveyAnswer.create(response, question, answerReq.textValue());
                    response.addAnswer(textAnswer);
                }
                case "OPTION" -> createOptionAnswers(response, question, answerReq);
                case "SCALE" -> {
                    if (answerReq.numericValue() != null) {
                        NumericSurveyAnswer numericAnswer = NumericSurveyAnswer.create(
                                response, question, answerReq.numericValue());
                        response.addAnswer(numericAnswer);
                    }
                }
                case "GRID" -> createGridAnswers(response, question, answerReq);
                default -> log.warn("인식되지 않은 질문 카테고리 - category: {}, questionId: {}, surveyId: {}",
                        category, question.getId(), survey.getId());
            }
        }
    }

    /**
     * 선택형(MULTIPLE_CHOICE, DROPDOWN, CHECKBOX) 질문의 답변을 생성합니다.
     */
    private void createOptionAnswers(SurveyResponse response, SurveyQuestion question, SubmitAnswerRequest answerReq) {
        if (answerReq.selectedOptionIds() == null || answerReq.selectedOptionIds().isEmpty()) {
            return;
        }

        OptionSurveyQuestion osq = (OptionSurveyQuestion) question;
        Map<Long, SurveyQuestionOption> optionMap = osq.getOptions().stream()
                .filter(o -> !o.isArchived())
                .collect(Collectors.toMap(SurveyQuestionOption::getId, o -> o));

        for (Long optionId : answerReq.selectedOptionIds()) {
            SurveyQuestionOption option = optionMap.get(optionId);
            if (option != null) {
                OptionSurveyAnswer optionAnswer = OptionSurveyAnswer.create(response, question, option);
                response.addAnswer(optionAnswer);
            }
        }
    }

    /**
     * 그리드형(MULTIPLE_CHOICE_GRID, CHECKBOX_GRID) 질문의 답변을 생성합니다.
     */
    private void createGridAnswers(SurveyResponse response, SurveyQuestion question, SubmitAnswerRequest answerReq) {
        if (answerReq.gridAnswers() == null || answerReq.gridAnswers().isEmpty()) {
            return;
        }

        GridSurveyQuestion gsq = (GridSurveyQuestion) question;
        Map<Long, SurveyQuestionOption> optionMap = gsq.getOptions().stream()
                .filter(o -> !o.isArchived())
                .collect(Collectors.toMap(SurveyQuestionOption::getId, o -> o));
        Map<Long, SurveyQuestionRow> rowMap = gsq.getRows().stream()
                .filter(r -> !r.isArchived())
                .collect(Collectors.toMap(SurveyQuestionRow::getId, r -> r));

        for (SubmitAnswerRequest.GridAnswerRequest ga : answerReq.gridAnswers()) {
            SurveyQuestionRow row = rowMap.get(ga.rowId());
            if (row == null) {
                continue;
            }
            for (Long optionId : ga.selectedOptionIds()) {
                SurveyQuestionOption option = optionMap.get(optionId);
                if (option != null) {
                    GridSurveyAnswer gridAnswer = GridSurveyAnswer.create(response, question, row, option);
                    response.addAnswer(gridAnswer);
                }
            }
        }
    }
}
