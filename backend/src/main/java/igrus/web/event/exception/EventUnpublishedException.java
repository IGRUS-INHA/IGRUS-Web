package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

public class EventUnpublishedException extends CustomBaseException {

    public EventUnpublishedException() {
        super(EventErrorCode.EVENT_UNPUBLISHED);
    }

    public EventUnpublishedException(Long eventId) {
        super(EventErrorCode.EVENT_UNPUBLISHED, "비공개인 행사는 신청할 수 없습니다: id=" + eventId);
    }
}
