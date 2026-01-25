# T059: 만료된 Refresh Token 정리 스케줄러

## 개요

만료된 Refresh Token을 주기적으로 정리하여 데이터베이스 공간을 확보하고 성능을 유지합니다.

## 구현 목표

- 만료된 Refresh Token 자동 삭제
- 매일 새벽 시간대에 실행 (서버 부하가 적은 시간)
- 삭제 결과 로깅

## 구현 상세

### 1. 스케줄러 클래스 생성

**파일 경로**: `backend/src/main/java/igrus/web/security/auth/common/scheduler/RefreshTokenCleanupScheduler.java`

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenCleanupService refreshTokenCleanupService;

    // 매일 새벽 4시 실행 (UnverifiedUserCleanupScheduler는 3시에 실행)
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupExpiredTokens() {
        log.info("만료된 Refresh Token 정리 스케줄러 시작");
        int deletedCount = refreshTokenCleanupService.deleteExpiredTokens();
        log.info("만료된 Refresh Token 정리 완료: {}개 삭제", deletedCount);
    }
}
```

### 2. 서비스 클래스 생성

**파일 경로**: `backend/src/main/java/igrus/web/security/auth/common/service/RefreshTokenCleanupService.java`

```java
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    public int deleteExpiredTokens() {
        Instant now = Instant.now();
        return refreshTokenRepository.deleteByExpiresAtBefore(now);
    }
}
```

### 3. Repository 메서드 추가

**파일 경로**: `backend/src/main/java/igrus/web/security/auth/common/repository/RefreshTokenRepository.java`

```java
// 기존 인터페이스에 추가
@Modifying
@Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
int deleteByExpiresAtBefore(@Param("now") Instant now);
```

## 의존성

- RefreshToken 엔티티의 `expiresAt` 필드 활용
- 기존 RefreshTokenRepository 확장

## 테스트 계획

### 단위 테스트

**파일 경로**: `backend/src/test/java/igrus/web/security/auth/common/service/RefreshTokenCleanupServiceTest.java`

| 테스트 케이스 | 설명 |
|-------------|------|
| 만료된 토큰만 삭제 | 만료 시간이 지난 토큰만 삭제되고, 유효한 토큰은 유지 |
| 삭제 대상 없음 | 만료된 토큰이 없으면 0 반환 |
| 다수 토큰 삭제 | 여러 만료 토큰이 한 번에 삭제됨 |

## 설정

- 스케줄링 활성화 필요: `@EnableScheduling` (이미 설정되어 있을 것으로 예상)
- 실행 시간: 매일 새벽 4시 (cron: `0 0 4 * * *`)

## 체크리스트

- [x] RefreshTokenCleanupService 생성
- [x] RefreshTokenCleanupScheduler 생성
- [x] RefreshTokenRepository에 deleteByExpiresAtBefore 메서드 추가 (수정: Instant 타입, int 반환)
- [x] 단위 테스트 작성 (5개 케이스)
- [ ] 로컬 환경에서 스케줄러 동작 확인

## 구현 완료 (2026-01-25)

### 변경 사항

1. **RefreshTokenRepository** - `deleteByExpiresAtBefore` 메서드 수정
   - 기존: `void deleteByExpiresAtBefore(LocalDateTime dateTime)`
   - 변경: `int deleteByExpiresAtBefore(Instant now)` - 엔티티의 `expiresAt` 필드 타입(Instant)에 맞게 수정, 삭제된 개수 반환

2. **RefreshTokenCleanupService** 생성
   - 경로: `backend/src/main/java/igrus/web/security/auth/common/service/RefreshTokenCleanupService.java`
   - `deleteExpiredTokens()` 메서드로 만료된 토큰 삭제

3. **RefreshTokenCleanupScheduler** 생성
   - 경로: `backend/src/main/java/igrus/web/security/auth/common/scheduler/RefreshTokenCleanupScheduler.java`
   - 매일 새벽 4시 실행 (cron: `0 0 4 * * *`)

4. **단위 테스트** 작성
   - 경로: `backend/src/test/java/igrus/web/security/auth/common/service/RefreshTokenCleanupServiceTest.java`
   - 5개 테스트 케이스 모두 통과