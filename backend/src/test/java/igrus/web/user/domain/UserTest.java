package igrus.web.user.domain;

import igrus.web.user.exception.InvalidEmailException;
import igrus.web.user.exception.InvalidStudentIdException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User 도메인")
class UserTest {

    private User createTestUser() {
        return User.create("20231234", "홍길동", "test@inha.edu", "010-1234-5678", "컴퓨터공학과", "테스트 동기");
    }

    private Position createTestPosition(String name) {
        return Position.create(name, "/images/" + name + ".png", 1);
    }

    @Nested
    @DisplayName("create 정적 팩토리 메서드")
    class CreateTest {

        @Test
        @DisplayName("유효한 정보로 User 생성 성공")
        void create_WithValidInfo_ReturnsUser() {
            // given
            String studentId = "20231234";
            String name = "홍길동";
            String email = "test@inha.edu";
            String phoneNumber = "010-1234-5678";
            String department = "컴퓨터공학과";
            String motivation = "테스트 동기";

            // when
            User user = User.create(studentId, name, email, phoneNumber, department, motivation);

            // then
            assertThat(user).isNotNull();
            assertThat(user.getStudentId()).isEqualTo(studentId);
            assertThat(user.getName()).isEqualTo(name);
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getPhoneNumber()).isEqualTo(phoneNumber);
            assertThat(user.getDepartment()).isEqualTo(department);
            assertThat(user.getMotivation()).isEqualTo(motivation);
        }

        @Test
        @DisplayName("생성 시 기본 역할은 ASSOCIATE")
        void create_DefaultRole_IsAssociate() {
            // when
            User user = createTestUser();

            // then
            assertThat(user.getRole()).isEqualTo(UserRole.ASSOCIATE);
            assertThat(user.isAssociate()).isTrue();
        }

        @Nested
        @DisplayName("학번 검증")
        class StudentIdValidationTest {

            @Test
            @DisplayName("null 학번 -> InvalidStudentIdException")
            void create_WithNullStudentId_ThrowsException() {
                assertThatThrownBy(() ->
                        User.create(null, "홍길동", "test@inha.edu", "010-1234-5678", "컴퓨터공학과", "동기"))
                        .isInstanceOf(InvalidStudentIdException.class);
            }

            @Test
            @DisplayName("빈 문자열 학번 -> InvalidStudentIdException")
            void create_WithEmptyStudentId_ThrowsException() {
                assertThatThrownBy(() ->
                        User.create("", "홍길동", "test@inha.edu", "010-1234-5678", "컴퓨터공학과", "동기"))
                        .isInstanceOf(InvalidStudentIdException.class);
            }

            @Test
            @DisplayName("7자리 학번 -> InvalidStudentIdException")
            void create_With7DigitStudentId_ThrowsException() {
                assertThatThrownBy(() ->
                        User.create("2023123", "홍길동", "test@inha.edu", "010-1234-5678", "컴퓨터공학과", "동기"))
                        .isInstanceOf(InvalidStudentIdException.class);
            }

            @Test
            @DisplayName("9자리 학번 -> InvalidStudentIdException")
            void create_With9DigitStudentId_ThrowsException() {
                assertThatThrownBy(() ->
                        User.create("202312345", "홍길동", "test@inha.edu", "010-1234-5678", "컴퓨터공학과", "동기"))
                        .isInstanceOf(InvalidStudentIdException.class);
            }

            @Test
            @DisplayName("숫자가 아닌 문자 포함 학번 -> InvalidStudentIdException")
            void create_WithNonNumericStudentId_ThrowsException() {
                assertThatThrownBy(() ->
                        User.create("2023123a", "홍길동", "test@inha.edu", "010-1234-5678", "컴퓨터공학과", "동기"))
                        .isInstanceOf(InvalidStudentIdException.class);
            }
        }

        @Nested
        @DisplayName("이메일 검증")
        class EmailValidationTest {

            @Test
            @DisplayName("null 이메일 -> InvalidEmailException")
            void create_WithNullEmail_ThrowsException() {
                assertThatThrownBy(() ->
                        User.create("20231234", "홍길동", null, "010-1234-5678", "컴퓨터공학과", "동기"))
                        .isInstanceOf(InvalidEmailException.class);
            }

            @Test
            @DisplayName("빈 문자열 이메일 -> InvalidEmailException")
            void create_WithEmptyEmail_ThrowsException() {
                assertThatThrownBy(() ->
                        User.create("20231234", "홍길동", "", "010-1234-5678", "컴퓨터공학과", "동기"))
                        .isInstanceOf(InvalidEmailException.class);
            }

            @Test
            @DisplayName("@ 없는 이메일 -> InvalidEmailException")
            void create_WithoutAtSign_ThrowsException() {
                assertThatThrownBy(() ->
                        User.create("20231234", "홍길동", "testinha.edu", "010-1234-5678", "컴퓨터공학과", "동기"))
                        .isInstanceOf(InvalidEmailException.class);
            }

            @Test
            @DisplayName("도메인 없는 이메일 -> InvalidEmailException")
            void create_WithoutDomain_ThrowsException() {
                assertThatThrownBy(() ->
                        User.create("20231234", "홍길동", "test@", "010-1234-5678", "컴퓨터공학과", "동기"))
                        .isInstanceOf(InvalidEmailException.class);
            }
        }
    }

    @Nested
    @DisplayName("역할 변경 메서드")
    class RoleChangeTest {

        @Test
        @DisplayName("promoteToMember 호출 시 MEMBER로 변경")
        void promoteToMember_ChangesRoleToMember() {
            // given
            User user = createTestUser();

            // when
            user.promoteToMember();

            // then
            assertThat(user.getRole()).isEqualTo(UserRole.MEMBER);
        }

        @Test
        @DisplayName("promoteToOperator 호출 시 OPERATOR로 변경")
        void promoteToOperator_ChangesRoleToOperator() {
            // given
            User user = createTestUser();

            // when
            user.promoteToOperator();

            // then
            assertThat(user.getRole()).isEqualTo(UserRole.OPERATOR);
        }

        @Test
        @DisplayName("promoteToAdmin 호출 시 ADMIN으로 변경")
        void promoteToAdmin_ChangesRoleToAdmin() {
            // given
            User user = createTestUser();

            // when
            user.promoteToAdmin();

            // then
            assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("demoteToMember 호출 시 MEMBER로 변경")
        void demoteToMember_ChangesRoleToMember() {
            // given
            User user = createTestUser();
            user.promoteToOperator();

            // when
            user.demoteToMember();

            // then
            assertThat(user.getRole()).isEqualTo(UserRole.MEMBER);
        }

        @Test
        @DisplayName("changeRole 호출 시 지정한 역할로 변경")
        void changeRole_WithSpecifiedRole_ChangesRole() {
            // given
            User user = createTestUser();

            // when
            user.changeRole(UserRole.ADMIN);

            // then
            assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        }
    }

    @Nested
    @DisplayName("역할 확인 메서드")
    class RoleCheckTest {

        @Nested
        @DisplayName("isAdmin")
        class IsAdminTest {

            @Test
            @DisplayName("ADMIN일 때 true 반환")
            void isAdmin_WhenAdmin_ReturnsTrue() {
                // given
                User user = createTestUser();
                user.promoteToAdmin();

                // then
                assertThat(user.isAdmin()).isTrue();
            }

            @Test
            @DisplayName("ASSOCIATE일 때 false 반환")
            void isAdmin_WhenAssociate_ReturnsFalse() {
                // given
                User user = createTestUser();

                // then
                assertThat(user.isAdmin()).isFalse();
            }

            @Test
            @DisplayName("MEMBER일 때 false 반환")
            void isAdmin_WhenMember_ReturnsFalse() {
                // given
                User user = createTestUser();
                user.promoteToMember();

                // then
                assertThat(user.isAdmin()).isFalse();
            }

            @Test
            @DisplayName("OPERATOR일 때 false 반환")
            void isAdmin_WhenOperator_ReturnsFalse() {
                // given
                User user = createTestUser();
                user.promoteToOperator();

                // then
                assertThat(user.isAdmin()).isFalse();
            }
        }

        @Nested
        @DisplayName("isOperator")
        class IsOperatorTest {

            @Test
            @DisplayName("OPERATOR일 때 true 반환")
            void isOperator_WhenOperator_ReturnsTrue() {
                // given
                User user = createTestUser();
                user.promoteToOperator();

                // then
                assertThat(user.isOperator()).isTrue();
            }

            @Test
            @DisplayName("ASSOCIATE일 때 false 반환")
            void isOperator_WhenAssociate_ReturnsFalse() {
                // given
                User user = createTestUser();

                // then
                assertThat(user.isOperator()).isFalse();
            }

            @Test
            @DisplayName("MEMBER일 때 false 반환")
            void isOperator_WhenMember_ReturnsFalse() {
                // given
                User user = createTestUser();
                user.promoteToMember();

                // then
                assertThat(user.isOperator()).isFalse();
            }

            @Test
            @DisplayName("ADMIN일 때 false 반환")
            void isOperator_WhenAdmin_ReturnsFalse() {
                // given
                User user = createTestUser();
                user.promoteToAdmin();

                // then
                assertThat(user.isOperator()).isFalse();
            }
        }

        @Nested
        @DisplayName("isOperatorOrAbove")
        class IsOperatorOrAboveTest {

            @Test
            @DisplayName("OPERATOR일 때 true 반환")
            void isOperatorOrAbove_WhenOperator_ReturnsTrue() {
                // given
                User user = createTestUser();
                user.promoteToOperator();

                // then
                assertThat(user.isOperatorOrAbove()).isTrue();
            }

            @Test
            @DisplayName("ADMIN일 때 true 반환")
            void isOperatorOrAbove_WhenAdmin_ReturnsTrue() {
                // given
                User user = createTestUser();
                user.promoteToAdmin();

                // then
                assertThat(user.isOperatorOrAbove()).isTrue();
            }

            @Test
            @DisplayName("ASSOCIATE일 때 false 반환")
            void isOperatorOrAbove_WhenAssociate_ReturnsFalse() {
                // given
                User user = createTestUser();

                // then
                assertThat(user.isOperatorOrAbove()).isFalse();
            }

            @Test
            @DisplayName("MEMBER일 때 false 반환")
            void isOperatorOrAbove_WhenMember_ReturnsFalse() {
                // given
                User user = createTestUser();
                user.promoteToMember();

                // then
                assertThat(user.isOperatorOrAbove()).isFalse();
            }
        }

        @Nested
        @DisplayName("isMember")
        class IsMemberTest {

            @Test
            @DisplayName("MEMBER일 때 true 반환")
            void isMember_WhenMember_ReturnsTrue() {
                // given
                User user = createTestUser();
                user.promoteToMember();

                // then
                assertThat(user.isMember()).isTrue();
            }

            @Test
            @DisplayName("ASSOCIATE일 때 false 반환")
            void isMember_WhenAssociate_ReturnsFalse() {
                // given
                User user = createTestUser();

                // then
                assertThat(user.isMember()).isFalse();
            }

            @Test
            @DisplayName("OPERATOR일 때 false 반환")
            void isMember_WhenOperator_ReturnsFalse() {
                // given
                User user = createTestUser();
                user.promoteToOperator();

                // then
                assertThat(user.isMember()).isFalse();
            }

            @Test
            @DisplayName("ADMIN일 때 false 반환")
            void isMember_WhenAdmin_ReturnsFalse() {
                // given
                User user = createTestUser();
                user.promoteToAdmin();

                // then
                assertThat(user.isMember()).isFalse();
            }
        }

        @Nested
        @DisplayName("isAssociate")
        class IsAssociateTest {

            @Test
            @DisplayName("ASSOCIATE일 때 true 반환 (기본 생성 직후)")
            void isAssociate_WhenAssociate_ReturnsTrue() {
                // given
                User user = createTestUser();

                // then
                assertThat(user.isAssociate()).isTrue();
            }

            @Test
            @DisplayName("MEMBER일 때 false 반환")
            void isAssociate_WhenMember_ReturnsFalse() {
                // given
                User user = createTestUser();
                user.promoteToMember();

                // then
                assertThat(user.isAssociate()).isFalse();
            }

            @Test
            @DisplayName("OPERATOR일 때 false 반환")
            void isAssociate_WhenOperator_ReturnsFalse() {
                // given
                User user = createTestUser();
                user.promoteToOperator();

                // then
                assertThat(user.isAssociate()).isFalse();
            }

            @Test
            @DisplayName("ADMIN일 때 false 반환")
            void isAssociate_WhenAdmin_ReturnsFalse() {
                // given
                User user = createTestUser();
                user.promoteToAdmin();

                // then
                assertThat(user.isAssociate()).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("직책 관련 메서드")
    class PositionManagementTest {

        @Nested
        @DisplayName("addPosition")
        class AddPositionTest {

            @Test
            @DisplayName("새 직책 추가 성공")
            void addPosition_WithNewPosition_AddsPosition() {
                // given
                User user = createTestUser();
                Position position = createTestPosition("기술부");

                // when
                user.addPosition(position);

                // then
                assertThat(user.hasPosition(position)).isTrue();
                assertThat(user.getPositions()).hasSize(1);
                assertThat(user.getPositions()).contains(position);
            }

            @Test
            @DisplayName("여러 직책 추가 가능")
            void addPosition_MultiplePositions_AddsAllPositions() {
                // given
                User user = createTestUser();
                Position position1 = createTestPosition("기술부");
                Position position2 = createTestPosition("기획부");

                // when
                user.addPosition(position1);
                user.addPosition(position2);

                // then
                assertThat(user.getPositions()).hasSize(2);
                assertThat(user.getPositions()).containsExactlyInAnyOrder(position1, position2);
            }
        }

        @Test
        @DisplayName("중복 직책 추가 시 무시")
        void addPosition_DuplicatePosition_IgnoresDuplicate() {
            // given
            User user = createTestUser();
            Position position = createTestPosition("기술부");
            user.addPosition(position);

            // when
            user.addPosition(position);

            // then
            assertThat(user.getPositions()).hasSize(1);
        }

        @Nested
        @DisplayName("removePosition")
        class RemovePositionTest {

            @Test
            @DisplayName("기존 직책 제거 성공")
            void removePosition_ExistingPosition_RemovesPosition() {
                // given
                User user = createTestUser();
                Position position = createTestPosition("기술부");
                user.addPosition(position);

                // when
                user.removePosition(position);

                // then
                assertThat(user.hasPosition(position)).isFalse();
                assertThat(user.getPositions()).isEmpty();
            }

            @Test
            @DisplayName("없는 직책 제거 시 예외 없이 정상 동작")
            void removePosition_NonExistingPosition_NoException() {
                // given
                User user = createTestUser();
                Position position = createTestPosition("기술부");

                // when & then - 예외 없이 정상 동작
                user.removePosition(position);
                assertThat(user.getPositions()).isEmpty();
            }
        }

        @Nested
        @DisplayName("clearPositions")
        class ClearPositionsTest {

            @Test
            @DisplayName("모든 직책 제거 성공")
            void clearPositions_WithPositions_RemovesAllPositions() {
                // given
                User user = createTestUser();
                user.addPosition(createTestPosition("기술부"));
                user.addPosition(createTestPosition("기획부"));

                // when
                user.clearPositions();

                // then
                assertThat(user.getPositions()).isEmpty();
                assertThat(user.hasAnyPosition()).isFalse();
            }

            @Test
            @DisplayName("이미 비어있을 때도 정상 동작")
            void clearPositions_WhenEmpty_NoException() {
                // given
                User user = createTestUser();

                // when & then - 예외 없이 정상 동작
                user.clearPositions();
                assertThat(user.getPositions()).isEmpty();
            }
        }

        @Nested
        @DisplayName("hasPosition")
        class HasPositionTest {

            @Test
            @DisplayName("해당 직책 보유 시 true 반환")
            void hasPosition_WhenHasPosition_ReturnsTrue() {
                // given
                User user = createTestUser();
                Position position = createTestPosition("기술부");
                user.addPosition(position);

                // then
                assertThat(user.hasPosition(position)).isTrue();
            }

            @Test
            @DisplayName("해당 직책 미보유 시 false 반환")
            void hasPosition_WhenNotHasPosition_ReturnsFalse() {
                // given
                User user = createTestUser();
                Position position = createTestPosition("기술부");

                // then
                assertThat(user.hasPosition(position)).isFalse();
            }
        }

        @Nested
        @DisplayName("hasAnyPosition")
        class HasAnyPositionTest {

            @Test
            @DisplayName("직책이 있으면 true 반환")
            void hasAnyPosition_WhenHasPositions_ReturnsTrue() {
                // given
                User user = createTestUser();
                user.addPosition(createTestPosition("기술부"));

                // then
                assertThat(user.hasAnyPosition()).isTrue();
            }

            @Test
            @DisplayName("직책이 없으면 false 반환 (생성 직후)")
            void hasAnyPosition_WhenNoPositions_ReturnsFalse() {
                // given
                User user = createTestUser();

                // then
                assertThat(user.hasAnyPosition()).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("프로필 수정 메서드")
    class ProfileUpdateTest {

        @Test
        @DisplayName("updateProfile로 프로필 정보 수정 성공")
        void updateProfile_WithNewInfo_UpdatesProfile() {
            // given
            User user = createTestUser();
            String newName = "김철수";
            String newPhoneNumber = "010-9999-8888";
            String newDepartment = "정보통신공학과";

            // when
            user.updateProfile(newName, newPhoneNumber, newDepartment);

            // then
            assertThat(user.getName()).isEqualTo(newName);
            assertThat(user.getPhoneNumber()).isEqualTo(newPhoneNumber);
            assertThat(user.getDepartment()).isEqualTo(newDepartment);
        }

        @Test
        @DisplayName("updateEmail로 이메일 수정 성공")
        void updateEmail_WithNewEmail_UpdatesEmail() {
            // given
            User user = createTestUser();
            String newEmail = "newemail@inha.edu";

            // when
            user.updateEmail(newEmail);

            // then
            assertThat(user.getEmail()).isEqualTo(newEmail);
        }
    }
}
