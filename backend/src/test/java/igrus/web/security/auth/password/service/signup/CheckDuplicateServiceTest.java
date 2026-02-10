package igrus.web.security.auth.password.service.signup;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.exception.signup.DuplicateEmailException;
import igrus.web.security.auth.common.exception.signup.DuplicateStudentIdException;
import igrus.web.security.auth.password.dto.response.DuplicateCheckResponse;
import igrus.web.user.domain.UserRole;
import igrus.web.user.exception.InvalidEmailException;
import igrus.web.user.exception.InvalidStudentIdException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CheckDuplicateService 통합 테스트")
class CheckDuplicateServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private CheckDuplicateService checkDuplicateService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Nested
    @DisplayName("학번 중복 체크")
    class CheckStudentIdTest {

        @Test
        @DisplayName("사용 가능한 학번이면 available=true 응답")
        void checkStudentId_WhenAvailable_ReturnsAvailable() {
            // given
            String studentId = "20231234";

            // when
            DuplicateCheckResponse response = checkDuplicateService.checkStudentId(studentId);

            // then
            assertThat(response.available()).isTrue();
            assertThat(response.message()).isNotBlank();
        }

        @Test
        @DisplayName("이미 가입된 학번이면 DuplicateStudentIdException 발생")
        void checkStudentId_WhenDuplicate_ThrowsDuplicateException() {
            // given
            createAndSaveUser("20231234", "existing@inha.edu", UserRole.ASSOCIATE);

            // when & then
            assertThatThrownBy(() -> checkDuplicateService.checkStudentId("20231234"))
                    .isInstanceOf(DuplicateStudentIdException.class);
        }

        @Test
        @DisplayName("학번이 8자리 숫자가 아니면 InvalidStudentIdException 발생")
        void checkStudentId_WhenInvalidFormat_ThrowsInvalidException() {
            assertThatThrownBy(() -> checkDuplicateService.checkStudentId("1234"))
                    .isInstanceOf(InvalidStudentIdException.class);
        }

        @Test
        @DisplayName("학번이 null이면 InvalidStudentIdException 발생")
        void checkStudentId_WhenNull_ThrowsInvalidException() {
            assertThatThrownBy(() -> checkDuplicateService.checkStudentId(null))
                    .isInstanceOf(InvalidStudentIdException.class);
        }

        @Test
        @DisplayName("학번에 문자가 포함되면 InvalidStudentIdException 발생")
        void checkStudentId_WhenContainsLetters_ThrowsInvalidException() {
            assertThatThrownBy(() -> checkDuplicateService.checkStudentId("2023abcd"))
                    .isInstanceOf(InvalidStudentIdException.class);
        }
    }

    @Nested
    @DisplayName("이메일 중복 체크")
    class CheckEmailTest {

        @Test
        @DisplayName("사용 가능한 이메일이면 available=true 응답")
        void checkEmail_WhenAvailable_ReturnsAvailable() {
            // given
            String email = "newuser@inha.edu";

            // when
            DuplicateCheckResponse response = checkDuplicateService.checkEmail(email);

            // then
            assertThat(response.available()).isTrue();
            assertThat(response.message()).isNotBlank();
        }

        @Test
        @DisplayName("이미 존재하는 이메일이면 DuplicateEmailException 발생")
        void checkEmail_WhenDuplicate_ThrowsDuplicateException() {
            // given
            createAndSaveUser("20231234", "existing@inha.edu", UserRole.ASSOCIATE);

            // when & then
            assertThatThrownBy(() -> checkDuplicateService.checkEmail("existing@inha.edu"))
                    .isInstanceOf(DuplicateEmailException.class);
        }

        @Test
        @DisplayName("이메일 형식이 올바르지 않으면 InvalidEmailException 발생")
        void checkEmail_WhenInvalidFormat_ThrowsInvalidException() {
            assertThatThrownBy(() -> checkDuplicateService.checkEmail("invalid-email"))
                    .isInstanceOf(InvalidEmailException.class);
        }

        @Test
        @DisplayName("이메일이 null이면 InvalidEmailException 발생")
        void checkEmail_WhenNull_ThrowsInvalidException() {
            assertThatThrownBy(() -> checkDuplicateService.checkEmail(null))
                    .isInstanceOf(InvalidEmailException.class);
        }
    }
}
