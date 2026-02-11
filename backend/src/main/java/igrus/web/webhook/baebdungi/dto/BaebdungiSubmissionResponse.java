package igrus.web.webhook.baebdungi.dto;

/**
 * 뱁둥이봇 웹훅 제출 응답 DTO.
 * 웹훅 API의 응답을 파싱합니다.
 *
 * @param success      성공 여부
 * @param submissionId 제출 ID (성공 시)
 * @param message      메시지 (성공 시)
 * @param error        에러 메시지 (실패 시)
 */
public record BaebdungiSubmissionResponse(
        Boolean success,
        String submissionId,
        String message,
        String error
) {
}
