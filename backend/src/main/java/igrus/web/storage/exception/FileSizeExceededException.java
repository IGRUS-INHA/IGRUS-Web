package igrus.web.storage.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 파일 크기가 최대 허용 크기를 초과했을 때 발생하는 예외.
 */
public class FileSizeExceededException extends CustomBaseException {

    public FileSizeExceededException() {
        super(StorageErrorCode.FILE_SIZE_EXCEEDED);
    }

    public FileSizeExceededException(long fileSize, long maxSize) {
        super(StorageErrorCode.FILE_SIZE_EXCEEDED,
                "파일 크기가 최대 허용 크기를 초과합니다: " + fileSize + " bytes (최대: " + maxSize + " bytes)");
    }
}
