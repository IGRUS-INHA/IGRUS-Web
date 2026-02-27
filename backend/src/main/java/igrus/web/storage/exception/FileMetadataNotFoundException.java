package igrus.web.storage.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 파일 메타데이터를 찾을 수 없을 때 발생하는 예외.
 */
public class FileMetadataNotFoundException extends CustomBaseException {

    public FileMetadataNotFoundException() {
        super(StorageErrorCode.FILE_METADATA_NOT_FOUND);
    }

    public FileMetadataNotFoundException(String objectKey) {
        super(StorageErrorCode.FILE_METADATA_NOT_FOUND,
                "파일 메타데이터를 찾을 수 없습니다: " + objectKey);
    }
}
