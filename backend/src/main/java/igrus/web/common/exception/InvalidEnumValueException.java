package igrus.web.common.exception;

/**
 * String에서 enum으로 변환 시 유효하지 않은 값이 전달되었을 때 발생하는 예외.
 *
 * <p>contract-first 방식에서 OpenAPI 생성 코드가 enum 쿼리 파라미터를 String으로 받는 경우 사용됩니다.
 * Spring의 {@code MethodArgumentTypeMismatchException}과 동일하게 400 Bad Request를 반환합니다.</p>
 */
public class InvalidEnumValueException extends CustomBaseException {

    public InvalidEnumValueException() {
        super(CommonErrorCode.INVALID_TYPE_VALUE);
    }

    public InvalidEnumValueException(String fieldName, String rejectedValue) {
        super(CommonErrorCode.INVALID_TYPE_VALUE,
                String.format("유효하지 않은 값입니다: field='%s', value='%s'", fieldName, rejectedValue));
    }
}
