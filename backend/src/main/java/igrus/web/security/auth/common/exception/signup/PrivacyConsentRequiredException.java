package igrus.web.security.auth.common.exception.signup;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class PrivacyConsentRequiredException extends CustomBaseException {
    public PrivacyConsentRequiredException() {
        super(AuthErrorCode.PRIVACY_CONSENT_REQUIRED);
    }
}
