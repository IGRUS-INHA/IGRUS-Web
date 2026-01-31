package igrus.web.user.semester.repository;

import igrus.web.user.domain.User;
import igrus.web.user.semester.domain.SemesterMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SemesterMemberRepository extends JpaRepository<SemesterMember, Long> {

    Page<SemesterMember> findByYearAndSemester(int year, int semester, Pageable pageable);

    List<SemesterMember> findByYearAndSemester(int year, int semester);

    List<SemesterMember> findByUser(User user);

    boolean existsByUserAndYearAndSemester(User user, int year, int semester);

    void deleteByUserAndYearAndSemester(User user, int year, int semester);

    long countByYearAndSemester(int year, int semester);

    /**
     * User의 @SQLRestriction 필터를 우회하여 탈퇴 회원 포함 전체 조회.
     * Native 쿼리로 semester_members와 users를 직접 JOIN합니다.
     */
    @Query(value = "SELECT u.users_id AS userId, u.users_student_id AS studentId, " +
            "u.users_name AS name, u.users_department AS department, " +
            "u.users_email AS email, u.users_phone_number AS phoneNumber, " +
            "sm.semester_members_role AS memberRole, u.users_deleted AS deleted " +
            "FROM semester_members sm " +
            "JOIN users u ON sm.semester_members_user_id = u.users_id " +
            "WHERE sm.semester_members_year = :year AND sm.semester_members_semester = :semester",
            nativeQuery = true)
    List<SemesterMemberWithUserProjection> findAllWithUserIncludingDeleted(@Param("year") int year, @Param("semester") int semester);

    /**
     * 학기 목록과 각 학기별 회원 수를 조회합니다.
     */
    @Query(value = "SELECT sm.semester_members_year AS semesterYear, sm.semester_members_semester AS semesterTerm, " +
            "COUNT(*) AS memberCount " +
            "FROM semester_members sm " +
            "GROUP BY sm.semester_members_year, sm.semester_members_semester " +
            "ORDER BY sm.semester_members_year DESC, sm.semester_members_semester DESC",
            nativeQuery = true)
    List<SemesterSummaryProjection> findDistinctSemestersWithCount();
}
