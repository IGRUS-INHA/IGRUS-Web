package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 신청하려는 행사의 시간이 이미 신청한 다른 행사와 겹치는 경우 발생하는 예외.
 */
public class EventTimeOverlapException extends CustomBaseException {

    public EventTimeOverlapException() {
        super(EventErrorCode.EVENT_TIME_OVERLAP);
    }
}
