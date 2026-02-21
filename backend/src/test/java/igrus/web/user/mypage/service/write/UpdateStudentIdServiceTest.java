package igrus.web.user.mypage.service.write;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.exception.signup.DuplicateStudentIdException;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.user.domain.*;
import igrus.web.user.exception.InvalidStudentIdException;
import igrus.web.user.exception.StudentIdNotTemporaryException;
import igrus.web.user.mypage.dto.request.UpdateStudentIdRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UpdateStudentIdService 통합 테스트")
class UpdateStudentIdServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private UpdateStudentIdService updateStudentIdService;

    private static final String TEMP_STUDENT_ID = "99260001";
    private static final String NEW_STUDENT_ID = "12345678";
    private static final String RAW_PASSWORD = "testpass1";

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    private User createTempStudentIdUser() {
        User user = User.createWithTemporaryStudentId(
                TEMP_STUDENT_ID, "신입생", "newbie@inha.edu", "010-9876-5432",
                "컴퓨터공학과", "동기", List.of(), Gender.MALE, 1,
                EnrollmentStatus.ENROLLED, List.of(), null, null, null
        );
        return userRepository.save(user);
    }

    private void createPasswordCredential(User user) {
        String encodedPassword = passwordEncoder.encode(RAW_PASSWORD);
        PasswordCredential credential = PasswordCredential.create(user, encodedPassword);
        passwordCredentialRepository.save(credential);
    }

    @Nested
    @DisplayName("학번 변경 - 정상 케이스")
    class UpdateSuccessTest {

        @Test
        @DisplayName("정상 학번 변경 [TEMP-INV-05~08, 10]")
        void update_ValidRequest_UpdatesStudentId() {
            // given
            User user = createTempStudentIdUser();
            createPasswordCredential(user);
            UpdateStudentIdRequest request = new UpdateStudentIdRequest(RAW_PASSWORD, NEW_STUDENT_ID);

            // when
            updateStudentIdService.updateStudentId(user.getId(), request);

            // then
            User updatedUser = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updatedUser.getStudentId()).isEqualTo(NEW_STUDENT_ID);
        }

        @Test
        @DisplayName("변경 후 hasTemporaryStudentId 플래그 해제 [TEMP-INV-05]")
        void update_ValidRequest_ClearsTemporaryFlag() {
            // given
            User user = createTempStudentIdUser();
            createPasswordCredential(user);
            assertThat(user.isHasTemporaryStudentId()).isTrue();

            UpdateStudentIdRequest request = new UpdateStudentIdRequest(RAW_PASSWORD, NEW_STUDENT_ID);

            // when
            updateStudentIdService.updateStudentId(user.getId(), request);

            // then
            User updatedUser = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updatedUser.isHasTemporaryStudentId()).isFalse();
        }
    }

    @Nested
    @DisplayName("학번 변경 - 예외 케이스")
    class UpdateExceptionTest {

        @Test
        @DisplayName("임시 학번 아닌 사용자 거부 [TEMP-INV-07]")
        void update_NotTemporary_ThrowsException() {
            // given - 일반 사용자 (hasTemporaryStudentId = false)
            User user = createAndSaveUser("20231234", "normal@inha.edu", UserRole.MEMBER);
            createPasswordCredential(user);
            UpdateStudentIdRequest request = new UpdateStudentIdRequest(RAW_PASSWORD, NEW_STUDENT_ID);

            // when & then
            assertThatThrownBy(() -> updateStudentIdService.updateStudentId(user.getId(), request))
                    .isInstanceOf(StudentIdNotTemporaryException.class);
        }

        @Test
        @DisplayName("99 접두사 학번 거부 [TEMP-INV-06]")
        void update_StartsWithNinetyNine_ThrowsException() {
            // given
            User user = createTempStudentIdUser();
            createPasswordCredential(user);
            UpdateStudentIdRequest request = new UpdateStudentIdRequest(RAW_PASSWORD, "99123456");

            // when & then
            assertThatThrownBy(() -> updateStudentIdService.updateStudentId(user.getId(), request))
                    .isInstanceOf(InvalidStudentIdException.class);
        }

        @Test
        @DisplayName("중복 학번 거부 [TEMP-INV-08]")
        void update_DuplicateStudentId_ThrowsException() {
            // given
            createAndSaveUser(NEW_STUDENT_ID, "existing@inha.edu", UserRole.MEMBER);
            User user = createTempStudentIdUser();
            createPasswordCredential(user);
            UpdateStudentIdRequest request = new UpdateStudentIdRequest(RAW_PASSWORD, NEW_STUDENT_ID);

            // when & then
            assertThatThrownBy(() -> updateStudentIdService.updateStudentId(user.getId(), request))
                    .isInstanceOf(DuplicateStudentIdException.class);
        }

        @Test
        @DisplayName("비밀번호 불일치 거부 [TEMP-INV-10]")
        void update_WrongPassword_ThrowsException() {
            // given
            User user = createTempStudentIdUser();
            createPasswordCredential(user);
            UpdateStudentIdRequest request = new UpdateStudentIdRequest("wrongpassword1", NEW_STUDENT_ID);

            // when & then
            assertThatThrownBy(() -> updateStudentIdService.updateStudentId(user.getId(), request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }
}
