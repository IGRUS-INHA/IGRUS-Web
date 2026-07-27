package igrus.web.security.auth.sso;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSO 일회용 코드 저장소.
 *
 * <p>코드는 60초 TTL, 1회 사용 후 즉시 폐기된다.
 */
// ponytail: 프로세스 메모리 저장 — 단일 인스턴스 배포 전제. 다중 인스턴스가 되면 DB/Redis 로 교체.
@Component
public class SsoCodeStore {

    static final Duration CODE_TTL = Duration.ofSeconds(60);

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> codes = new ConcurrentHashMap<>();

    private record Entry(Long userId, Instant expiresAt) {}

    public SsoCodeStore(Clock clock) {
        this.clock = clock;
    }

    /**
     * 사용자에 대한 일회용 코드를 발급합니다.
     */
    public String issue(Long userId) {
        byte[] buf = new byte[32];
        random.nextBytes(buf);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        codes.put(code, new Entry(userId, clock.instant().plus(CODE_TTL)));
        return code;
    }

    /**
     * 코드를 소비합니다. 유효하면 userId를 반환하고 코드를 폐기합니다.
     */
    public Optional<Long> consume(String code) {
        Instant now = clock.instant();
        codes.values().removeIf(e -> now.isAfter(e.expiresAt()));
        Entry entry = codes.remove(code);
        if (entry == null || now.isAfter(entry.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(entry.userId());
    }
}
