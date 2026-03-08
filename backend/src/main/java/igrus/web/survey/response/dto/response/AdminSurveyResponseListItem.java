package igrus.web.survey.response.dto.response;

import igrus.web.survey.response.domain.SurveyResponse;

import java.time.Instant;
import java.util.List;

/**
 * 관리자 설문 응답 목록 항목 DTO.
 * 응답 ID, 응답자 정보, 제출 시각, 답변 목록을 포함합니다.
 */
public record AdminSurveyResponseListItem(
        Long responseId,
        Long userId,
        String userName,
        Instant submittedAt,
        List<SurveyResponseDetailResponse.AnswerResponse> answers
) {

    /**
     * SurveyResponse 엔티티에서 관리자 응답 목록 항목 DTO를 생성합니다.
     * user가 null인 경우 (비회원 응답) userId, userName은 null입니다.
     *
     * @param response 설문 응답 엔티티 (user fetch join 필요)
     * @return 관리자 응답 목록 항목 DTO
     */
    public static AdminSurveyResponseListItem from(SurveyResponse response) {
        List<SurveyResponseDetailResponse.AnswerResponse> answerResponses =
                SurveyResponseDetailResponse.groupAnswersByQuestion(response.getAnswers());

        return new AdminSurveyResponseListItem(
                response.getId(),
                response.getUser() != null ? response.getUser().getId() : null,
                response.getUser() != null ? response.getUser().getName() : null,
                response.getCreatedAt(),
                answerResponses
        );
    }
}
