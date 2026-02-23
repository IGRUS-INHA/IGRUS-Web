package igrus.web.security.auth.common.exception.email;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;
import lombok.Getter;

@Getter
public class EmailNotVerifiedException extends CustomBaseException {

    private final String email;

    public EmailNotVerifiedException(String email) {
        super(AuthErrorCode.EMAIL_NOT_VERIFIED);
        this.email = email;
    }
}
