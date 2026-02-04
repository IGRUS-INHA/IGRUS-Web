package igrus.web.user.semester.domain;

import igrus.web.common.domain.BaseEntity;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "semester_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_semester_members_user_year_semester",
                columnNames = {"semester_members_user_id", "semester_members_year", "semester_members_semester"}
        )
)
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "semester_members_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "semester_members_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "semester_members_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "semester_members_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SemesterMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "semester_members_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_members_user_id", nullable = false)
    private User user;

    @Column(name = "semester_members_year", nullable = false)
    private Integer year;

    @Column(name = "semester_members_semester", nullable = false)
    private Integer semester;

    @Enumerated(EnumType.STRING)
    @Column(name = "semester_members_role", nullable = false, length = 20)
    private UserRole role;

    /** 등록 시점 스냅샷: 사용자 본명 */
    @Column(name = "semester_members_name", nullable = false, length = 50)
    private String name;

    /** 등록 시점 스냅샷: 학번 */
    @Column(name = "semester_members_student_id", nullable = false, length = 20)
    private String studentId;

    /** 등록 시점 스냅샷: 학과 */
    @Column(name = "semester_members_department", length = 50)
    private String department;

    /** 등록 시점 스냅샷: 학년 */
    @Column(name = "semester_members_grade", nullable = false)
    private Integer grade;

    /** 등록 시점 스냅샷: 가입 동기 */
    @Column(name = "semester_members_motivation", columnDefinition = "TEXT")
    private String motivation;

    public static SemesterMember create(User user, int year, int semester, UserRole role) {
        SemesterMember member = new SemesterMember();
        member.user = user;
        member.year = year;
        member.semester = semester;
        member.role = role;
        member.name = user.getName();
        member.studentId = user.getStudentId();
        member.department = user.getDepartment();
        member.grade = user.getGrade();
        member.motivation = user.getMotivation();
        return member;
    }
}
