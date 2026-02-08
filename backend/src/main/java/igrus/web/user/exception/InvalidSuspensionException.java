package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 사용자 정지 관련 유효성 검증 예외.
 */
public class InvalidSuspensionException extends CustomBaseException {

    public InvalidSuspensionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidSuspensionException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidSuspensionException invalidPeriod() {
        return new InvalidSuspensionException(ErrorCode.SUSPENSION_INVALID_PERIOD);
    }

    public static InvalidSuspensionException alreadyLifted() {
        return new InvalidSuspensionException(ErrorCode.SUSPENSION_ALREADY_LIFTED);
    }

    public static InvalidSuspensionException reasonRequired() {
        return new InvalidSuspensionException(ErrorCode.SUSPENSION_REASON_REQUIRED);
    }

    public static InvalidSuspensionException cannotExtend() {
        return new InvalidSuspensionException(ErrorCode.SUSPENSION_CANNOT_EXTEND);
    }

    public static InvalidSuspensionException extendInvalidDate() {
        return new InvalidSuspensionException(ErrorCode.SUSPENSION_EXTEND_INVALID_DATE);
    }

    public static InvalidSuspensionException endDateMustBeFuture() {
        return new InvalidSuspensionException(ErrorCode.SUSPENSION_END_DATE_MUST_BE_FUTURE);
    }

    public static InvalidSuspensionException lastAdminCannotSuspend() {
        return new InvalidSuspensionException(ErrorCode.LAST_ADMIN_CANNOT_SUSPEND);
    }
}
