package igrus.web.user.semester.repository;

/**
 * 학기 목록과 회원 수를 조회하기 위한 Native Query Projection.
 */
public interface SemesterSummaryProjection {

    Integer getSemesterYear();

    Integer getSemesterTerm();

    Long getMemberCount();
}
