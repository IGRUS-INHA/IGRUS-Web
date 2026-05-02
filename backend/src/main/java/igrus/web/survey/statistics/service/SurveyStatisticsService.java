package igrus.web.survey.statistics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.event.domain.ExternalSurveyResponse;
import igrus.web.event.repository.ExternalSurveyResponseRepository;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyAccessLevel;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.exception.SurveyStatisticsAggregationException;
import igrus.web.survey.question.domain.*;
import igrus.web.survey.question.repository.SurveyQuestionRepository;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.survey.response.domain.*;
import igrus.web.survey.response.dto.request.SubmitAnswerRequest;
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
    private final ExternalSurveyResponseRepository externalSurveyResponseRepository;
    private final ObjectMapper objectMapper;

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
        //    NOTE: 응답 기간은 회원 응답(SurveyResponse)의 createdAt만 기준으로 계산합니다.
        //    외부인 응답(ExternalSurveyResponse)의 createdAt은 포함하지 않습니다.
        //    외부인 응답은 행사 신청 시 함께 저장되므로 설문 응답 기간과 성격이 다릅니다.
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

        // 8. 외부인 응답 통합 (ExternalSurveyResponse JSON 파싱)
        List<ExternalSurveyResponse> externalResponses = externalSurveyResponseRepository.findBySurveyId(surveyId);
        int externalResponseCount = 0;
        Map<Long, List<ExternalAnswerData>> externalAnswersByQuestionId = new HashMap<>();

        for (ExternalSurveyResponse externalResponse : externalResponses) {
            try {
                List<SubmitAnswerRequest> parsedAnswers = objectMapper.readValue(
                        externalResponse.getAnswers(),
                        new TypeReference<>() {}
                );
                externalResponseCount++;
                for (SubmitAnswerRequest answer : parsedAnswers) {
                    externalAnswersByQuestionId
                            .computeIfAbsent(answer.questionId(), k -> new ArrayList<>())
                            .add(ExternalAnswerData.from(answer));
                }
            } catch (JsonProcessingException e) {
                log.warn("외부인 응답 JSON 파싱 실패, 건너뜀: externalResponseId={}, surveyId={}, error={}",
                        externalResponse.getId(), surveyId, e.getMessage());
            }
        }

        int combinedTotalResponseCount = totalResponseCount + externalResponseCount;

        // 9. 질문별 통계 계산 (회원 + 외부인 합산)
        List<QuestionStatisticsResponse> questionStatistics = allQuestions.stream()
                .map(question -> buildQuestionStatistics(
                        question, answersByQuestionId, externalAnswersByQuestionId,
                        combinedTotalResponseCount, includeRespondentInfo))
                .toList();

        log.info("설문 통계 조회 완료: surveyId={}, 회원 응답 수={}, 외부인 응답 수={}, 총 응답 수={}",
                surveyId, totalResponseCount, externalResponseCount, combinedTotalResponseCount);

        return new SurveyStatisticsResponse(
                combinedTotalResponseCount,
                responseStartedAt,
                responseEndedAt,
                respondents,
                questionStatistics
        );
    }

    // ==================== Private helpers ====================

    /**
     * 질문별 통계를 생성합니다. 회원 응답(SurveyAnswer)과 외부인 응답(ExternalAnswerData)을 합산합니다.
     */
    private QuestionStatisticsResponse buildQuestionStatistics(
            SurveyQuestion question,
            Map<Long, List<SurveyAnswer>> answersByQuestionId,
            Map<Long, List<ExternalAnswerData>> externalAnswersByQuestionId,
            int totalResponseCount,
            boolean includeRespondentInfo) {

        List<SurveyAnswer> answers = answersByQuestionId.getOrDefault(question.getId(), List.of());
        List<ExternalAnswerData> externalAnswers = externalAnswersByQuestionId.getOrDefault(question.getId(), List.of());
        int responseCount = answers.size();

        String category = question.getQuestionType().getCategory();

        TextQuestionStatistics textStatistics = null;
        ScaleQuestionStatistics scaleStatistics = null;
        OptionQuestionStatistics optionStatistics = null;
        GridQuestionStatistics gridStatistics = null;

        switch (category) {
            case "TEXT" -> {
                responseCount += externalAnswers.size();
                textStatistics = buildTextStatistics(answers, externalAnswers, includeRespondentInfo);
            }
            case "SCALE" -> {
                responseCount += externalAnswers.size();
                scaleStatistics = buildScaleStatistics(question, answers, externalAnswers);
            }
            case "OPTION" -> {
                // OPTION 카테고리에는 MC, CHECKBOX, DROPDOWN이 포함
                // CHECKBOX는 응답자당 여러 OptionSurveyAnswer가 생성되므로 responseCount 보정 필요
                if (question.getQuestionType() == SurveyQuestionType.CHECKBOX) {
                    responseCount = countDistinctResponses(answers);
                }
                // 외부인 OPTION: 한 응답당 selectedOptionIds 목록을 제출하므로 1명으로 카운트
                responseCount += externalAnswers.size();
                optionStatistics = buildOptionStatistics(question, answers, externalAnswers, totalResponseCount);
            }
            case "GRID" -> {
                // GRID는 행x옵션 조합당 1개의 GridSurveyAnswer이므로 responseCount 보정 필요
                responseCount = countDistinctResponses(answers);
                responseCount += externalAnswers.size();
                gridStatistics = buildGridStatistics(question, answers, externalAnswers, totalResponseCount);
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
     * 회원 텍스트 응답은 SurveyResponse.createdAt 오름차순으로, 외부인 응답은 그 뒤에 추가됩니다.
     *
     * @param answers               회원 답변 목록
     * @param externalAnswers       외부인 답변 목록
     * @param includeRespondentInfo 응답자 정보 포함 여부 (비PUBLIC 설문이면 true)
     */
    private TextQuestionStatistics buildTextStatistics(List<SurveyAnswer> answers,
                                                       List<ExternalAnswerData> externalAnswers,
                                                       boolean includeRespondentInfo) {
        List<TextResponseItem> textResponses = new ArrayList<>(answers.stream()
                .sorted(Comparator.comparing(a -> a.getResponse().getCreatedAt()))
                .map(a -> {
                    if (!(a instanceof TextSurveyAnswer textAnswer)) {
                        throw new SurveyStatisticsAggregationException(
                                "TEXT 질문에 잘못된 답변 유형: answerId=" + a.getId()
                                        + ", actualType=" + a.getClass().getSimpleName());
                    }
                    String text = textAnswer.getTextValue();
                    RespondentInfo respondent = null;
                    if (includeRespondentInfo) {
                        User user = a.getResponse().getUser();
                        if (user != null) {
                            respondent = RespondentInfo.from(user);
                        }
                    }
                    return new TextResponseItem(text, respondent);
                })
                .toList());

        // 외부인 텍스트 응답 추가 (응답자 정보 없음)
        for (ExternalAnswerData ext : externalAnswers) {
            if (ext.textValue() != null) {
                textResponses.add(new TextResponseItem(ext.textValue(), null));
            }
        }

        return new TextQuestionStatistics(textResponses);
    }

    /**
     * SCALE 카테고리 통계를 생성합니다.
     * 평균(HALF_UP, scale=1), 최솟값, 최댓값, 값별 분포를 계산합니다.
     * 회원 응답과 외부인 응답을 합산합니다.
     */
    private ScaleQuestionStatistics buildScaleStatistics(SurveyQuestion question,
                                                          List<SurveyAnswer> answers,
                                                          List<ExternalAnswerData> externalAnswers) {
        if (!(question instanceof LinearScaleSurveyQuestion scaleQuestion)) {
            throw new SurveyStatisticsAggregationException(
                    "SCALE 질문에 잘못된 질문 유형: questionId=" + question.getId()
                            + ", actualType=" + question.getClass().getSimpleName());
        }
        int scaleMin = scaleQuestion.getScaleMin();
        int scaleMax = scaleQuestion.getScaleMax();

        // 분포 초기화 (scaleMin~scaleMax 전체 키 포함, 미선택은 0)
        Map<Integer, Integer> distribution = new LinkedHashMap<>();
        for (int i = scaleMin; i <= scaleMax; i++) {
            distribution.put(i, 0);
        }

        // 회원 응답 값 추출
        List<Integer> values = new ArrayList<>(answers.stream()
                .map(a -> {
                    if (!(a instanceof NumericSurveyAnswer numericAnswer)) {
                        throw new SurveyStatisticsAggregationException(
                                "SCALE 질문에 잘못된 답변 유형: answerId=" + a.getId()
                                        + ", actualType=" + a.getClass().getSimpleName());
                    }
                    return numericAnswer.getNumericValue();
                })
                .toList());

        // 외부인 응답 값 추가
        for (ExternalAnswerData ext : externalAnswers) {
            if (ext.numericValue() != null) {
                values.add(ext.numericValue());
            }
        }

        if (values.isEmpty()) {
            return new ScaleQuestionStatistics(
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    0,
                    0,
                    distribution
            );
        }

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
     * 옵션별 선택 수와 비율을 계산합니다. 회원 응답과 외부인 응답을 합산합니다.
     */
    private OptionQuestionStatistics buildOptionStatistics(
            SurveyQuestion question,
            List<SurveyAnswer> answers,
            List<ExternalAnswerData> externalAnswers,
            int totalResponseCount) {

        // 질문의 archived되지 않은 옵션만 조회
        if (!(question instanceof OptionSurveyQuestion optionQuestion)) {
            throw new SurveyStatisticsAggregationException(
                    "OPTION 질문에 잘못된 질문 유형: questionId=" + question.getId()
                            + ", actualType=" + question.getClass().getSimpleName());
        }
        List<SurveyQuestionOption> activeOptions = optionQuestion.getOptions().stream()
                .filter(option -> !option.isArchived())
                .toList();

        // 옵션별 선택 수 집계
        Map<Long, Integer> optionCounts = new HashMap<>();
        for (SurveyQuestionOption option : activeOptions) {
            optionCounts.put(option.getId(), 0);
        }
        for (SurveyAnswer answer : answers) {
            if (!(answer instanceof OptionSurveyAnswer optionAnswer)) {
                throw new SurveyStatisticsAggregationException(
                        "OPTION 질문에 잘못된 답변 유형: answerId=" + answer.getId()
                                + ", actualType=" + answer.getClass().getSimpleName());
            }
            Long optionId = optionAnswer.getSelectedOption().getId();
            optionCounts.merge(optionId, 1, Integer::sum);
        }

        // 외부인 응답의 옵션 선택 합산
        for (ExternalAnswerData ext : externalAnswers) {
            if (ext.selectedOptionIds() != null) {
                for (Long optionId : ext.selectedOptionIds()) {
                    optionCounts.merge(optionId, 1, Integer::sum);
                }
            }
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
     * 회원 응답과 외부인 응답을 합산합니다.
     */
    private GridQuestionStatistics buildGridStatistics(
            SurveyQuestion question,
            List<SurveyAnswer> answers,
            List<ExternalAnswerData> externalAnswers,
            int totalResponseCount) {

        if (!(question instanceof GridSurveyQuestion gridQuestion)) {
            throw new SurveyStatisticsAggregationException(
                    "GRID 질문에 잘못된 질문 유형: questionId=" + question.getId()
                            + ", actualType=" + question.getClass().getSimpleName());
        }
        List<SurveyQuestionOption> activeOptions = gridQuestion.getOptions().stream()
                .filter(option -> !option.isArchived())
                .toList();
        List<SurveyQuestionRow> activeRows = gridQuestion.getRows().stream()
                .filter(row -> !row.isArchived())
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
            if (!(answer instanceof GridSurveyAnswer gridAnswer)) {
                throw new SurveyStatisticsAggregationException(
                        "GRID 질문에 잘못된 답변 유형: answerId=" + answer.getId()
                                + ", actualType=" + answer.getClass().getSimpleName());
            }
            Long rowId = gridAnswer.getSelectedRow().getId();
            Long optionId = gridAnswer.getSelectedOption().getId();
            rowOptionCounts.computeIfAbsent(rowId, k -> new LinkedHashMap<>())
                    .merge(optionId, 1, Integer::sum);
        }

        // 외부인 GRID 응답 합산
        for (ExternalAnswerData ext : externalAnswers) {
            if (ext.gridAnswers() != null) {
                for (ExternalGridAnswerData gridAnswer : ext.gridAnswers()) {
                    Long rowId = gridAnswer.rowId();
                    if (gridAnswer.selectedOptionIds() != null) {
                        for (Long optionId : gridAnswer.selectedOptionIds()) {
                            rowOptionCounts.computeIfAbsent(rowId, k -> new LinkedHashMap<>())
                                    .merge(optionId, 1, Integer::sum);
                        }
                    }
                }
            }
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

    // ==================== 외부인 응답 데이터 구조 ====================

    /**
     * 외부인 설문 응답 JSON에서 파싱된 개별 답변 데이터.
     * {@link SubmitAnswerRequest}의 JSON 구조와 1:1 대응합니다.
     *
     * @param questionId        질문 ID
     * @param textValue         텍스트 응답 값 (TEXT 카테고리)
     * @param selectedOptionIds 선택된 옵션 ID 목록 (OPTION 카테고리)
     * @param numericValue      숫자 값 (SCALE 카테고리)
     * @param gridAnswers       그리드 답변 목록 (GRID 카테고리)
     */
    private record ExternalAnswerData(
            Long questionId,
            String textValue,
            List<Long> selectedOptionIds,
            Integer numericValue,
            List<ExternalGridAnswerData> gridAnswers
    ) {
        static ExternalAnswerData from(SubmitAnswerRequest request) {
            List<ExternalGridAnswerData> gridAnswerDataList = null;
            if (request.gridAnswers() != null) {
                gridAnswerDataList = request.gridAnswers().stream()
                        .map(g -> new ExternalGridAnswerData(g.rowId(), g.selectedOptionIds()))
                        .toList();
            }
            return new ExternalAnswerData(
                    request.questionId(),
                    request.textValue(),
                    request.selectedOptionIds(),
                    request.numericValue(),
                    gridAnswerDataList
            );
        }
    }

    /**
     * 외부인 그리드 답변 데이터.
     *
     * @param rowId             행 ID
     * @param selectedOptionIds 선택된 옵션 ID 목록
     */
    private record ExternalGridAnswerData(
            Long rowId,
            List<Long> selectedOptionIds
    ) {
    }
}
