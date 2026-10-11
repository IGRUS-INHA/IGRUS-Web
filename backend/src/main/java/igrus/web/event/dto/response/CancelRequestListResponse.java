package igrus.web.event.dto.response;

import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationStatus;
import igrus.web.user.domain.User;

import java.time.Instant;

/**
 * 신청 취소 요청 알림 항목 응답 DTO. (운영진용)
 *
 * @param registrationId    신청 ID
 * @param eventId           행사 ID
 * @param eventTitle        행사 제목
 * @param userName          신청자 이름
 * @param studentId         학번
 * @param isExternal        외부인 신청 여부
 * @param cancelReason      취소 요청 사유
 * @param cancelRequestedAt 취소 요청 일시
 * @param status            신청 상태 (CANCELED이면 처리 완료된 요청)
 */
public record CancelRequestListResponse(
        Long registrationId,
        Long eventId,
        String eventTitle,
        String userName,
        String studentId,
        Boolean isExternal,
        String cancelReason,
        Instant cancelRequestedAt,
        EventRegistrationStatus status
) {
    public static CancelRequestListResponse from(EventRegistration registration) {
        var user = registration.getUser();
        return new CancelRequestListResponse(
                registration.getId(),
                registration.getEvent().getId(),
                registration.getEvent().getTitle(),
                resolveUserName(registration),
                user != null ? user.getStudentId() : registration.getExternalStudentId(),
                registration.getIsExternal(),
                registration.getCancelReason(),
                registration.getCancelRequestedAt(),
                registration.getStatus()
        );
    }

    /**
     * 탈퇴(soft delete)한 회원은 @SQLRestriction 때문에 user가 null로 조회되므로
     * 외부인 신청과 구분하여 탈퇴 표시 이름을 내려준다.
     */
    private static String resolveUserName(EventRegistration registration) {
        if (registration.getUser() != null) {
            return registration.getUser().getName();
        }
        return Boolean.TRUE.equals(registration.getIsExternal())
                ? registration.getExternalName()
                : User.WITHDRAWN_DISPLAY_NAME;
    }
}
