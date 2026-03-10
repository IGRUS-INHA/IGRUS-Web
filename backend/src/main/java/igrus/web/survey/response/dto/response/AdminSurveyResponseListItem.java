package igrus.web.survey.response.dto.response;

import igrus.web.event.domain.ExternalSurveyResponse;
import igrus.web.survey.response.domain.SurveyResponse;

import java.time.Instant;
import java.util.List;

/**
 * 관리자 설문 응답 목록 항목 DTO.
 * 응답 ID, 응답자 정보, 제출 시각, 답변 목록을 포함합니다.
 * 회원 응답은 userId로, 외부인 응답은 registrationId로 식별합니다.
 */
public record AdminSurveyResponseListItem(
        Long responseId,
        Long registrationId,
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
                null,
                response.getUser() != null ? response.getUser().getId() : null,
                response.getUser() != null ? response.getUser().getName() : null,
                response.getCreatedAt(),
                answerResponses
        );
    }

    /**
     * ExternalSurveyResponse 엔티티에서 관리자 응답 목록 항목 DTO를 생성합니다.
     * 외부인 응답이므로 userId, userName은 null이고, registrationId가 설정됩니다.
     *
     * @param externalResponse 외부인 설문 응답 엔티티
     * @param answers          파싱된 답변 목록
     * @return 관리자 응답 목록 항목 DTO
     */
    public static AdminSurveyResponseListItem fromExternal(
            ExternalSurveyResponse externalResponse,
            List<SurveyResponseDetailResponse.AnswerResponse> answers) {
        return new AdminSurveyResponseListItem(
                externalResponse.getId(),
                externalResponse.getRegistrationId(),
                null,
                null,
                externalResponse.getCreatedAt(),
                answers
        );
    }
}
