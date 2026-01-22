package igrus.web.user.domain;

import igrus.web.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 사용자 기본정보
@Entity
@Table(name = "users")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "users_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "users_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "users_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "users_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "users_id")
    private Long id;

    // 학번
    @Column(name = "users_student_id", unique = true, nullable = false, length = 8)
    private String studentId;

    // 본명
    @Column(name = "users_name", nullable = false, length = 50)
    private String name;

    // 이메일
    @Column(name = "users_email", unique = true, nullable = false)
    private String email;

    // 전화번호
    @Column(name = "users_phone_number", unique = true, nullable = false, length = 20)
    private String phoneNumber;

    // 학과
    @Column(name = "users_department", nullable = false, length = 50)
    private String department;

    // 가입 동기
    @Column(name = "users_motivation", nullable = false, columnDefinition = "TEXT")
    private String motivation;

    // 역할
    @Enumerated(EnumType.STRING)
    @Column(name = "users_role", nullable = false)
    private UserRole role = UserRole.ASSOCIATE;

    // 직책 (다대다: 한 유저가 여러 직책 보유 가능)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPosition> userPositions = new ArrayList<>();

    // === 정적 팩토리 메서드 ===

    public static User create(String studentId, String name, String email,
                              String phoneNumber, String department, String motivation) {
        User user = new User();
        user.studentId = studentId;
        user.name = name;
        user.email = email;
        user.phoneNumber = phoneNumber;
        user.department = department;
        user.motivation = motivation;
        user.role = UserRole.ASSOCIATE;
        return user;
    }

    // === 역할 변경 ===

    public void promoteToMember() {
        this.role = UserRole.MEMBER;
    }

    public void promoteToOperator() {
        this.role = UserRole.OPERATOR;
    }

    public void promoteToAdmin() {
        this.role = UserRole.ADMIN;
    }

    public void demoteToMember() {
        this.role = UserRole.MEMBER;
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }

    // === 역할 확인 ===

    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }

    public boolean isOperator() {
        return this.role == UserRole.OPERATOR;
    }

    public boolean isOperatorOrAbove() {
        return this.role == UserRole.OPERATOR || this.role == UserRole.ADMIN;
    }

    public boolean isMember() {
        return this.role == UserRole.MEMBER;
    }

    public boolean isAssociate() {
        return this.role == UserRole.ASSOCIATE;
    }

    // === 직책 관련 ===

    public void addPosition(Position position) {
        boolean alreadyHas = this.userPositions.stream()
                .anyMatch(up -> up.getPosition().equals(position));
        if (!alreadyHas) {
            UserPosition userPosition = UserPosition.create(this, position);
            this.userPositions.add(userPosition);
        }
    }

    public void removePosition(Position position) {
        this.userPositions.removeIf(up -> up.getPosition().equals(position));
    }

    public void clearPositions() {
        this.userPositions.clear();
    }

    public boolean hasPosition(Position position) {
        return this.userPositions.stream()
                .anyMatch(up -> up.getPosition().equals(position));
    }

    public boolean hasAnyPosition() {
        return !this.userPositions.isEmpty();
    }

    public List<Position> getPositions() {
        return this.userPositions.stream()
                .map(UserPosition::getPosition)
                .toList();
    }

    public List<UserPosition> getUserPositions() {
        return Collections.unmodifiableList(this.userPositions);
    }

    // === 프로필 수정 ===

    public void updateProfile(String name, String phoneNumber, String department) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.department = department;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

}
