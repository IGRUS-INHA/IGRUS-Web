package igrus.web.event.dto.response;

import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationStatus;

import java.time.Instant;

/**
 * 행사 신청자 목록 응답 DTO.
 * 관리자가 신청자 목록을 조회할 때 사용합니다.
 *
 * @param registrationId 신청 ID
 * @param userId         신청자 ID
 * @param userName       신청자 이름
 * @param userEmail      신청자 이메일
 * @param studentId      학번
 * @param status         신청 상태
 * @param registeredAt   신청일시
 */
public record RegistrationListResponse(
        Long registrationId,
        Long userId,
        String userName,
        String userEmail,
        String studentId,
        EventRegistrationStatus status,
        Instant registeredAt
) {
    /**
     * EventRegistration 엔티티로부터 RegistrationListResponse를 생성합니다.
     *
     * @param registration 신청 엔티티
     * @return RegistrationListResponse
     */
    public static RegistrationListResponse from(EventRegistration registration) {
        return new RegistrationListResponse(
                registration.getId(),
                registration.getUser().getId(),
                registration.getUser().getName(),
                registration.getUser().getEmail(),
                registration.getUser().getStudentId(),
                registration.getStatus(),
                registration.getRegisteredAt()
        );
    }
}
