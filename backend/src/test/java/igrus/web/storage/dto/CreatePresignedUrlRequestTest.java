package igrus.web.storage.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CreatePresignedUrlRequest DTO Bean Validation 단위 테스트.
 *
 * <p>TC-003: 최소 유효 크기(1B) 검증</p>
 * <p>파일 크기/파일명/Content-Type 경계값 검증</p>
 */
@DisplayName("CreatePresignedUrlRequest DTO 검증 단위 테스트")
class CreatePresignedUrlRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private CreatePresignedUrlRequest validRequest() {
        return new CreatePresignedUrlRequest("test.png", "image/png", 1024L, "posts");
    }

    @Nested
    @DisplayName("파일 크기 검증")
    class FileSizeValidation {

        @DisplayName("TC-003: 최소 유효 크기(1B) 통과")
        @Test
        void fileSize_1Byte_IsValid() {
            var request = new CreatePresignedUrlRequest("test.png", "image/png", 1L, "posts");
            Set<ConstraintViolation<CreatePresignedUrlRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @DisplayName("최대 허용 크기(10MB) 통과")
        @Test
        void fileSize_10MB_IsValid() {
            var request = new CreatePresignedUrlRequest("test.png", "image/png", 10485760L, "posts");
            Set<ConstraintViolation<CreatePresignedUrlRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @DisplayName("파일 크기 0바이트 거부")
        @Test
        void fileSize_0Byte_IsInvalid() {
            var request = new CreatePresignedUrlRequest("test.png", "image/png", 0L, "posts");
            Set<ConstraintViolation<CreatePresignedUrlRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fileSize"));
        }

        @DisplayName("파일 크기 10MB+1B 거부")
        @Test
        void fileSize_10MBPlus1_IsInvalid() {
            var request = new CreatePresignedUrlRequest("test.png", "image/png", 10485761L, "posts");
            Set<ConstraintViolation<CreatePresignedUrlRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fileSize"));
        }

        @DisplayName("파일 크기 음수(-1) 거부")
        @Test
        void fileSize_Negative_IsInvalid() {
            var request = new CreatePresignedUrlRequest("test.png", "image/png", -1L, "posts");
            Set<ConstraintViolation<CreatePresignedUrlRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
        }

        @DisplayName("파일 크기 null 거부")
        @Test
        void fileSize_Null_IsInvalid() {
            var request = new CreatePresignedUrlRequest("test.png", "image/png", null, "posts");
            Set<ConstraintViolation<CreatePresignedUrlRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fileSize"));
        }
    }

    @Nested
    @DisplayName("파일명 검증")
    class FileNameValidation {

        @DisplayName("빈 문자열 파일명 거부")
        @Test
        void fileName_Empty_IsInvalid() {
            var request = new CreatePresignedUrlRequest("", "image/png", 1024L, "posts");
            Set<ConstraintViolation<CreatePresignedUrlRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fileName"));
        }

        @DisplayName("null 파일명 거부")
        @Test
        void fileName_Null_IsInvalid() {
            var request = new CreatePresignedUrlRequest(null, "image/png", 1024L, "posts");
            Set<ConstraintViolation<CreatePresignedUrlRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fileName"));
        }

        @DisplayName("공백만 파일명 거부")
        @Test
        void fileName_BlankOnly_IsInvalid() {
            var request = new CreatePresignedUrlRequest("   ", "image/png", 1024L, "posts");
            Set<ConstraintViolation<CreatePresignedUrlRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fileName"));
        }

        @DisplayName("255자 파일명 통과")
        @Test
        void fileName_255Chars_IsValid() {
            String longName = "a".repeat(255);
            var request = new CreatePresignedUrlRequest(longName, "image/png", 1024L, "posts");
            Set<ConstraintViolation<CreatePresignedUrlRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @DisplayName("256자 파일명 거부")
        @Test
        void fileName_256Chars_IsInvalid() {
            String longName = "a".repeat(256);
            var request = new CreatePresignedUrlRequest(longName, "image/png", 1024L, "posts");
            Set<ConstraintViolation<CreatePresignedUrlRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fileName"));
        }
    }

    @Nested
    @DisplayName("Content-Type 검증")
    class ContentTypeValidation {

        @DisplayName("null Content-Type 거부")
        @Test
        void contentType_Null_IsInvalid() {
            var request = new CreatePresignedUrlRequest("test.png", null, 1024L, "posts");
            Set<ConstraintViolation<CreatePresignedUrlRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("contentType"));
        }

        @DisplayName("빈 문자열 Content-Type 거부")
        @Test
        void contentType_Empty_IsInvalid() {
            var request = new CreatePresignedUrlRequest("test.png", "", 1024L, "posts");
            Set<ConstraintViolation<CreatePresignedUrlRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("contentType"));
        }
    }

    @Nested
    @DisplayName("유효한 요청 검증")
    class ValidRequest {

        @DisplayName("모든 필드가 유효한 요청 통과")
        @Test
        void allFieldsValid_NoViolations() {
            Set<ConstraintViolation<CreatePresignedUrlRequest>> violations = validator.validate(validRequest());
            assertThat(violations).isEmpty();
        }
    }
}
