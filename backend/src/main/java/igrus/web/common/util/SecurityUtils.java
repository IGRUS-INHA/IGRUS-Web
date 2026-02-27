package igrus.web.common.util;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return user;
        }
        return null;
    }

    public static AuthenticatedUser requireCurrentUser() {
        AuthenticatedUser user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("No authenticated user found in SecurityContext");
        }
        return user;
    }
}
