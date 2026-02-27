package igrus.web.storage.exception;

import igrus.web.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum StorageErrorCode implements ErrorCode {

    FILE_SIZE_EXCEEDED(400, "파일 크기가 최대 허용 크기를 초과합니다"),
    UNSUPPORTED_CONTENT_TYPE(400, "지원하지 않는 파일 형식입니다"),
    FILE_METADATA_NOT_FOUND(404, "파일 메타데이터를 찾을 수 없습니다"),
    INVALID_FILE_STATUS_TRANSITION(400, "허용되지 않는 상태 전이입니다"),
    FILE_OWNERSHIP_MISMATCH(403, "파일 소유권이 일치하지 않습니다"),
    S3_OPERATION_FAILED(500, "S3 작업 중 오류가 발생했습니다"),
    FILE_REFERENCE_EXISTS(409, "참조 중인 파일은 삭제할 수 없습니다"),
    UPLOAD_VERIFICATION_FAILED(400, "업로드 검증에 실패했습니다");

    private final int status;
    private final String message;

    StorageErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
