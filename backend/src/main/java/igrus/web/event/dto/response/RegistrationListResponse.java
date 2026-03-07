package igrus.web.event.dto.response;

import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationStatus;

import java.time.Instant;

/**
 * 행사 신청자 목록 응답 DTO.
 * 관리자가 신청자 목록을 조회할 때 사용합니다.
 * 회원/외부인 신청 모두를 지원합니다.
 *
 * @param registrationId 신청 ID
 * @param userId         신청자 ID (외부인인 경우 null)
 * @param userName       신청자 이름
 * @param userEmail      신청자 이메일 (외부인인 경우 null)
 * @param studentId      학번
 * @param userGender     성별 ("MALE" 또는 "FEMALE", 외부인인 경우 null)
 * @param userGrade      학년 (외부인인 경우 null)
 * @param userDepartment 학과
 * @param status         신청 상태
 * @param registeredAt   신청일시
 * @param isExternal     외부인 신청 여부
 * @param phone          외부인 전화번호 (회원인 경우 null)
 */
public record RegistrationListResponse(
        Long registrationId,
        Long userId,
        String userName,
        String userEmail,
        String studentId,
        String userGender,
        Integer userGrade,
        String userDepartment,
        EventRegistrationStatus status,
        Instant registeredAt,
        Boolean isExternal,
        String phone
) {
    /**
     * EventRegistration 엔티티로부터 RegistrationListResponse를 생성합니다.
     * 외부인 신청(user == null)과 회원 신청 모두를 처리합니다.
     *
     * @param registration 신청 엔티티
     * @return RegistrationListResponse
     */
    public static RegistrationListResponse from(EventRegistration registration) {
        var user = registration.getUser();
        if (user != null) {
            // 회원 신청
            return new RegistrationListResponse(
                    registration.getId(),
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getStudentId(),
                    user.getGender() != null ? user.getGender().name() : null,
                    user.getGrade(),
                    user.getDepartment(),
                    registration.getStatus(),
                    registration.getRegisteredAt(),
                    registration.getIsExternal(),
                    null
            );
        } else {
            // 외부인 신청
            return new RegistrationListResponse(
                    registration.getId(),
                    null,
                    registration.getExternalName(),
                    null,
                    registration.getExternalStudentId(),
                    null,
                    null,
                    registration.getExternalDepartment(),
                    registration.getStatus(),
                    registration.getRegisteredAt(),
                    registration.getIsExternal(),
                    registration.getExternalPhone()
            );
        }
    }
}
