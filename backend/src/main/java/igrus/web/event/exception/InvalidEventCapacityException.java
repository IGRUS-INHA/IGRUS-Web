package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;
import lombok.Getter;

/**
 * 잘못된 행사 정원 설정 시 발생하는 예외.
 */
@Getter
public class InvalidEventCapacityException extends CustomBaseException {

    private final Integer capacity;

    public InvalidEventCapacityException(Integer capacity) {
        super(ErrorCode.EVENT_INVALID_CAPACITY,
                String.format("행사 정원은 1명 이상이어야 합니다. 입력값: %d", capacity));
        this.capacity = capacity;
    }
}
