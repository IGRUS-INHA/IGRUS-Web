package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 비인증 사용자가 allowExternal이 false인 행사에 접근할 때 발생하는 예외.
 */
public class EventAuthenticationRequiredException extends CustomBaseException {

    public EventAuthenticationRequiredException() {
        super(EventErrorCode.EVENT_AUTHENTICATION_REQUIRED);
    }
}
