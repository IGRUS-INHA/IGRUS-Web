package igrus.web.common.exception;

public interface ErrorCode {

    int getStatus();

    String getMessage();

    String getCode();
}
