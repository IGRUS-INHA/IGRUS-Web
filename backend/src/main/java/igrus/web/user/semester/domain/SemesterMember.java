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

    public static SemesterMember create(User user, int year, int semester, UserRole role) {
        SemesterMember member = new SemesterMember();
        member.user = user;
        member.year = year;
        member.semester = semester;
        member.role = role;
        return member;
    }
}
