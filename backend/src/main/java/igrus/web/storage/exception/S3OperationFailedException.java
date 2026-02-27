package igrus.web.storage.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * S3 작업 중 오류가 발생했을 때 발생하는 예외.
 */
public class S3OperationFailedException extends CustomBaseException {

    public S3OperationFailedException() {
        super(StorageErrorCode.S3_OPERATION_FAILED);
    }

    public S3OperationFailedException(Throwable cause) {
        super(StorageErrorCode.S3_OPERATION_FAILED, cause);
    }

    public S3OperationFailedException(String message) {
        super(StorageErrorCode.S3_OPERATION_FAILED, "S3 작업 중 오류가 발생했습니다: " + message);
    }
}
