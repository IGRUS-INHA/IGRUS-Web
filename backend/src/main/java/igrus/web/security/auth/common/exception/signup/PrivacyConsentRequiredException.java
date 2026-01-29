package igrus.web.security.auth.common.exception.signup;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class PrivacyConsentRequiredException extends CustomBaseException {
    public PrivacyConsentRequiredException() {
        super(ErrorCode.PRIVACY_CONSENT_REQUIRED);
    }
}
