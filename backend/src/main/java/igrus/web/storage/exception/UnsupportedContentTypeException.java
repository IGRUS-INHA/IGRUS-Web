package igrus.web.storage.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 지원하지 않는 파일 형식일 때 발생하는 예외.
 */
public class UnsupportedContentTypeException extends CustomBaseException {

    public UnsupportedContentTypeException() {
        super(StorageErrorCode.UNSUPPORTED_CONTENT_TYPE);
    }

    public UnsupportedContentTypeException(String contentType) {
        super(StorageErrorCode.UNSUPPORTED_CONTENT_TYPE,
                "지원하지 않는 파일 형식입니다: " + contentType);
    }
}
