package igrus.web.event.dto.response;

import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationStatus;

/**
 * 행사 신청 결과 응답 DTO.
 * 신청, 취소 등의 결과를 반환할 때 사용합니다.
 *
 * @param registrationId 신청 ID
 * @param status         신청 상태
 */
public record RegistrationResponse(
        Long registrationId,
        EventRegistrationStatus status
) {
    /**
     * EventRegistration 엔티티로부터 RegistrationResponse를 생성합니다.
     *
     * @param registration 신청 엔티티
     * @return RegistrationResponse
     */
    public static RegistrationResponse from(EventRegistration registration) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getStatus()
        );
    }
}
