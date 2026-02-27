package igrus.web.survey.statistics.dto.response;

import igrus.web.user.domain.User;

/**
 * 설문 응답자 정보 DTO.
 * 운영자가 응답자의 기본 정보를 확인할 수 있도록 제공합니다.
 *
 * <p>soft-deleted 사용자는 {@code @SQLRestriction}에 의해 user가 null로 로드되므로 생략됩니다.
 *
 * @param userId     사용자 ID (탈퇴 사용자는 null)
 * @param name       사용자 이름 (탈퇴 사용자는 "탈퇴한 사용자")
 * @param grade      학년
 * @param gender     성별
 * @param department 학과
 */
public record RespondentInfo(
        Long userId,
        String name,
        Integer grade,
        String gender,
        String department
) {

    /**
     * User 엔티티로부터 RespondentInfo를 생성합니다.
     *
     * @param user 사용자 엔티티
     * @return 응답자 정보
     */
    public static RespondentInfo from(User user) {
        return new RespondentInfo(
                user.getDisplayId(),
                user.getDisplayName(),
                user.getGrade(),
                user.getGender() != null ? user.getGender().name() : null,
                user.getDepartment()
        );
    }
}
