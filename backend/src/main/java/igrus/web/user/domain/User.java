package igrus.web.user.domain;

import igrus.web.common.domain.SoftDeletableEntity;
import igrus.web.user.exception.InvalidEmailException;
import igrus.web.user.exception.InvalidStudentIdException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 사용자 기본정보
@Entity
@Table(name = "users")
@SQLRestriction("users_deleted = false")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "users_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "users_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "users_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "users_updated_by")),
        @AttributeOverride(name = "deleted", column = @Column(name = "users_deleted", nullable = false)),
        @AttributeOverride(name = "deletedAt", column = @Column(name = "users_deleted_at")),
        @AttributeOverride(name = "deletedBy", column = @Column(name = "users_deleted_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeletableEntity {

    /** 사용자 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "users_id")
    private Long id;

    /** 학번 (8자리 숫자, 고유값) */
    @Column(name = "users_student_id", unique = true, nullable = false, length = 20)
    private String studentId;

    /** 사용자 본명 (최대 50자) */
    @Column(name = "users_name", nullable = false, length = 50)
    private String name;

    /** 사용자 이메일 (고유값) */
    @Column(name = "users_email", unique = true, nullable = false)
    private String email;

    /** 전화번호 (최대 20자, 익명화 시 null) */
    @Column(name = "users_phone_number", unique = true, length = 20)
    private String phoneNumber;

    /** 학과 (최대 50자, 익명화 시 null) */
    @Column(name = "users_department", length = 50)
    private String department;

    /** 가입 동기 (익명화 시 null) */
    @Column(name = "users_motivation", columnDefinition = "TEXT")
    private String motivation;

    /** 사용자 역할 (ASSOCIATE, MEMBER, OPERATOR, ADMIN). 기본값: ASSOCIATE */
    @Enumerated(EnumType.STRING)
    @Column(name = "users_role", nullable = false)
    private UserRole role = UserRole.ASSOCIATE;

    /** 사용자 상태 (PENDING_VERIFICATION, ACTIVE, SUSPENDED, WITHDRAWN). 기본값: PENDING_VERIFICATION */
    @Enumerated(EnumType.STRING)
    @Column(name = "users_status", nullable = false)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    /**
     * 익명화 처리 완료 여부.
     * <p>
     * 탈퇴 후 복구 가능 기간(5일) 만료 시 개인정보 영구 삭제 스케줄러에 의해 true로 설정됩니다.
     * 스케줄러가 매일 실행될 때, 이미 익명화된 사용자를 중복 처리하지 않도록 구분하는 용도입니다.
     * </p>
     *
     * @see igrus.web.security.auth.common.scheduler.WithdrawnUserCleanupScheduler
     */
    @Column(name = "users_anonymized", nullable = false)
    private boolean anonymized = false;

    /** 사용자 직책 목록 (다대다 관계: 한 사용자가 여러 직책 보유 가능) */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPosition> userPositions = new ArrayList<>();

    // === 정적 팩토리 메서드 ===

    public static User create(String studentId, String name, String email,
                              String phoneNumber, String department, String motivation) {
        validateStudentId(studentId);
        validateEmail(email);

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

    private static void validateStudentId(String studentId) {
        if (studentId == null || !studentId.matches("^\\d{8}$")) {
            throw new InvalidStudentIdException(studentId);
        }
    }

    private static void validateEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (email == null || !email.matches(emailRegex)) {
            throw new InvalidEmailException(email);
        }
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

    // === 상태 확인 ===

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean isSuspended() {
        return this.status == UserStatus.SUSPENDED;
    }

    public boolean isWithdrawn() {
        return this.status == UserStatus.WITHDRAWN;
    }

    public boolean isPendingVerification() {
        return this.status == UserStatus.PENDING_VERIFICATION;
    }

    // === 상태 변경 ===

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
    }

    public void verifyEmail() {
        if (this.status == UserStatus.PENDING_VERIFICATION) {
            this.status = UserStatus.ACTIVE;
        }
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

    // === 익명화 ===

    /**
     * 사용자 개인정보를 익명화합니다.
     * <p>
     * 탈퇴 후 복구 가능 기간 만료 시 호출됩니다.
     * 이름, 이메일, 학번을 익명 값으로 대체하고, 전화번호/학과/가입동기는 null로 설정합니다.
     * </p>
     *
     * @param hash 익명화에 사용할 고유 해시값 (예: UUID 앞 8자리)
     */
    public void anonymize(String hash) {
        this.name = "탈퇴회원_" + hash;
        this.email = "deleted_" + hash + "@deleted.local";
        this.studentId = "DELETED_" + this.id;
        this.phoneNumber = null;
        this.department = null;
        this.motivation = null;
        this.anonymized = true;
    }

    public boolean isAnonymized() {
        return this.anonymized;
    }

}
