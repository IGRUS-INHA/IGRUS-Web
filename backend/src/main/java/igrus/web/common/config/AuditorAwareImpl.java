package igrus.web.common.config;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<Long> {

    @Override
    @NonNull
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if ("anonymousUser".equals(principal)) {
            return Optional.empty();
        }

        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return Optional.of(authenticatedUser.userId());
        }

        return Optional.empty();
    }
}
