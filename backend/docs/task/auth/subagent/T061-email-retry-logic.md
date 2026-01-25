# T061: 이메일 발송 실패 시 재시도 로직 구현

## 개요

이메일 발송 실패 시 자동으로 재시도하여 일시적인 네트워크 문제나 메일 서버 장애에 대응합니다.

## 구현 목표

- 이메일 발송 실패 시 지수 백오프(Exponential Backoff) 재시도
- 재시도 간격: 1분 → 5분 → 15분 (총 3회)
- 비동기 처리로 사용자 응답 지연 방지
- 최종 실패 시 로깅 및 알림

## 구현 방식 선택

### Option A: Spring Retry 사용 (권장)
- 간단하고 선언적인 방식
- `@Retryable` 어노테이션 활용

### Option B: 수동 재시도 큐 구현
- 더 세밀한 제어 가능
- 메시지 큐(Redis, RabbitMQ) 활용

**선택: Option A (Spring Retry)** - 프로젝트 규모에 적합

## 구현 상세

### 1. 의존성 추가

**파일 경로**: `backend/build.gradle`

```gradle
dependencies {
    implementation 'org.springframework.retry:spring-retry'
    implementation 'org.springframework:spring-aspects'
}
```

### 2. Retry 설정 활성화

**파일 경로**: `backend/src/main/java/igrus/web/config/RetryConfig.java`

```java
@Configuration
@EnableRetry
public class RetryConfig {
}
```

### 3. EmailService 인터페이스 수정

**파일 경로**: `backend/src/main/java/igrus/web/security/auth/common/service/EmailService.java`

```java
public interface EmailService {

    /**
     * 이메일 발송 (재시도 없음, 동기)
     */
    void sendEmail(String to, String subject, String body);

    /**
     * 이메일 발송 (재시도 포함, 비동기)
     */
    void sendEmailWithRetry(String to, String subject, String body);

    /**
     * 인증 코드 이메일 발송 (재시도 포함)
     */
    void sendVerificationCodeWithRetry(String to, String code);

    /**
     * 비밀번호 재설정 이메일 발송 (재시도 포함)
     */
    void sendPasswordResetWithRetry(String to, String resetLink);
}
```

### 4. SmtpEmailService 수정

**파일 경로**: `backend/src/main/java/igrus/web/security/auth/common/service/SmtpEmailService.java`

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Profile("!local & !test")
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
            log.info("이메일 발송 성공: to={}", to);
        } catch (Exception e) {
            log.error("이메일 발송 실패: to={}, error={}", to, e.getMessage());
            throw new EmailSendFailedException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    @Async
    @Retryable(
        retryFor = EmailSendFailedException.class,
        maxAttempts = 4,  // 최초 시도 1회 + 재시도 3회
        backoff = @Backoff(
            delay = 60000,      // 1분
            multiplier = 3,     // 1분 → 3분 → 9분 (또는 커스텀)
            maxDelay = 900000   // 최대 15분
        )
    )
    @Override
    public void sendEmailWithRetry(String to, String subject, String body) {
        log.debug("이메일 발송 시도 (재시도 포함): to={}", to);
        sendEmail(to, subject, body);
    }

    @Recover
    public void recoverSendEmail(EmailSendFailedException e, String to, String subject, String body) {
        log.error("이메일 발송 최종 실패 (재시도 소진): to={}, subject={}", to, subject);
        // 추가 알림 로직 (관리자 알림, 실패 기록 등)
    }

    @Async
    @Retryable(
        retryFor = EmailSendFailedException.class,
        maxAttempts = 4,
        backoff = @Backoff(delay = 60000, multiplier = 3, maxDelay = 900000)
    )
    @Override
    public void sendVerificationCodeWithRetry(String to, String code) {
        String subject = "[IGRUS] 이메일 인증 코드";
        String body = buildVerificationEmailBody(code);
        sendEmail(to, subject, body);
    }

    @Async
    @Retryable(
        retryFor = EmailSendFailedException.class,
        maxAttempts = 4,
        backoff = @Backoff(delay = 60000, multiplier = 3, maxDelay = 900000)
    )
    @Override
    public void sendPasswordResetWithRetry(String to, String resetLink) {
        String subject = "[IGRUS] 비밀번호 재설정";
        String body = buildPasswordResetEmailBody(resetLink);
        sendEmail(to, subject, body);
    }

    private String buildVerificationEmailBody(String code) {
        // 기존 이메일 템플릿 활용
        return String.format("인증 코드: %s (10분간 유효)", code);
    }

    private String buildPasswordResetEmailBody(String resetLink) {
        return String.format("비밀번호 재설정 링크: %s (30분간 유효)", resetLink);
    }
}
```

### 5. Async 설정 추가

**파일 경로**: `backend/src/main/java/igrus/web/config/AsyncConfig.java`

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-");
        executor.initialize();
        return executor;
    }
}
```

### 6. 기존 서비스 코드 수정

회원가입, 비밀번호 재설정 등에서 재시도 버전 메서드 호출로 변경:

```java
// 기존
emailService.sendVerificationCode(email, code);

// 변경
emailService.sendVerificationCodeWithRetry(email, code);
```

## 재시도 정책

| 시도 | 대기 시간 | 누적 시간 |
|-----|----------|----------|
| 1차 (최초) | - | 0분 |
| 2차 | 1분 | 1분 |
| 3차 | 3분 | 4분 |
| 4차 | 9분 | 13분 |

> 참고: `multiplier=3`으로 설정 시 1분 → 3분 → 9분이 됨. 정확히 1분 → 5분 → 15분을 원하면 커스텀 BackOff 정책 필요.

## 테스트 계획

### 단위 테스트

**파일 경로**: `backend/src/test/java/igrus/web/security/auth/common/service/SmtpEmailServiceRetryTest.java`

| 테스트 케이스 | 설명 |
|-------------|------|
| 첫 시도 성공 | 재시도 없이 정상 발송 |
| 2회째 성공 | 1회 실패 후 재시도에서 성공 |
| 3회째 성공 | 2회 실패 후 재시도에서 성공 |
| 모든 시도 실패 | 4회 모두 실패 시 recover 호출 확인 |
| 비동기 동작 확인 | 호출 즉시 반환되고 백그라운드에서 처리 |

## 주의사항

1. **비동기 처리 시 예외 핸들링**: 비동기 메서드에서 발생한 예외는 호출자에게 전파되지 않음
2. **트랜잭션 분리**: 이메일 발송 실패가 주요 트랜잭션 롤백을 유발하지 않도록 분리
3. **테스트 프로파일**: LoggingEmailService에도 동일한 인터페이스 구현 필요

## 체크리스트

- [x] spring-retry 의존성 추가 ✅ 2026-01-25
- [x] RetryConfig 생성 (@EnableRetry) ✅ 2026-01-25
- [x] AsyncConfig 생성 (@EnableAsync) ✅ 2026-01-25
- [x] EmailService 인터페이스에 WithRetry 메서드 추가 ✅ 2026-01-25
- [x] SmtpEmailService에 @Retryable, @Async 적용 ✅ 2026-01-25
- [x] LoggingEmailService 동일하게 수정 (테스트용) ✅ 2026-01-25
- [x] 기존 서비스에서 WithRetry 메서드 호출로 변경 ✅ 2026-01-25
- [x] 단위 테스트 작성 (SmtpEmailServiceRetryTest) ✅ 2026-01-25
- [ ] 통합 테스트로 재시도 동작 확인 (Spring Retry AOP 통합 테스트는 선택적)