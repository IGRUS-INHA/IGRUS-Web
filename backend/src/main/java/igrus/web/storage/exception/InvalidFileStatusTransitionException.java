package igrus.web.storage.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 허용되지 않는 파일 상태 전이를 시도할 때 발생하는 예외.
 */
public class InvalidFileStatusTransitionException extends CustomBaseException {

    public InvalidFileStatusTransitionException() {
        super(StorageErrorCode.INVALID_FILE_STATUS_TRANSITION);
    }

    public InvalidFileStatusTransitionException(String currentStatus, String targetStatus) {
        super(StorageErrorCode.INVALID_FILE_STATUS_TRANSITION,
                "허용되지 않는 상태 전이입니다: " + currentStatus + " -> " + targetStatus);
    }
}
