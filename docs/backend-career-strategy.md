# 백엔드 취업 준비 전략

IGRUS-Web 프로젝트를 중심으로 한 백엔드 개발자 취업 준비 전략.

---

## 1. 현재 프로젝트 강점 분석

### 기술적 차별화 포인트

| 영역 | 구현 내용 | 차별화 수준 |
|------|----------|------------|
| 인증/보안 | JWT + Refresh Token Rotation + 탈취 감지 + Grace Period | 상 |
| 동시성 제어 | 낙관적 잠금 + 원자적 SQL 카운터 + 동시성 테스트 | 상 |
| API 설계 | Contract-First OpenAPI → 프론트/백 코드 자동 생성 | 상 |
| 트랜잭션 관리 | noRollbackFor, REQUIRES_NEW, 이벤트 기반 감사 추적 | 상 |
| 테스트 | 237파일/1,300+케이스, 동시성 테스트, @Transactional 미사용 원칙 | 상 |
| 인프라 | GitHub Actions → ECR → ECS, OIDC 인증, Flyway 무결성 검증 | 중상 |
| 도메인 설계 | 소프트 삭제, 상태 머신, PII 익명화, 권한 체계 | 중상 |
| 문서화 | ADR 12건, 기능 명세, 테스트 케이스 문서 | 중상 |

### 수치 요약

- Java 소스: 658개 파일
- 테스트: 237개 파일, 1,300+ 케이스
- Flyway 마이그레이션: 45개
- ADR: 12건
- OpenAPI 엔드포인트: 100+개

---

## 2. 포트폴리오 어필 전략

### 2.1 이력서/포트폴리오 작성 시 강조할 항목

각 항목은 **"무엇을 → 왜 → 어떻게 → 결과"** 구조로 작성한다.

#### (1) Refresh Token Rotation & 탈취 감지 — 최우선

```
[무엇을] JWT 기반 인증에서 Refresh Token 탈취 공격을 방어하는 시스템 설계 및 구현
[왜]     Access Token만 사용하면 탈취 시 만료까지 무방비, 단순 Refresh Token은 재사용 공격에 취약
[어떻게] Token Family 기반 회전, 이미 사용된 토큰 재사용 감지 시 전체 Family 무효화,
        Grace Period(10초)로 멀티탭 race condition 해결,
        @Version 낙관적 잠금으로 동시 갱신 방지,
        @Transactional(noRollbackFor)로 revoke 커밋 후 예외 전파
[결과]   토큰 탈취 시 즉시 전체 세션 무효화, 동시 요청에서도 안정적 동작
```

#### (2) 낙관적 잠금 + 원자적 카운터 패턴

```
[무엇을] 게시글 동시 수정 방지와 좋아요/조회수 카운터의 동시성 문제 해결
[왜]     @Version으로 게시글 수정을 보호하면, 좋아요/조회수 증가까지 버전 충돌 발생
[어떻게] 카운터 연산은 @Modifying atomic SQL UPDATE로 @Version 우회,
        주기적 스케줄러로 원본 테이블과 카운터 정합성 보정 (eventual consistency)
[결과]   교차 관심사 간 불필요한 충돌 제거, CountDownLatch 기반 동시성 테스트로 검증
```

#### (3) Contract-First API 개발

```
[무엇을] OpenAPI 스펙을 단일 진실점으로 프론트엔드/백엔드 코드 자동 생성
[왜]     수동 API 문서화는 코드와 괴리가 생기고, 프론트-백 타입 불일치 발생
[어떻게] openapi.yaml → 백엔드: Spring Interface + DTO 생성 (openapi-generator),
        프론트엔드: TypeScript 타입 + TanStack Query 훅 생성 (Orval),
        컨트롤러가 생성된 인터페이스를 구현하여 컴파일 타임에 스펙 준수 강제
[결과]   API 문서-코드 간 불일치 원천 차단, 프론트-백 협업 비용 절감
```

#### (4) 이벤트 기반 감사 추적

```
[무엇을] 계정 상태 변경, 이벤트 상태 변경 등의 이력을 비즈니스 로직과 분리하여 기록
[왜]     감사 로그 저장 실패가 비즈니스 트랜잭션을 롤백시키면 안 됨
[어떻게] @EventListener + TransactionTemplate(REQUIRES_NEW)로 독립 트랜잭션 분리,
        try-catch로 감사 실패 격리
[결과]   비즈니스 로직 무영향, @TransactionalEventListener 테스트 미동작 이슈 발견 및 해결
```

#### (5) 테스트 전략

```
[무엇을] 1,300+ 테스트 케이스, 동시성 테스트 포함한 통합 테스트 체계
[왜]     테스트 클래스에 @Transactional 붙이면 실제 트랜잭션 경계와 다른 동작으로 false positive
[어떻게] 테스트에서 @Transactional 사용 금지 원칙,
        cleanupDatabase()로 soft delete 우회한 데이터 정리,
        ExecutorService + CountDownLatch로 동시성 시나리오 검증
[결과]   프로덕션과 동일한 트랜잭션 동작 보장, 동시성 버그 사전 포착
```

### 2.2 포트폴리오 문서 구성 권장안

```
1. 프로젝트 개요 (1문단)
2. 아키텍처 다이어그램 (시스템 구성도)
3. 기술적 도전과 해결 (위 5개 항목 중 3개 선택)
4. ERD 또는 주요 도메인 모델
5. CI/CD 파이프라인 구성도
6. 성과 수치 (테스트 커버리지, API 수, 마이그레이션 수 등)
```

---

## 3. 기술 면접 대비

### 3.1 이 프로젝트에서 나올 수 있는 질문과 답변 전략

#### 인증/보안

| 예상 질문 | 답변 핵심 |
|----------|----------|
| JWT의 단점은? | Stateless라 즉시 무효화 불가 → Refresh Token DB 관리로 보완 |
| Access Token이 탈취되면? | 짧은 만료 시간(5분)으로 피해 최소화, Refresh Token으로 재발급 |
| Refresh Token이 탈취되면? | Token Family 추적, 이미 사용된 토큰 재사용 시 전체 Family 무효화 |
| 동시에 여러 탭에서 토큰 갱신하면? | Grace Period(10초) 내 revoked 토큰은 현재 활성 토큰으로 응답 |
| 세션 기반 대비 JWT의 장단점? | 장점: 서버 무상태, 수평 확장 용이 / 단점: 즉시 무효화 어려움, 토큰 크기 |

#### 동시성

| 예상 질문 | 답변 핵심 |
|----------|----------|
| 낙관적 잠금 vs 비관적 잠금? | 낙관적: 충돌 적을 때 유리, 비관적: 충돌 많을 때 유리. 이 프로젝트는 게시글 수정 충돌이 드물어 낙관적 선택 |
| 좋아요에 낙관적 잠금을 안 쓴 이유? | 게시글 수정과 좋아요는 독립적 관심사인데 같은 version 필드를 공유하면 불필요한 충돌 발생. atomic SQL UPDATE로 version 우회 |
| 카운터 정합성은 어떻게 보장? | 주기적 스케줄러가 원본 테이블 COUNT와 비교하여 보정. 실시간 정확성보다 성능 우선 (eventual consistency) |
| 동시성 테스트는 어떻게? | ExecutorService + CountDownLatch로 N개 스레드 동시 실행, AtomicInteger로 성공/실패 카운트 검증 |

#### 트랜잭션

| 예상 질문 | 답변 핵심 |
|----------|----------|
| @Transactional 전파 옵션은? | REQUIRED(기본), REQUIRES_NEW(독립), NESTED 등. 감사 로그에 REQUIRES_NEW 사용 |
| noRollbackFor를 왜 썼나? | 토큰 탈취 감지 시 revoke는 반드시 커밋되어야 하는데, 예외를 던지면 기본적으로 rollback됨 |
| 테스트에서 @Transactional을 안 쓰는 이유? | 테스트 트랜잭션이 서비스 트랜잭션을 감싸면 LazyLoading 등이 테스트에서만 성공하는 false positive 발생 |

#### 설계/아키텍처

| 예상 질문 | 답변 핵심 |
|----------|----------|
| 서비스를 왜 유즈케이스별로 분리? | SRP + OCP: 새 기능 추가 시 기존 코드 수정 없이 새 클래스 추가. 하나의 서비스가 비대해지는 것 방지 |
| Contract-First의 장단점? | 장점: 스펙-코드 일치 보장, 코드 생성 자동화 / 단점: 초기 설정 비용, 스펙 변경 시 재생성 필요 |
| 소프트 삭제를 선택한 이유? | 데이터 복구 가능성, 감사 추적, 외래 키 참조 무결성 유지. @SQLRestriction으로 쿼리 투명성 확보 |
| ADR을 왜 작성하나? | 시간이 지나면 "왜 이렇게 했는지" 잊힘. 의사결정 맥락을 보존하여 미래의 자신과 팀원을 위한 문서 |

### 3.2 CS 기본기와 연결짓기

이 프로젝트의 구현을 CS 기본 개념과 연결하면 깊이가 더해진다.

| CS 개념 | 프로젝트 연결 |
|---------|-------------|
| ACID | 트랜잭션 전파, noRollbackFor, 감사 로그 독립 트랜잭션 |
| CAP 정리 | 카운터의 eventual consistency 선택 (AP 우선) |
| 동시성 제어 (OS) | 낙관적 잠금 = Compare-And-Swap, 비관적 잠금 = Mutex |
| 네트워크 보안 | JWT 서명 검증 = HMAC, 토큰 탈취 = 중간자 공격 대응 |
| 데이터베이스 인덱싱 | Flyway 마이그레이션에서 인덱스 설계 |
| 소프트웨어 공학 | SOLID 원칙 적용 (SRP 서비스 분리, OCP 확장) |

---

## 4. 프로젝트 개선 로드맵

임팩트와 난이도를 기준으로 우선순위를 매긴 개선 사항.

### Phase 1: 즉시 추가 가능 (1~2주)

#### 4.1 Redis 캐싱 레이어 도입

- **목표**: 읽기 빈도 높은 API 응답 시간 개선
- **적용 대상**: 게시글 목록, 대시보드 통계, 게시판 목록
- **구현 방향**:
  - Spring Cache Abstraction (`@Cacheable`, `@CacheEvict`)
  - Cache-Aside 패턴 (읽기 시 캐시 확인 → 미스 시 DB 조회 → 캐시 저장)
  - TTL 기반 만료 + 쓰기 시 명시적 무효화
  - 캐시 워밍업 전략
- **면접 어필**: 캐시 설계, 일관성-성능 트레이드오프, 캐시 스탬피드 방지

#### 4.2 API Rate Limiting

- **목표**: 로그인 브루트포스, 회원가입 남용 방지
- **적용 대상**: 인증 API (`/login`, `/signup`, `/password-reset`)
- **구현 방향**:
  - Bucket4j + Redis 기반 Sliding Window
  - IP별 + 사용자별 이중 제한
  - `429 Too Many Requests` 응답 + `Retry-After` 헤더
- **면접 어필**: 보안, 가용성, 분산 환경에서의 rate limiting

### Phase 2: 아키텍처 고도화 (2~4주)

#### 4.3 Testcontainers 도입

- **목표**: H2 대신 실제 MySQL로 테스트하여 신뢰도 향상
- **구현 방향**:
  - Testcontainers MySQL 모듈
  - Flyway 마이그레이션이 실제 MySQL에서 동작하는지 검증
  - CI에서도 Testcontainers 사용
- **면접 어필**: 테스트 환경-프로덕션 환경 일치, 테스트 신뢰도

#### 4.4 QueryDSL 도입 + 복잡 검색 쿼리 최적화

- **목표**: 타입 안전한 동적 쿼리, N+1 문제 해결
- **적용 대상**: 관리자 사용자 검색, 게시글 필터링, 문의 목록
- **구현 방향**:
  - QueryDSL + JPAQueryFactory
  - 동적 where 조건 조합 (BooleanExpression)
  - fetch join으로 N+1 해결
  - 커버링 인덱스 활용
- **면접 어필**: 쿼리 최적화, 타입 안전, N+1 문제 이해

#### 4.5 비동기 이벤트 처리 (메시지 큐)

- **목표**: 웹훅, 이메일, 감사 로그를 메시지 큐로 분리
- **구현 방향**:
  - AWS SQS 또는 RabbitMQ
  - 현재 `@Async` + `@Retryable` 웹훅을 큐 기반으로 전환
  - Dead Letter Queue로 실패 메시지 관리
  - Idempotency Key로 중복 처리 방지
- **면접 어필**: 메시지 기반 아키텍처, 복원력, 멱등성

### Phase 3: 운영 성숙도 (4주+)

#### 4.6 분산 추적 + 구조화 로깅 고도화

- **목표**: 요청 단위 추적, 장애 진단 시간 단축
- **구현 방향**:
  - Micrometer Tracing + Zipkin
  - 요청별 Trace ID를 모든 로그에 포함
  - 현재 logstash-logback-encoder 활용하여 JSON 구조화 로깅 강화
  - Grafana 대시보드 구성
- **면접 어필**: 관측 가능성(Observability), 운영 역량

#### 4.7 성능 테스트 + 부하 테스트

- **목표**: 병목 지점 식별, 성능 기준선 확보
- **구현 방향**:
  - k6 또는 Gatling으로 시나리오 기반 부하 테스트
  - 주요 API 응답 시간 SLA 설정
  - 결과를 문서화하여 포트폴리오에 수치 근거 추가
- **면접 어필**: 성능 엔지니어링, 정량적 사고

---

## 5. 면접 스토리텔링 템플릿

기술 면접에서 프로젝트 경험을 설명할 때 STAR 구조를 활용한다.

### 예시 1: Refresh Token 탈취 감지

```
[Situation] 동아리 웹사이트의 JWT 인증 시스템을 구현하는 과정에서,
            단순 Refresh Token 방식은 토큰 탈취 시 무방비 상태라는 문제를 인식

[Task]      토큰이 탈취되더라도 피해를 최소화하고,
            탈취 사실을 감지하여 즉시 대응하는 시스템 필요

[Action]    - Token Family 개념 도입: 하나의 로그인 세션에서 발급된 모든 토큰을 그룹으로 추적
            - 이미 사용된(revoked) 토큰이 재사용되면 탈취로 판단, 전체 Family 무효화
            - 멀티탭 환경의 race condition은 Grace Period(10초)로 해결
            - @Version 낙관적 잠금으로 동시 갱신 방지
            - @Transactional(noRollbackFor)로 revoke 커밋 보장 후 예외 전파

[Result]    - 토큰 탈취 시 평균 1회 요청 내에 감지 및 전체 세션 무효화
            - 정상 사용자의 멀티탭 사용에는 영향 없음
            - ADR 문서로 의사결정 과정 기록
```

### 예시 2: 동시성 문제 해결

```
[Situation] 게시글의 좋아요, 조회수, 북마크 카운터가 동시 요청 시
            @Version 낙관적 잠금과 충돌하는 문제 발견

[Task]      게시글 내용 수정의 동시성은 보호하면서,
            카운터 증가는 충돌 없이 처리해야 함

[Action]    - 문제 분석: 좋아요와 게시글 수정은 독립적 관심사인데
              같은 @Version 필드를 공유하여 불필요한 OptimisticLockException 발생
            - 해결: 카운터 연산은 atomic SQL UPDATE (@Modifying @Query)로 @Version 우회
            - 정합성: 주기적 스케줄러가 원본 테이블 COUNT와 비교하여 카운터 보정
            - 검증: ExecutorService + CountDownLatch로 8개 동시성 시나리오 테스트 작성

[Result]    - 교차 관심사 간 버전 충돌 100% 제거
            - 카운터 정확도: 실시간 근사치 + 주기적 정합성 보정 (eventual consistency)
```

### 예시 3: 테스트에서 @TransactionalEventListener 미동작 발견

```
[Situation] 감사 로그를 @TransactionalEventListener(AFTER_COMMIT)로 구현했는데,
            통합 테스트에서 감사 로그가 0건 저장되는 현상 발견

[Task]      원인을 파악하고, 테스트와 프로덕션 모두에서 안정적으로 동작하는 방식으로 전환

[Action]    - 디버깅: @TransactionalEventListener(AFTER_COMMIT)가 @SpringBootTest 환경에서
              트랜잭션 커밋 이벤트를 수신하지 못하는 것을 확인
            - 대안 탐색: @EventListener + TransactionTemplate(REQUIRES_NEW) 조합으로 전환
            - 이점: 리스너가 동기적으로 실행되고, REQUIRES_NEW로 독립 트랜잭션 보장
            - try-catch로 감사 실패가 비즈니스 로직에 영향 없도록 격리

[Result]    - 테스트와 프로덕션 모두에서 안정적으로 감사 로그 기록
            - 트러블슈팅 과정을 MEMORY.md에 기록하여 재발 방지
```

---

## 6. 기술 블로그 주제 추천

면접관이 사전에 볼 수 있는 블로그 글을 작성하면 효과적이다.

### 우선 작성 추천 (프로젝트에서 직접 경험한 내용)

1. **"JWT Refresh Token Rotation과 탈취 감지 구현기"**
   - Token Family, Grace Period, 트랜잭션 롤백 제어까지 전체 과정

2. **"@Version 낙관적 잠금의 함정: 교차 관심사 충돌 해결기"**
   - 좋아요와 게시글 수정이 충돌하는 문제 발견부터 해결까지

3. **"테스트에서 @Transactional을 쓰지 않는 이유"**
   - false positive 사례, @TransactionalEventListener 미동작 발견 경험

4. **"Contract-First API 개발: OpenAPI로 프론트-백 협업 자동화"**
   - 도입 배경, 설정, 코드 생성 과정, 장단점 경험

5. **"Spring Boot에서 소프트 삭제 + 개인정보 익명화 구현하기"**
   - @SQLRestriction, unique 제약 해제 트릭, 클린업 스케줄러

### 추가 작성 추천 (개선 사항 구현 후)

6. "Redis 캐시 설계: Cache-Aside 패턴과 일관성 전략"
7. "Testcontainers로 H2에서 MySQL 테스트로 전환하기"
8. "API Rate Limiting 구현: Bucket4j + Redis Sliding Window"

---

## 7. 실행 체크리스트

### 즉시 할 일

- [ ] 포트폴리오 문서에 프로젝트 아키텍처 다이어그램 추가
- [ ] README.md에 기술 스택 + 핵심 기능 요약 보강
- [ ] 블로그 글 1편 작성 (Refresh Token Rotation 추천)

### 1~2주 내

- [ ] Redis 캐싱 레이어 구현
- [ ] Rate Limiting 구현
- [ ] 블로그 글 2편 작성

### 2~4주 내

- [ ] Testcontainers 도입
- [ ] QueryDSL 도입 + 검색 쿼리 최적화
- [ ] 성능 테스트 + 결과 문서화

### 지속적

- [ ] ADR 추가 작성 (새로운 기술 결정마다)
- [ ] 테스트 커버리지 유지/향상
- [ ] 면접 예상 질문에 대한 답변 연습