package igrus.web.survey.dto.response;

import igrus.web.survey.domain.*;
import igrus.web.survey.question.domain.*;

import java.time.Instant;
import java.util.List;

/**
 * 설문 상세 응답 DTO.
 * 생성, 수정, 단건 조회 시 공통으로 사용합니다.
 * 질문·선택지·행을 포함한 전체 설문 구조를 담습니다.
 *
 * @param id             설문 ID
 * @param title          설문 제목
 * @param description    설문 설명
 * @param visibility     공개 상태 (UNPUBLISHED / PUBLISHED)
 * @param responseStatus 응답 수집 상태 (NOT_STARTED / OPEN / CLOSED)
 * @param accessLevel    응답 대상 권한
 * @param deadline       설문 마감일
 * @param createdAt      생성 시각
 * @param updatedAt      수정 시각
 * @param questions      질문 목록 (삭제되지 않은 질문만 포함)
 */
public record SurveyDetailResponse(
        Long id,
        String title,
        String description,
        SurveyVisibility visibility,
        SurveyResponseStatus responseStatus,
        SurveyAccessLevel accessLevel,
        Instant deadline,
        Instant createdAt,
        Instant updatedAt,
        List<QuestionResponse> questions
) {
    /**
     * Survey 엔티티로부터 SurveyDetailResponse를 생성합니다.
     *
     * @param survey 설문 엔티티
     * @return SurveyDetailResponse
     */
    public static SurveyDetailResponse from(Survey survey) {
        List<QuestionResponse> questions = survey.getQuestions().stream()
                .filter(q -> !q.isDeleted())
                .map(QuestionResponse::from)
                .toList();

        return new SurveyDetailResponse(
                survey.getId(),
                survey.getTitle(),
                survey.getDescription(),
                survey.getVisibility(),
                survey.getResponseStatus(),
                survey.getAccessLevel(),
                survey.getDeadline(),
                survey.getCreatedAt(),
                survey.getUpdatedAt(),
                questions
        );
    }

    /**
     * 질문 응답 DTO.
     *
     * @param id           질문 ID
     * @param questionType 질문 유형
     * @param title        질문 제목
     * @param description  질문 설명
     * @param required     필수 응답 여부
     * @param displayOrder 표시 순서
     * @param scaleMin     선형 배율 최솟값 (LINEAR_SCALE만)
     * @param scaleMax     선형 배율 최댓값 (LINEAR_SCALE만)
     * @param options      선택지 목록
     * @param rows         그리드 행 목록
     */
    public record QuestionResponse(
            Long id,
            SurveyQuestionType questionType,
            String title,
            String description,
            boolean required,
            int displayOrder,
            Integer scaleMin,
            Integer scaleMax,
            List<OptionResponse> options,
            List<RowResponse> rows
    ) {
        public static QuestionResponse from(SurveyQuestion question) {
            List<OptionResponse> options = List.of();
            List<RowResponse> rows = List.of();
            Integer scaleMin = null;
            Integer scaleMax = null;

            if (question instanceof LinearScaleSurveyQuestion scaleQ) {
                scaleMin = scaleQ.getScaleMin();
                scaleMax = scaleQ.getScaleMax();
            } else if (question instanceof GridSurveyQuestion gridQ) {
                options = gridQ.getOptions().stream()
                        .filter(o -> !o.isDeleted())
                        .map(OptionResponse::from)
                        .toList();
                rows = gridQ.getRows().stream()
                        .filter(r -> !r.isDeleted())
                        .map(RowResponse::from)
                        .toList();
            } else if (question instanceof OptionSurveyQuestion optionQ) {
                options = optionQ.getOptions().stream()
                        .filter(o -> !o.isDeleted())
                        .map(OptionResponse::from)
                        .toList();
            }
            // TextSurveyQuestion: 추가 필드 없음

            return new QuestionResponse(
                    question.getId(),
                    question.getQuestionType(),
                    question.getTitle(),
                    question.getDescription(),
                    question.isRequired(),
                    question.getDisplayOrder(),
                    scaleMin,
                    scaleMax,
                    options,
                    rows
            );
        }
    }

    /**
     * 선택지 응답 DTO.
     *
     * @param id           선택지 ID
     * @param text         선택지 텍스트
     * @param displayOrder 표시 순서
     */
    public record OptionResponse(
            Long id,
            String text,
            int displayOrder
    ) {
        public static OptionResponse from(SurveyQuestionOption option) {
            return new OptionResponse(
                    option.getId(),
                    option.getText(),
                    option.getDisplayOrder()
            );
        }
    }

    /**
     * 그리드 행 응답 DTO.
     *
     * @param id           행 ID
     * @param label        행 라벨
     * @param displayOrder 표시 순서
     */
    public record RowResponse(
            Long id,
            String label,
            int displayOrder
    ) {
        public static RowResponse from(SurveyQuestionRow row) {
            return new RowResponse(
                    row.getId(),
                    row.getLabel(),
                    row.getDisplayOrder()
            );
        }
    }
}
