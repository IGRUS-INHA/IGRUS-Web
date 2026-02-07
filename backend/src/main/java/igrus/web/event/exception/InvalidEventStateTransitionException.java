package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;
import igrus.web.event.domain.EventStatus;
import lombok.Getter;

/**
 * 잘못된 행사 상태 전이 시 발생하는 예외.
 */
@Getter
public class InvalidEventStateTransitionException extends CustomBaseException {

    private final EventStatus from;
    private final EventStatus to;

    public InvalidEventStateTransitionException(EventStatus from, EventStatus to) {
        super(ErrorCode.EVENT_INVALID_STATE_TRANSITION,
                String.format("행사 상태를 %s에서 %s로 변경할 수 없습니다.", from.getDisplayName(), to.getDisplayName()));
        this.from = from;
        this.to = to;
    }
}
