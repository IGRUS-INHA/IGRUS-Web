package igrus.web.event.exception;

import igrus.web.event.domain.EventStatus;

/**
 * 잘못된 행사 상태 전이 시 발생하는 예외.
 */
public class    InvalidEventStateTransitionException extends RuntimeException {

    private final EventStatus from;
    private final EventStatus to;

    public InvalidEventStateTransitionException(EventStatus from, EventStatus to) {
        super(String.format("행사 상태를 %s에서 %s로 변경할 수 없습니다.", from.getDisplayName(), to.getDisplayName()));
        this.from = from;
        this.to = to;
    }

    public EventStatus getFrom() {
        return from;
    }

    public EventStatus getTo() {
        return to;
    }
}
