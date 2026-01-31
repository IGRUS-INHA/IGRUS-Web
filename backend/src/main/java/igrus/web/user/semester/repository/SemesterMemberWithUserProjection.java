package igrus.web.user.semester.repository;

/**
 * 학기별 회원과 사용자 정보를 함께 조회하기 위한 Native Query Projection.
 * User의 @SQLRestriction을 우회하여 탈퇴 회원도 포함합니다.
 *
 * <p>컬럼 alias와 getter 메서드명이 자동 매핑됩니다.
 */
public interface SemesterMemberWithUserProjection {

    Long getUserId();

    String getStudentId();

    String getName();

    String getDepartment();

    String getEmail();

    String getPhoneNumber();

    String getMemberRole();

    Boolean getDeleted();
}
