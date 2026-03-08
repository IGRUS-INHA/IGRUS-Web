package igrus.web.event.dto;

import igrus.web.generated.model.ApiExternalRegisterEventRequest;
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
 * ExternalRegisterEventRequest (OpenAPI 생성 모델) Bean Validation 단위 테스트.
 * 테스트 케이스 문서: docs/test-case/event/external-event-registration-test-cases.md
 * 검증 기준: EXT-INV-10 (필수 필드 검증), Section 4-1 (BVA)
 *
 * @see igrus.web.generated.model.ApiExternalRegisterEventRequest
 */
@DisplayName("ExternalRegisterEventRequest Bean Validation")
class ExternalRegisterEventRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private ApiExternalRegisterEventRequest validRequest() {
        return new ApiExternalRegisterEventRequest("홍길동", "12345678", "01012345678", "컴퓨터공학과");
    }

    // ===== TC-022: 모든 필수 필드 유효값 =====

    @Test
    @DisplayName("[TC-022] 모든 필수 필드를 유효한 값으로 제공 시 Validation 통과")
    void allFieldsValid_PassesValidation() {
        ApiExternalRegisterEventRequest request = validRequest();
        Set<ConstraintViolation<ApiExternalRegisterEventRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    // ===== name 필드 (1-50자) =====

    @Nested
    @DisplayName("name 필드 검증")
    class NameValidation {

        @Test
        @DisplayName("[TC-023] name이 null인 경우 Validation 위반")
        void name_Null_IsInvalid() {
            ApiExternalRegisterEventRequest request = validRequest();
            request.setName(null);
            Set<ConstraintViolation<ApiExternalRegisterEventRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("[TC-041] name 1자 (최소 경계) 정상")
        void name_1Char_IsValid() {
            ApiExternalRegisterEventRequest request = validRequest();
            request.setName("김");
            Set<ConstraintViolation<ApiExternalRegisterEventRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("[TC-042] name 50자 (최대 경계) 정상")
        void name_50Chars_IsValid() {
            ApiExternalRegisterEventRequest request = validRequest();
            request.setName("가".repeat(50));
            Set<ConstraintViolation<ApiExternalRegisterEventRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("[TC-043] name 51자 (상한 초과) Validation 위반")
        void name_51Chars_IsInvalid() {
            ApiExternalRegisterEventRequest request = validRequest();
            request.setName("가".repeat(51));
            Set<ConstraintViolation<ApiExternalRegisterEventRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
        }
    }

    // ===== studentId 필드 (1-20자) =====

    @Nested
    @DisplayName("studentId 필드 검증")
    class StudentIdValidation {

        @Test
        @DisplayName("[TC-024] studentId가 빈 문자열인 경우 Validation 위반")
        void studentId_Empty_IsInvalid() {
            ApiExternalRegisterEventRequest request = validRequest();
            request.setStudentId("");
            Set<ConstraintViolation<ApiExternalRegisterEventRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("[TC-044] studentId 1자 (최소 경계) 정상")
        void studentId_1Char_IsValid() {
            ApiExternalRegisterEventRequest request = validRequest();
            request.setStudentId("1");
            Set<ConstraintViolation<ApiExternalRegisterEventRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("[TC-045] studentId 20자 (최대 경계) 정상")
        void studentId_20Chars_IsValid() {
            ApiExternalRegisterEventRequest request = validRequest();
            request.setStudentId("12345678901234567890");
            Set<ConstraintViolation<ApiExternalRegisterEventRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("[TC-046] studentId 21자 (상한 초과) Validation 위반")
        void studentId_21Chars_IsInvalid() {
            ApiExternalRegisterEventRequest request = validRequest();
            request.setStudentId("123456789012345678901");
            Set<ConstraintViolation<ApiExternalRegisterEventRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
        }
    }

    // ===== phone 필드 (1-20자) =====

    @Nested
    @DisplayName("phone 필드 검증")
    class PhoneValidation {

        @Test
        @DisplayName("[TC-025] phone이 null인 경우 Validation 위반")
        void phone_Null_IsInvalid() {
            ApiExternalRegisterEventRequest request = validRequest();
            request.setPhone(null);
            Set<ConstraintViolation<ApiExternalRegisterEventRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("[TC-047] phone 1자 (최소 경계) 정상")
        void phone_1Char_IsValid() {
            ApiExternalRegisterEventRequest request = validRequest();
            request.setPhone("1");
            Set<ConstraintViolation<ApiExternalRegisterEventRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("[TC-048] phone 20자 (최대 경계) 정상")
        void phone_20Chars_IsValid() {
            ApiExternalRegisterEventRequest request = validRequest();
            request.setPhone("01012345678901234567");
            Set<ConstraintViolation<ApiExternalRegisterEventRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("[TC-049] phone 21자 (상한 초과) Validation 위반")
        void phone_21Chars_IsInvalid() {
            ApiExternalRegisterEventRequest request = validRequest();
            request.setPhone("010123456789012345678");
            Set<ConstraintViolation<ApiExternalRegisterEventRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
        }
    }

    // ===== department 필드 (1-100자) =====

    @Nested
    @DisplayName("department 필드 검증")
    class DepartmentValidation {

        @Test
        @DisplayName("[TC-026] department가 null인 경우 Validation 위반")
        void department_Null_IsInvalid() {
            ApiExternalRegisterEventRequest request = validRequest();
            request.setDepartment(null);
            Set<ConstraintViolation<ApiExternalRegisterEventRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("[TC-050] department 1자 (최소 경계) 정상")
        void department_1Char_IsValid() {
            ApiExternalRegisterEventRequest request = validRequest();
            request.setDepartment("공");
            Set<ConstraintViolation<ApiExternalRegisterEventRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("[TC-051] department 100자 (최대 경계) 정상")
        void department_100Chars_IsValid() {
            ApiExternalRegisterEventRequest request = validRequest();
            request.setDepartment("가".repeat(100));
            Set<ConstraintViolation<ApiExternalRegisterEventRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("[TC-052] department 101자 (상한 초과) Validation 위반")
        void department_101Chars_IsInvalid() {
            ApiExternalRegisterEventRequest request = validRequest();
            request.setDepartment("가".repeat(101));
            Set<ConstraintViolation<ApiExternalRegisterEventRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
        }
    }
}
