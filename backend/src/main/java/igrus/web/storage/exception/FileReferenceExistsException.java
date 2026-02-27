package igrus.web.storage.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 참조 중인 파일을 삭제하려 할 때 발생하는 예외.
 */
public class FileReferenceExistsException extends CustomBaseException {

    public FileReferenceExistsException() {
        super(StorageErrorCode.FILE_REFERENCE_EXISTS);
    }

    public FileReferenceExistsException(String objectKey) {
        super(StorageErrorCode.FILE_REFERENCE_EXISTS,
                "참조 중인 파일은 삭제할 수 없습니다: " + objectKey);
    }
}
