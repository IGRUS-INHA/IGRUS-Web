package igrus.web.event.exception;

/**
 * 잘못된 행사 날짜 설정 시 발생하는 예외.
 */
public class InvalidEventDateException extends RuntimeException {

    public InvalidEventDateException(String message) {
        super(message);
    }
}
