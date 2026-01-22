package igrus.web.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof String && "anonymousUser".equals(principal)) {
            return Optional.empty();
        }

        // TODO: Spring Security 인증 구현 후 실제 사용자 ID 반환하도록 수정
        // 현재는 인증 시스템 구현 전이므로 empty 반환
        return Optional.empty();
    }
}
