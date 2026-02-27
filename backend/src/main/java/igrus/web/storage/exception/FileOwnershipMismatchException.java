package igrus.web.storage.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 파일 소유권이 일치하지 않을 때 발생하는 예외.
 */
public class FileOwnershipMismatchException extends CustomBaseException {

    public FileOwnershipMismatchException() {
        super(StorageErrorCode.FILE_OWNERSHIP_MISMATCH);
    }

    public FileOwnershipMismatchException(String objectKey) {
        super(StorageErrorCode.FILE_OWNERSHIP_MISMATCH,
                "파일 소유권이 일치하지 않습니다: " + objectKey);
    }
}
