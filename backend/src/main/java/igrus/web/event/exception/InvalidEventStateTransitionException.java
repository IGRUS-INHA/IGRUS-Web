package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.event.domain.EventStatus;
import igrus.web.event.domain.RegistrationStatus;
import lombok.Getter;

/**
 * 잘못된 행사 상태 전이 시 발생하는 예외.
 * EventStatus 및 RegistrationStatus 양쪽 전이 오류를 처리합니다.
 */
@Getter
public class InvalidEventStateTransitionException extends CustomBaseException {

    private final String fromState;
    private final String toState;

    public InvalidEventStateTransitionException(EventStatus from, EventStatus to) {
        super(EventErrorCode.EVENT_INVALID_STATE_TRANSITION,
                String.format("행사 상태를 %s에서 %s로 변경할 수 없습니다.", from.getDisplayName(), to.getDisplayName()));
        this.fromState = from.name();
        this.toState = to.name();
    }

    public InvalidEventStateTransitionException(RegistrationStatus from, RegistrationStatus to) {
        super(EventErrorCode.EVENT_INVALID_STATE_TRANSITION,
                String.format("등록 상태를 %s에서 %s로 변경할 수 없습니다.", from.getDisplayName(), to.getDisplayName()));
        this.fromState = from.name();
        this.toState = to.name();
    }
}
