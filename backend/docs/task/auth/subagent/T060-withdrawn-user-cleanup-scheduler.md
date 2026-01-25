# T060: 탈퇴 후 5일 경과 개인정보 영구 삭제 스케줄러

## 개요

탈퇴 후 5일(복구 가능 기간) 경과한 사용자의 개인정보를 영구 삭제합니다. 개인정보보호법 준수 및 데이터 최소화 원칙을 따릅니다.

## 구현 목표

- 탈퇴 후 5일 경과한 사용자의 개인정보 익명화/삭제
- 필수 보관 데이터(활동 이력 등)는 익명화하여 유지
- 완전 삭제 대상 데이터 정의 및 처리

## 개인정보 처리 정책

### 완전 삭제 대상
- 이메일 주소
- 전화번호
- 비밀번호 (PasswordCredential)
- 개인정보 동의 기록 (PrivacyConsent)
- 이메일 인증 기록 (EmailVerification)
- Refresh Token

### 익명화 처리 대상
- 이름: "탈퇴회원" + 랜덤 해시 (예: "탈퇴회원_a1b2c3")
- 학번: 익명화 (예: "DELETED_123456")

### 유지 대상
- User ID (FK 참조 유지를 위해)
- 활동 기록, 게시글 등 (작성자 표시를 "탈퇴한 회원"으로)

## 구현 상세

### 1. 스케줄러 클래스 생성

**파일 경로**: `backend/src/main/java/igrus/web/security/auth/common/scheduler/WithdrawnUserCleanupScheduler.java`

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawnUserCleanupScheduler {

    private final WithdrawnUserCleanupService withdrawnUserCleanupService;

    // 매일 새벽 5시 실행
    @Scheduled(cron = "0 0 5 * * *")
    public void cleanupWithdrawnUsers() {
        log.info("탈퇴 사용자 개인정보 정리 스케줄러 시작");
        int processedCount = withdrawnUserCleanupService.anonymizeExpiredWithdrawnUsers();
        log.info("탈퇴 사용자 개인정보 정리 완료: {}명 처리", processedCount);
    }
}
```

### 2. 서비스 클래스 생성

**파일 경로**: `backend/src/main/java/igrus/web/security/auth/common/service/WithdrawnUserCleanupService.java`

```java
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WithdrawnUserCleanupService {

    private static final int RECOVERY_PERIOD_DAYS = 5;

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final PrivacyConsentRepository privacyConsentRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public int anonymizeExpiredWithdrawnUsers() {
        Instant cutoffTime = Instant.now().minus(RECOVERY_PERIOD_DAYS, ChronoUnit.DAYS);

        // 탈퇴 후 5일 경과한 사용자 조회 (아직 익명화되지 않은)
        List<User> usersToAnonymize = userRepository
            .findWithdrawnUsersBeforeAndNotAnonymized(cutoffTime);

        int count = 0;
        for (User user : usersToAnonymize) {
            anonymizeUser(user);
            count++;
        }

        return count;
    }

    private void anonymizeUser(User user) {
        Long userId = user.getId();
        String anonymousHash = generateAnonymousHash();

        // 1. 연관 데이터 삭제
        passwordCredentialRepository.deleteByUserId(userId);
        privacyConsentRepository.deleteByUserId(userId);
        emailVerificationRepository.deleteByUserId(userId);
        refreshTokenRepository.deleteByUserId(userId);

        // 2. 사용자 정보 익명화
        user.anonymize(anonymousHash);
        userRepository.save(user);

        log.debug("사용자 익명화 완료: userId={}", userId);
    }

    private String generateAnonymousHash() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
```

### 3. User 엔티티에 익명화 메서드 추가

**파일 경로**: `backend/src/main/java/igrus/web/user/domain/User.java`

```java
// User 엔티티에 추가
public void anonymize(String hash) {
    this.name = "탈퇴회원_" + hash;
    this.email = "deleted_" + hash + "@deleted.local";
    this.studentId = "DELETED_" + this.id;
    this.phoneNumber = null;
    this.department = null;
    this.joinReason = null;
    this.isAnonymized = true; // 새 필드 추가 필요
}
```

### 4. Repository 메서드 추가

```java
// UserRepository에 추가
@Query("SELECT u FROM User u WHERE u.status = 'WITHDRAWN' " +
       "AND u.deletedAt < :cutoffTime AND u.isAnonymized = false")
List<User> findWithdrawnUsersBeforeAndNotAnonymized(@Param("cutoffTime") Instant cutoffTime);

// 각 Repository에 deleteByUserId 추가 (없는 경우)
void deleteByUserId(Long userId);
```

### 5. Flyway 마이그레이션 (User 테이블에 isAnonymized 컬럼 추가)

**파일 경로**: `backend/src/main/resources/db/migration/V{N}__add_user_anonymized_flag.sql`

```sql
ALTER TABLE users ADD COLUMN is_anonymized BOOLEAN NOT NULL DEFAULT FALSE;
```

## 테스트 계획

### 단위 테스트

**파일 경로**: `backend/src/test/java/igrus/web/security/auth/common/service/WithdrawnUserCleanupServiceTest.java`

| 테스트 케이스 | 설명 |
|-------------|------|
| 5일 경과 사용자 익명화 | 탈퇴 후 5일 경과한 사용자 개인정보 삭제 및 익명화 |
| 5일 미경과 사용자 유지 | 아직 복구 기간 내인 사용자는 처리하지 않음 |
| 이미 익명화된 사용자 스킵 | 중복 처리 방지 |
| 연관 데이터 삭제 확인 | PasswordCredential, PrivacyConsent 등 삭제 |
| 처리 대상 없음 | 대상이 없으면 0 반환 |

## 설정

- 실행 시간: 매일 새벽 5시 (cron: `0 0 5 * * *`)
- 복구 가능 기간: 5일 (app.auth.recovery-period-days 설정 고려)

## 체크리스트

- [x] User 엔티티에 isAnonymized 필드 추가
- [x] User 엔티티에 anonymize() 메서드 추가
- [x] Flyway 마이그레이션 스크립트 작성 (`V4__add_user_anonymized_flag.sql`)
- [x] WithdrawnUserCleanupService 생성
- [x] WithdrawnUserCleanupScheduler 생성
- [x] UserRepository에 쿼리 메서드 추가 (`findWithdrawnUsersBeforeAndNotAnonymized`)
- [x] 각 Repository에 삭제 메서드 추가 (`EmailVerificationRepository.deleteByEmail`, `RefreshTokenRepository.deleteByUserId`)
- [x] 단위 테스트 작성 (`WithdrawnUserCleanupServiceTest`)
- [ ] 통합 테스트로 전체 플로우 확인 (선택사항)