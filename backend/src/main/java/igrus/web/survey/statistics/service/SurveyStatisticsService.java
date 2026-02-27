package igrus.web.survey.statistics.service;

import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyAccessLevel;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.question.domain.*;
import igrus.web.survey.question.repository.SurveyQuestionRepository;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.survey.response.domain.*;
import igrus.web.survey.response.repository.SurveyAnswerRepository;
import igrus.web.survey.response.repository.SurveyResponseRepository;
import igrus.web.survey.statistics.dto.response.*;
import igrus.web.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 설문 통계 서비스.
 * 설문 응답 데이터를 집계하여 질문 타입별 통계를 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyStatisticsService {

    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final SurveyAnswerRepository surveyAnswerRepository;
    private final SurveyQuestionRepository surveyQuestionRepository;

    /**
     * 설문 통계를 조회합니다.
     *
     * @param surveyId   설문 ID
     * @param operatorId 조회 요청 운영자 ID
     * @return 설문 통계 응답
     * @throws SurveyNotFoundException 설문이 존재하지 않거나 영구 삭제된 경우
     */
    public SurveyStatisticsResponse getSurveyStatistics(Long surveyId, Long operatorId) {
        log.info("설문 통계 조회 요청: surveyId={}, operatorId={}", surveyId, operatorId);

        // 1. 설문 조회 (deleted=true이면 SurveyNotFoundException)
        Survey survey = surveyRepository.findByIdAndDeletedFalse(surveyId)
                .orElseThrow(() -> {
                    log.warn("통계 조회 실패: 설문 없음 surveyId={}", surveyId);
                    return new SurveyNotFoundException(surveyId);
                });

        // 2. 응답자 정보 포함 여부 판단 (PUBLIC 설문은 응답자 정보 생략)
        boolean includeRespondentInfo = survey.getAccessLevel() != SurveyAccessLevel.PUBLIC;

        // 3. 유효 응답(deleted=false) 목록 조회 -> totalResponseCount 계산
        //    응답자 정보가 필요한 경우 user를 함께 fetch join하여 N+1 방지
        List<SurveyResponse> validResponses = includeRespondentInfo
                ? surveyResponseRepository.findValidResponsesWithUserBySurveyId(surveyId)
                : surveyResponseRepository.findBySurveyIdAndDeletedFalseOrderByCreatedAtAsc(surveyId);
        int totalResponseCount = validResponses.size();

        // 4. 응답 기간 계산 (min/max createdAt, 0건이면 null)
        Instant responseStartedAt = null;
        Instant responseEndedAt = null;
        if (!validResponses.isEmpty()) {
            responseStartedAt = validResponses.getFirst().getCreatedAt();
            responseEndedAt = validResponses.getLast().getCreatedAt();
        }

        // 5. 응답자 정보 목록 생성 (비PUBLIC 설문만)
        List<RespondentInfo> respondents = null;
        if (includeRespondentInfo) {
            respondents = validResponses.stream()
                    .map(SurveyResponse::getUser)
                    .filter(Objects::nonNull)
                    .map(RespondentInfo::from)
                    .toList();
        }

        // 6. 설문의 삭제되지 않은 질문 조회 (displayOrder 오름차순)
        //    MultipleBagFetchException 방지를 위해 options, rows를 분리 조회합니다.
        //    같은 트랜잭션(영속성 컨텍스트) 내에서 두 쿼리를 순차 호출하면
        //    첫 번째 쿼리 결과의 엔티티에 두 번째 쿼리의 rows 컬렉션이 자동 병합됩니다.
        List<SurveyQuestion> allQuestions = surveyQuestionRepository
                .findAllBySurveyIdWithOptions(surveyId);
        surveyQuestionRepository.findAllBySurveyIdWithRows(surveyId);

        // 7. 유효 답변 조회 (SurveyAnswer.deleted=false AND SurveyResponse.deleted=false)
        List<SurveyAnswer> validAnswers = surveyAnswerRepository.findValidAnswersBySurveyId(surveyId);

        // 질문별로 답변 그룹핑
        Map<Long, List<SurveyAnswer>> answersByQuestionId = validAnswers.stream()
                .collect(Collectors.groupingBy(a -> a.getQuestion().getId()));

        // 8. 질문별 통계 계산
        List<QuestionStatisticsResponse> questionStatistics = allQuestions.stream()
                .map(question -> buildQuestionStatistics(
                        question, answersByQuestionId, totalResponseCount, includeRespondentInfo))
                .toList();

        log.info("설문 통계 조회 완료: surveyId={}, 총 응답 수={}", surveyId, totalResponseCount);

        return new SurveyStatisticsResponse(
                totalResponseCount,
                responseStartedAt,
                responseEndedAt,
                respondents,
                questionStatistics
        );
    }

    // ==================== Private helpers ====================

    /**
     * 질문별 통계를 생성합니다.
     */
    private QuestionStatisticsResponse buildQuestionStatistics(
            SurveyQuestion question,
            Map<Long, List<SurveyAnswer>> answersByQuestionId,
            int totalResponseCount,
            boolean includeRespondentInfo) {

        List<SurveyAnswer> answers = answersByQuestionId.getOrDefault(question.getId(), List.of());
        int responseCount = answers.size();

        String category = question.getQuestionType().getCategory();

        TextQuestionStatistics textStatistics = null;
        ScaleQuestionStatistics scaleStatistics = null;
        OptionQuestionStatistics optionStatistics = null;
        GridQuestionStatistics gridStatistics = null;

        switch (category) {
            case "TEXT" -> textStatistics = buildTextStatistics(answers, includeRespondentInfo);
            case "SCALE" -> scaleStatistics = buildScaleStatistics(question, answers);
            case "OPTION" -> {
                // OPTION 카테고리에는 MC, CHECKBOX, DROPDOWN이 포함
                // CHECKBOX는 응답자당 여러 OptionSurveyAnswer가 생성되므로 responseCount 보정 필요
                if (question.getQuestionType() == SurveyQuestionType.CHECKBOX) {
                    responseCount = countDistinctResponses(answers);
                }
                optionStatistics = buildOptionStatistics(question, answers, totalResponseCount);
            }
            case "GRID" -> {
                // GRID는 행x옵션 조합당 1개의 GridSurveyAnswer이므로 responseCount 보정 필요
                responseCount = countDistinctResponses(answers);
                gridStatistics = buildGridStatistics(question, answers, totalResponseCount);
            }
        }

        return new QuestionStatisticsResponse(
                question.getId(),
                question.getTitle(),
                question.getQuestionType(),
                responseCount,
                textStatistics,
                scaleStatistics,
                optionStatistics,
                gridStatistics
        );
    }

    /**
     * 답변 목록에서 고유 응답(SurveyResponse) 수를 계산합니다.
     * CHECKBOX, GRID 유형에서 응답자당 여러 답변이 있을 수 있으므로 중복을 제거합니다.
     */
    private int countDistinctResponses(List<SurveyAnswer> answers) {
        return (int) answers.stream()
                .map(a -> a.getResponse().getId())
                .distinct()
                .count();
    }

    /**
     * TEXT 카테고리 통계를 생성합니다.
     * 텍스트 응답 항목 목록을 SurveyResponse.createdAt 오름차순으로 반환합니다.
     *
     * @param answers               답변 목록
     * @param includeRespondentInfo 응답자 정보 포함 여부 (비PUBLIC 설문이면 true)
     */
    private TextQuestionStatistics buildTextStatistics(List<SurveyAnswer> answers,
                                                       boolean includeRespondentInfo) {
        List<TextResponseItem> textResponses = answers.stream()
                .sorted(Comparator.comparing(a -> a.getResponse().getCreatedAt()))
                .map(a -> {
                    String text = ((TextSurveyAnswer) a).getTextValue();
                    RespondentInfo respondent = null;
                    if (includeRespondentInfo) {
                        User user = a.getResponse().getUser();
                        if (user != null) {
                            respondent = RespondentInfo.from(user);
                        }
                    }
                    return new TextResponseItem(text, respondent);
                })
                .toList();

        return new TextQuestionStatistics(textResponses);
    }

    /**
     * SCALE 카테고리 통계를 생성합니다.
     * 평균(HALF_UP, scale=1), 최솟값, 최댓값, 값별 분포를 계산합니다.
     */
    private ScaleQuestionStatistics buildScaleStatistics(SurveyQuestion question, List<SurveyAnswer> answers) {
        LinearScaleSurveyQuestion scaleQuestion = (LinearScaleSurveyQuestion) question;
        int scaleMin = scaleQuestion.getScaleMin();
        int scaleMax = scaleQuestion.getScaleMax();

        // 분포 초기화 (scaleMin~scaleMax 전체 키 포함, 미선택은 0)
        Map<Integer, Integer> distribution = new LinkedHashMap<>();
        for (int i = scaleMin; i <= scaleMax; i++) {
            distribution.put(i, 0);
        }

        if (answers.isEmpty()) {
            return new ScaleQuestionStatistics(
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    null,
                    null,
                    distribution
            );
        }

        List<Integer> values = answers.stream()
                .map(a -> ((NumericSurveyAnswer) a).getNumericValue())
                .toList();

        // 분포 계산
        for (Integer value : values) {
            distribution.merge(value, 1, Integer::sum);
        }

        // 평균 계산 (HALF_UP, scale=1)
        BigDecimal sum = values.stream()
                .map(BigDecimal::new)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);

        int min = values.stream().mapToInt(Integer::intValue).min().orElse(0);
        int max = values.stream().mapToInt(Integer::intValue).max().orElse(0);

        return new ScaleQuestionStatistics(average, min, max, distribution);
    }

    /**
     * OPTION/CHECKBOX 카테고리 통계를 생성합니다.
     * 옵션별 선택 수와 비율을 계산합니다.
     */
    private OptionQuestionStatistics buildOptionStatistics(
            SurveyQuestion question,
            List<SurveyAnswer> answers,
            int totalResponseCount) {

        // 질문의 삭제되지 않은 옵션만 조회
        List<SurveyQuestionOption> activeOptions = ((OptionSurveyQuestion) question).getOptions().stream()
                .filter(option -> !option.isDeleted())
                .toList();

        // 옵션별 선택 수 집계
        Map<Long, Integer> optionCounts = new HashMap<>();
        for (SurveyQuestionOption option : activeOptions) {
            optionCounts.put(option.getId(), 0);
        }
        for (SurveyAnswer answer : answers) {
            OptionSurveyAnswer optionAnswer = (OptionSurveyAnswer) answer;
            Long optionId = optionAnswer.getSelectedOption().getId();
            optionCounts.merge(optionId, 1, Integer::sum);
        }

        // 옵션별 통계 생성
        List<OptionStatisticsItem> optionItems = activeOptions.stream()
                .map(option -> new OptionStatisticsItem(
                        option.getId(),
                        option.getText(),
                        optionCounts.getOrDefault(option.getId(), 0),
                        calculatePercentage(optionCounts.getOrDefault(option.getId(), 0), totalResponseCount)
                ))
                .toList();

        return new OptionQuestionStatistics(optionItems);
    }

    /**
     * GRID 카테고리 통계를 생성합니다.
     * 행별 옵션 분포를 계산합니다. 비율 분모는 전체 설문 응답자 수(totalResponseCount)입니다.
     */
    private GridQuestionStatistics buildGridStatistics(
            SurveyQuestion question,
            List<SurveyAnswer> answers,
            int totalResponseCount) {

        GridSurveyQuestion gridQuestion = (GridSurveyQuestion) question;
        List<SurveyQuestionOption> activeOptions = gridQuestion.getOptions().stream()
                .filter(option -> !option.isDeleted())
                .toList();
        List<SurveyQuestionRow> activeRows = gridQuestion.getRows().stream()
                .filter(row -> !row.isDeleted())
                .toList();

        // 행별 옵션별 선택 수 집계: Map<rowId, Map<optionId, count>>
        Map<Long, Map<Long, Integer>> rowOptionCounts = new HashMap<>();
        for (SurveyQuestionRow row : activeRows) {
            Map<Long, Integer> optionCounts = new LinkedHashMap<>();
            for (SurveyQuestionOption option : activeOptions) {
                optionCounts.put(option.getId(), 0);
            }
            rowOptionCounts.put(row.getId(), optionCounts);
        }

        for (SurveyAnswer answer : answers) {
            GridSurveyAnswer gridAnswer = (GridSurveyAnswer) answer;
            Long rowId = gridAnswer.getSelectedRow().getId();
            Long optionId = gridAnswer.getSelectedOption().getId();
            rowOptionCounts.computeIfAbsent(rowId, k -> new LinkedHashMap<>())
                    .merge(optionId, 1, Integer::sum);
        }

        // 행별 통계 생성
        List<GridRowStatistics> rowStatistics = activeRows.stream()
                .map(row -> {
                    Map<Long, Integer> optionCounts = rowOptionCounts.getOrDefault(row.getId(), Map.of());
                    List<OptionStatisticsItem> optionItems = activeOptions.stream()
                            .map(option -> new OptionStatisticsItem(
                                    option.getId(),
                                    option.getText(),
                                    optionCounts.getOrDefault(option.getId(), 0),
                                    calculatePercentage(optionCounts.getOrDefault(option.getId(), 0), totalResponseCount)
                            ))
                            .toList();
                    return new GridRowStatistics(row.getId(), row.getLabel(), optionItems);
                })
                .toList();

        return new GridQuestionStatistics(rowStatistics);
    }

    /**
     * 비율을 계산합니다. 분모가 0이면 0.0을 반환합니다.
     * RoundingMode.HALF_UP, scale=1
     *
     * @param count 분자 (선택 수)
     * @param total 분모 (전체 응답자 수)
     * @return 비율 (소수 첫째 자리 반올림)
     */
    private BigDecimal calculatePercentage(int count, int total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
}
