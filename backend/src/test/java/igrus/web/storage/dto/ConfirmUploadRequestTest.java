package igrus.web.storage.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConfirmUploadRequest DTO Bean Validation 단위 테스트.
 */
@DisplayName("ConfirmUploadRequest DTO 검증 단위 테스트")
class ConfirmUploadRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @DisplayName("유효한 objectKey 통과")
    @Test
    void objectKey_Valid_NoViolations() {
        var request = new ConfirmUploadRequest("posts/2026/02/26/uuid.png");
        Set<ConstraintViolation<ConfirmUploadRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @DisplayName("빈 문자열 objectKey 거부")
    @Test
    void objectKey_Empty_IsInvalid() {
        var request = new ConfirmUploadRequest("");
        Set<ConstraintViolation<ConfirmUploadRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("objectKey"));
    }

    @DisplayName("null objectKey 거부")
    @Test
    void objectKey_Null_IsInvalid() {
        var request = new ConfirmUploadRequest(null);
        Set<ConstraintViolation<ConfirmUploadRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("objectKey"));
    }

    @DisplayName("공백만 objectKey 거부")
    @Test
    void objectKey_BlankOnly_IsInvalid() {
        var request = new ConfirmUploadRequest("   ");
        Set<ConstraintViolation<ConfirmUploadRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("objectKey"));
    }
}
