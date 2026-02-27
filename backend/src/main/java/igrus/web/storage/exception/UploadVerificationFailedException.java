package igrus.web.storage.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 업로드 검증에 실패했을 때 발생하는 예외.
 */
public class UploadVerificationFailedException extends CustomBaseException {

    public UploadVerificationFailedException() {
        super(StorageErrorCode.UPLOAD_VERIFICATION_FAILED);
    }

    public UploadVerificationFailedException(String reason) {
        super(StorageErrorCode.UPLOAD_VERIFICATION_FAILED,
                "업로드 검증에 실패했습니다: " + reason);
    }
}
