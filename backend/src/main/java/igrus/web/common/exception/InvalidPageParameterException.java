package igrus.web.common.exception;

/**
 * 페이지네이션 파라미터(page, size)가 유효하지 않을 때 발생하는 예외.
 *
 * <p>page가 음수이거나 size가 0 이하일 때 400 Bad Request를 반환합니다.</p>
 */
public class InvalidPageParameterException extends CustomBaseException {

    public InvalidPageParameterException(String message) {
        super(CommonErrorCode.INVALID_INPUT_VALUE, message);
    }
}
