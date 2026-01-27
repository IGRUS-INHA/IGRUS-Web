package igrus.web.common.fixture;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;

import static igrus.web.common.fixture.TestConstants.*;
import static igrus.web.common.fixture.TestEntityIdAssigner.withId;

/**
 * User 도메인 관련 테스트 픽스처 클래스.
 *
 * <p>테스트에서 사용되는 User 엔티티와 AuthenticatedUser 객체를 생성하는 팩토리 메서드를 제공합니다.
 */
public final class UserTestFixture {

    private UserTestFixture() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    // ==================== User 생성 (ID 없음) ====================

    /**
     * 정회원(MEMBER) 역할의 User를 생성합니다.
     *
     * @return 정회원 User
     */
    public static User createMember() {
        return createUserWithRole(MEMBER_STUDENT_ID, DEFAULT_NAME, UserRole.MEMBER);
    }

    /**
     * 운영진(OPERATOR) 역할의 User를 생성합니다.
     *
     * @return 운영진 User
     */
    public static User createOperator() {
        return createUserWithRole(OPERATOR_STUDENT_ID, OPERATOR_NAME, UserRole.OPERATOR);
    }

    /**
     * 관리자(ADMIN) 역할의 User를 생성합니다.
     *
     * @return 관리자 User
     */
    public static User createAdmin() {
        return createUserWithRole(ADMIN_STUDENT_ID, ADMIN_NAME, UserRole.ADMIN);
    }

    /**
     * 다른 정회원 User를 생성합니다.
     *
     * <p>권한 테스트 등에서 작성자가 아닌 다른 회원이 필요할 때 사용합니다.
     *
     * @return 다른 정회원 User
     */
    public static User createAnotherMember() {
        return createUserWithRole(ANOTHER_MEMBER_STUDENT_ID, ANOTHER_MEMBER_NAME, UserRole.MEMBER);
    }

    /**
     * 지정된 역할의 User를 생성합니다.
     *
     * @param studentId 학번
     * @param name      이름
     * @param role      사용자 역할
     * @return 생성된 User
     */
    public static User createUserWithRole(String studentId, String name, UserRole role) {
        String email = studentId + DEFAULT_EMAIL_DOMAIN;
        User user = User.create(
                studentId,
                name,
                email,
                DEFAULT_PHONE,
                DEFAULT_DEPARTMENT,
                DEFAULT_MOTIVATION
        );
        user.changeRole(role);
        user.verifyEmail();
        return user;
    }

    // ==================== User 생성 (ID 포함) ====================

    /**
     * ID가 설정된 정회원(MEMBER) User를 생성합니다.
     *
     * @return ID가 설정된 정회원 User
     */
    public static User createMemberWithId() {
        return withId(createMember(), DEFAULT_MEMBER_ID);
    }

    /**
     * 지정된 ID가 설정된 정회원(MEMBER) User를 생성합니다.
     *
     * @param id 설정할 ID
     * @return ID가 설정된 정회원 User
     */
    public static User createMemberWithId(Long id) {
        return withId(createMember(), id);
    }

    /**
     * ID가 설정된 운영진(OPERATOR) User를 생성합니다.
     *
     * @return ID가 설정된 운영진 User
     */
    public static User createOperatorWithId() {
        return withId(createOperator(), DEFAULT_OPERATOR_ID);
    }

    /**
     * 지정된 ID가 설정된 운영진(OPERATOR) User를 생성합니다.
     *
     * @param id 설정할 ID
     * @return ID가 설정된 운영진 User
     */
    public static User createOperatorWithId(Long id) {
        return withId(createOperator(), id);
    }

    /**
     * ID가 설정된 관리자(ADMIN) User를 생성합니다.
     *
     * @return ID가 설정된 관리자 User
     */
    public static User createAdminWithId() {
        return withId(createAdmin(), DEFAULT_ADMIN_ID);
    }

    /**
     * 지정된 ID가 설정된 관리자(ADMIN) User를 생성합니다.
     *
     * @param id 설정할 ID
     * @return ID가 설정된 관리자 User
     */
    public static User createAdminWithId(Long id) {
        return withId(createAdmin(), id);
    }

    /**
     * ID가 설정된 다른 정회원 User를 생성합니다.
     *
     * @return ID가 설정된 다른 정회원 User
     */
    public static User createAnotherMemberWithId() {
        return withId(createAnotherMember(), ANOTHER_MEMBER_ID);
    }

    // ==================== AuthenticatedUser 생성 ====================

    /**
     * 정회원(MEMBER) 역할의 AuthenticatedUser를 생성합니다.
     *
     * @return 정회원 AuthenticatedUser
     */
    public static AuthenticatedUser memberAuth() {
        return new AuthenticatedUser(DEFAULT_MEMBER_ID, MEMBER_STUDENT_ID, UserRole.MEMBER.name());
    }

    /**
     * 지정된 ID의 정회원 AuthenticatedUser를 생성합니다.
     *
     * @param userId 사용자 ID
     * @return 정회원 AuthenticatedUser
     */
    public static AuthenticatedUser memberAuth(Long userId) {
        return new AuthenticatedUser(userId, MEMBER_STUDENT_ID, UserRole.MEMBER.name());
    }

    /**
     * 운영진(OPERATOR) 역할의 AuthenticatedUser를 생성합니다.
     *
     * @return 운영진 AuthenticatedUser
     */
    public static AuthenticatedUser operatorAuth() {
        return new AuthenticatedUser(DEFAULT_OPERATOR_ID, OPERATOR_STUDENT_ID, UserRole.OPERATOR.name());
    }

    /**
     * 지정된 ID의 운영진 AuthenticatedUser를 생성합니다.
     *
     * @param userId 사용자 ID
     * @return 운영진 AuthenticatedUser
     */
    public static AuthenticatedUser operatorAuth(Long userId) {
        return new AuthenticatedUser(userId, OPERATOR_STUDENT_ID, UserRole.OPERATOR.name());
    }

    /**
     * 관리자(ADMIN) 역할의 AuthenticatedUser를 생성합니다.
     *
     * @return 관리자 AuthenticatedUser
     */
    public static AuthenticatedUser adminAuth() {
        return new AuthenticatedUser(DEFAULT_ADMIN_ID, ADMIN_STUDENT_ID, UserRole.ADMIN.name());
    }

    /**
     * 지정된 ID의 관리자 AuthenticatedUser를 생성합니다.
     *
     * @param userId 사용자 ID
     * @return 관리자 AuthenticatedUser
     */
    public static AuthenticatedUser adminAuth(Long userId) {
        return new AuthenticatedUser(userId, ADMIN_STUDENT_ID, UserRole.ADMIN.name());
    }
}
