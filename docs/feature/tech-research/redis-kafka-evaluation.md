# Redis / Kafka 도입 검토 및 학습 전략

> 작성일: 2026-02-24
> 상태: 검토 완료 - 현재 프로젝트에는 미도입, 별도 실험 프로젝트로 학습 예정

## 배경

신입 백엔드 채용 시장에서 Redis, Kafka 경험을 요구하는 경우가 많다. 그러나 IGRUS-Web처럼 소규모 트래픽의 프로젝트에서는 이들 기술이 실질적으로 불필요하며, 근거 없이 도입하면 오버엔지니어링이 된다.

이 문서는 다음 두 가지를 정리한다:

1. IGRUS-Web에 Redis/Kafka를 도입하지 않는 기술적 근거
2. 별도 실험 프로젝트를 통한 학습 전략

## IGRUS-Web 현황 분석

| 항목 | 현재 상태 |
|---|---|
| 일일 활성 사용자 | 100명 미만 (동아리 회원 규모) |
| 동시 접속 | 최대 수십 명 수준 |
| 아키텍처 | 단일 서버, 모놀리식 |
| DB | MySQL 단일 인스턴스 |

## 미도입 판단 근거

### Redis

- 현재 트래픽에서 DB 직접 조회의 응답 시간이 충분히 낮음
- 캐시 무효화 로직이 추가되면 코드 복잡도만 증가
- 세션 관리: 단일 서버이므로 서버 메모리 세션으로 충분
- 분산 락: 단일 인스턴스 환경에서 불필요

### Kafka

- 비동기 처리 대상이 제한적 (Spring `@EventListener`로 충분)
- MSA가 아닌 모놀리식 구조에서 메시지 브로커의 이점이 거의 없음
- 인프라 복잡도 대비 얻는 이점이 없음

## 학습 ROI 비교: Redis vs Kafka

신입 백엔드 취준 관점에서 **Redis의 ROI가 압도적으로 높다.**

### 실무 등장 빈도

- **Redis**: 백엔드 프로젝트 대부분에서 사용 (캐시, 세션, 분산 락, Rate Limiting)
- **Kafka**: 대규모 서비스나 MSA 환경에서만 등장. 신입이 배치되는 팀에서 직접 다룰 확률 낮음

### 면접 출제 빈도

| Redis (거의 매 면접) | Kafka (시니어급 질문) |
|---|---|
| 캐시 전략 (Cache Aside, Write Through) | 파티션과 컨슈머 그룹 |
| TTL과 캐시 무효화 | Exactly-once 보장 |
| 세션 저장소 | 이벤트 소싱 |
| 동시성 제어 (분산 락) | - |

### 학습 곡선

- **Redis**: Docker + `spring-boot-starter-data-redis`로 30분 안에 동작
- **Kafka**: Zookeeper/KRaft + Broker + Producer + Consumer + 토픽 설계... 환경 세팅만 반나절

### 포트폴리오 설명 난이도

- **Redis**: "캐시 적용으로 응답 시간 70% 감소" → 직관적
- **Kafka**: "이벤트 기반 비동기 처리 도입" → "왜 @Async로 안 했나요?" → 설명이 꼬리를 물음

### 결론

**Redis 먼저, Kafka는 여유 있으면.** Redis 실험 하나를 깊이 있게 해서 숫자를 뽑는 게, 둘 다 얕게 아는 것보다 훨씬 낫다.

## 실험 프로젝트 계획

### 프로젝트 구조

```
redis-kafka-lab/
├── docker-compose.yml       # MySQL, Redis, Kafka, Zookeeper
├── sync-api/                # 동기 처리 버전
├── async-api/               # Kafka 비동기 버전
├── cache-api/               # Redis 캐시 버전
├── load-test/               # k6 스크립트
└── results/                 # 벤치마크 결과 + 그래프
    ├── cache-benchmark.md
    ├── async-benchmark.md
    └── graphs/
```

### Redis 실험 시나리오

> 모든 시나리오는 IGRUS-Web의 실제 기능을 기반으로 설계했다.
> 실험 프로젝트에서 동일한 도메인을 재현하되, 트래픽만 증폭시켜 Redis 도입 효과를 측정한다.

#### 시나리오 1: 게시글 상세 조회 캐시 (Cache Aside 패턴)

**IGRUS-Web 대응 기능:** 게시글 상세 조회 API

현재 IGRUS-Web의 게시글 상세 조회는 한 번의 요청에 다음 쿼리가 발생한다:
- Post 조회 (EntityGraph로 author JOIN)
- 좋아요 여부 확인 (`existsByPostIdAndUserId`)
- 북마크 여부 확인 (`existsByPostIdAndUserId`)
- 조회수 기록 (비동기)

**설정:**

- DB: MySQL에 게시글 10만건 + 유저 1만명 + 좋아요/북마크 데이터 seed
- 부하 도구: k6
- 시나리오: 인기 게시글 상위 100개에 트래픽 집중 (Zipf 분포)
- 동시 요청: 50 / 100 / 300 / 500명

**비교 축:**

| | Redis 없음 | Redis 캐시 적용 |
|---|---|---|
| 게시글 데이터 | 매번 DB JOIN 쿼리 | Cache Aside (TTL 5분) |
| 좋아요/북마크 상태 | 매번 DB EXISTS 쿼리 | 유저별 Set 캐시 |
| DB 커넥션 소비 | 요청당 3~4개 쿼리 | 캐시 히트 시 0개 |

**도출 목표:**

- 인기 게시글의 캐시 히트율과 응답 시간 상관관계 그래프
- "동시 접속 N명 이하에서는 DB 직접 조회로 충분하다"는 **임계점** 도출
- 캐시 무효화 전략 비교: TTL만 사용 vs 좋아요/북마크 토글 시 즉시 무효화
- DB 커넥션 풀 소진 시점 측정 (HikariCP 기본 10개 기준)

**면접 키워드:** Cache Aside, TTL, 캐시 무효화, Look Aside + Write Through 혼합

---

#### 시나리오 2: 로그인 Rate Limiting (Redis Counter)

**IGRUS-Web 대응 기능:** 로그인 시도 제한 (5회 실패 시 30분 잠금)

현재 IGRUS-Web은 `LoginAttempt` 엔티티를 DB에 저장하고, 매 로그인 시도마다 DB를 조회해서 잠금 여부를 판단한다. 이 패턴은 Redis Counter + TTL로 대체하면 훨씬 효율적이다.

**설정:**

- 시나리오 A (DB 기반): `LoginAttemptRepository.findByStudentId()` → COUNT → 잠금 판단
- 시나리오 B (Redis 기반): `INCR login:attempt:{studentId}` + `EXPIRE 1800` → 값 비교

**비교 축:**

| | DB 기반 (현재) | Redis Counter |
|---|---|---|
| 잠금 확인 | SELECT + COUNT 쿼리 | `GET` O(1) |
| 실패 기록 | INSERT 쿼리 | `INCR` O(1) |
| 자동 해제 | 스케줄러 또는 조건부 쿼리 필요 | TTL 자동 만료 |
| 브루트포스 공격 시 | DB에 대량 INSERT 발생 | 메모리만 사용, DB 무부하 |

**실험:**

- 단일 계정에 초당 100회 로그인 시도 (브루트포스 시뮬레이션)
- DB 기반: INSERT 폭주로 인한 write 부하 측정
- Redis 기반: `INCR` + `EXPIRE`의 처리량 측정
- 잠금 해제 정확도 비교 (TTL vs 스케줄러)

**도출 목표:**

- 브루트포스 공격 시 DB write 부하 vs Redis 메모리 사용량 비교
- "정상 트래픽에서는 DB 기반으로 충분하지만, 공격 상황에서는 Redis가 DB를 보호한다"는 근거
- Redis TTL을 이용한 자동 만료가 스케줄러 대비 얼마나 간결한지 코드 복잡도 비교

**면접 키워드:** Rate Limiting, Sliding Window Counter, TTL, 브루트포스 방어

---

#### 시나리오 3: 선착순 행사 등록 - 분산 락 (Redisson)

**IGRUS-Web 대응 기능:** 행사 등록 API (선착순 자동 승인 모드)

현재 IGRUS-Web의 행사 등록은 `incrementCurrentCountIfAvailable`로 Atomic SQL UPDATE를 사용한다.
단일 서버에서는 잘 동작하지만, 서버가 2대 이상이 되면 다음 문제가 발생할 수 있다:

- 정원 30명인 행사에 동시 50명이 요청 → 원자적 UPDATE이긴 하지만, 등록 가능 여부 확인과 유저 검증 사이에 갭 존재
- 같은 유저의 중복 등록 방지(`existsByEventIdAndUserId`)와 실제 INSERT 사이의 레이스 컨디션

**설정:**

- 행사 정원: 30명
- 동시 등록 요청: 100명 (선착순 경쟁 시뮬레이션)
- 서버: Docker Compose로 2대 로드밸런싱

**비교 축:**

| | Atomic SQL만 (현재) | Atomic SQL + Redis 분산 락 |
|---|---|---|
| 정원 초과 방지 | DB 레벨에서 보장 | 애플리케이션 레벨에서 선제 차단 |
| 중복 등록 방지 | UNIQUE 제약 + existsBy | 락 획득 실패 시 즉시 거절 |
| 서버 2대 환경 | 레이스 컨디션 가능성 존재 | 분산 락으로 직렬화 |
| 응답 시간 (경합 시) | DB 레벨 경합 → 느림 | 락 대기 → 타임아웃으로 빠른 실패 |

**실험:**

1. 서버 1대 + Atomic SQL: 정원 초과 발생 여부 확인
2. 서버 2대 + Atomic SQL: 레이스 컨디션 재현 시도
3. 서버 2대 + Redis 분산 락: 정합성 보장 확인
4. 부하별 응답 시간 비교 (정상 vs 경합 상황)

**도출 목표:**

- "단일 서버 + Atomic SQL이면 분산 락이 불필요하다"는 근거 (IGRUS-Web의 현재 선택이 합리적)
- 멀티 인스턴스 전환 시 분산 락이 필요해지는 구체적 조건
- Redisson의 `tryLock` 타임아웃이 사용자 경험에 미치는 영향 (빠른 실패 vs 무한 대기)

**면접 키워드:** 분산 락, Redisson, tryLock, 선착순 동시성 제어, Optimistic vs Pessimistic

---

#### 시나리오 4: 게시글 작성 Rate Limit (Sliding Window)

**IGRUS-Web 대응 기능:** 게시글 작성 제한 (시간당 20개)

현재 IGRUS-Web은 `countByAuthorAndCreatedAtAfter(userId, oneHourAgo)`로 매 게시글 작성 시 DB COUNT 쿼리를 실행한다. Redis Sorted Set을 사용하면 더 정밀한 Sliding Window 패턴을 구현할 수 있다.

**설정:**

- 시나리오 A (DB COUNT): `SELECT COUNT(*) FROM posts WHERE author_id = ? AND created_at > ?`
- 시나리오 B (Redis Fixed Window): `INCR post:rate:{userId}:{hourBucket}` + TTL 1시간
- 시나리오 C (Redis Sliding Window): `ZADD post:rate:{userId} {timestamp} {postId}` + `ZREMRANGEBYSCORE` + `ZCARD`

**비교 축:**

| | DB COUNT | Fixed Window | Sliding Window |
|---|---|---|---|
| 정확도 | 정확 | 경계 시점에 2배 허용 가능 | 정확 |
| 성능 | 인덱스 스캔 | O(1) | O(log N) |
| 구현 복잡도 | 낮음 | 매우 낮음 | 중간 |

**실험:**

- Fixed Window의 경계 문제 재현: 00:59에 20개 + 01:00에 20개 = 1분간 40개 통과
- Sliding Window의 정확한 차단 확인
- 각 방식의 처리량 비교 (초당 요청 수)

**도출 목표:**

- "시간당 20개 수준이면 DB COUNT로 충분하다"는 근거 (인덱스 있으면 빠름)
- Fixed Window의 경계 문제를 숫자로 보여주기
- Sliding Window가 필요해지는 조건 (제한이 엄격하거나, 초 단위 Rate Limit이 필요한 경우)

**면접 키워드:** Rate Limiting, Sliding Window, Fixed Window, Token Bucket, Sorted Set

---

#### 시나리오 5: 조회수 Write-Behind 패턴 (Redis → DB 배치 동기화)

**IGRUS-Web 대응 기능:** 게시글 조회수 기록

현재 IGRUS-Web은 조회수를 두 곳에 기록한다:
- `Post.viewCount` - Atomic SQL UPDATE로 즉시 증가
- `PostView` 테이블 - 비동기로 개별 조회 이력 INSERT (고유 조회자 수 계산용)

트래픽이 증가하면 매 조회마다 DB UPDATE + INSERT가 병목이 된다.

**설정:**

- 게시글 1000건, 유저 1만명
- 조회 패턴: 인기 게시글 상위 10개에 80% 트래픽 집중
- 동시 요청: 100 / 300 / 1000명

**비교 축:**

| | 즉시 DB 기록 (현재) | Redis 버퍼 + 배치 동기화 |
|---|---|---|
| 조회수 증가 | 매번 `UPDATE posts SET view_count = view_count + 1` | `INCR view:{postId}` |
| DB write 빈도 | 조회 수 = write 수 | 10초마다 배치 flush |
| 데이터 유실 위험 | 없음 | Redis 장애 시 카운트 유실 가능 |
| DB 커넥션 소비 | 조회마다 1 write | 배치당 N건 bulk update |

**실험:**

1. 동시 1000명이 같은 게시글 조회 시 DB write 부하 측정
2. Redis 버퍼 + 10초 배치 flush 시 DB write 부하 측정
3. Redis 장애 시뮬레이션: 버퍼 유실량 측정
4. HikariCP 커넥션 풀 사용률 비교

**도출 목표:**

- "초당 N건 이하의 조회에서는 즉시 DB 기록이 더 간단하고 안전하다"는 임계점
- Write-Behind의 이점이 명확해지는 트래픽 수준
- 데이터 유실 허용 범위와 비즈니스 요구사항의 관계 (조회수는 약간의 손실 허용 가능)

**면접 키워드:** Write-Behind, Write-Back, 배치 처리, 데이터 정합성 vs 성능 트레이드오프

---

#### 시나리오 6: 세션 저장소 비교 (스케일아웃 대응)

**IGRUS-Web 대응 기능:** JWT 기반 인증 (Refresh Token)

현재 IGRUS-Web은 JWT + Refresh Token Rotation을 사용한다. Refresh Token은 DB에 저장되며, 매 토큰 갱신 시 DB 조회가 발생한다. 만약 세션 기반 인증이었다면 Redis가 필수였을 것이다.

**비교 대상:**

1. HttpSession (서버 메모리) - 단일 서버
2. Spring Session + Redis - 다중 서버
3. JWT + Refresh Token in DB (현재 IGRUS-Web 방식)

**실험:**

- Docker Compose로 서버 2대 로드밸런싱 환경 구성
- 로그인 후 요청이 다른 서버로 라우팅될 때:
  - 메모리 세션: 세션 유실
  - Redis 세션: 정상 유지
  - JWT: 서버 무관하게 동작 (stateless)

**도출 목표:**

- JWT 선택이 스케일아웃에서 세션 저장소 문제를 원천 차단한다는 근거
- 그럼에도 Redis가 필요한 경우: Refresh Token 캐시, 토큰 블랙리스트
- 단일 서버에서 Redis 세션이 불필요하다는 근거

**면접 키워드:** 세션 vs JWT, Stateless, 스케일아웃, Redis Session

### Kafka 실험 시나리오 (선택)

#### 시나리오 3: 동기 vs 비동기 처리 비교

**설정:** 주문 시스템 (주문 → 결제 → 알림 → 포인트 적립)

```
[동기 처리]
POST /orders
  → 결제 API 호출 (200ms)
  → 알림 발송 (150ms)
  → 포인트 적립 (100ms)
  → 총 응답: ~450ms

[Kafka 비동기 처리]
POST /orders
  → 결제 API 호출 (200ms)
  → OrderCreated 이벤트 발행 (5ms)
  → 총 응답: ~205ms

  (Consumer가 별도로 알림 + 포인트 처리)
```

**비교 축:**

| | 동기 | Kafka 비동기 |
|---|---|---|
| 응답 시간 | 전체 체인 합산 | 핵심 로직만 |
| 알림 서버 장애 시 | 주문 전체 실패 | 주문은 성공, 알림만 재시도 |
| 처리량 (TPS) | 병목에 의존 | 독립적 스케일링 |

**도출 목표:**

- 후속 작업이 N개 이하이고 실패 허용 불가면 동기가 단순하고 나음
- 장애 격리가 필요한 시점의 구체적 조건

#### 시나리오 4: 이벤트 유실 vs 보장 실험

- `ApplicationEvent` (Spring 내부 이벤트) → 서버 다운 시 유실
- Kafka → 서버 다운 후 재시작해도 미처리 메시지 보존

**방법:** 이벤트 처리 중 프로세스 강제 종료 → 재시작 후 미처리 이벤트 잔존 여부 확인

## 시나리오별 면접 키워드 매핑

| 시나리오 | 핵심 면접 키워드 | 질문 빈도 |
|---|---|---|
| 1. 게시글 캐시 | Cache Aside, TTL, 캐시 무효화 | 거의 매번 |
| 2. 로그인 Rate Limit | Rate Limiting, TTL, 브루트포스 방어 | 자주 |
| 3. 선착순 분산 락 | 분산 락, Redisson, 동시성 제어 | 자주 |
| 4. 작성 Rate Limit | Sliding Window, Sorted Set | 가끔 |
| 5. 조회수 Write-Behind | Write-Behind, 배치 처리, 정합성 트레이드오프 | 가끔 |
| 6. 세션 저장소 | 세션 vs JWT, Stateless, 스케일아웃 | 자주 |

## 면접 활용 예시

### IGRUS-Web 미도입 설명

> "IGRUS-Web에서는 일일 사용자가 100명 미만이라 Redis를 도입하지 않았습니다. 별도 실험 프로젝트에서 동일한 도메인(게시판, 선착순 등록)을 벤치마크해봤는데, 동시 접속 50명 이하에서는 캐시 유무의 p95 차이가 10ms 미만이었습니다. 오히려 캐시 무효화 복잡도가 올라가서 순손해였고요. 반면 동시 접속 300명부터는 확실한 차이가 났습니다."

### 기술 선택 관점

> "기술 도입은 문제가 먼저 있어야 한다고 생각합니다. 예를 들어 로그인 Rate Limiting은 현재 DB 기반으로 구현했는데, 정상 트래픽에서는 충분합니다. 다만 브루트포스 공격 시에는 DB write 폭주가 문제가 되므로, 실험 프로젝트에서 Redis Counter 방식과 비교해봤습니다. 공격 시나리오에서 DB 부하가 90% 감소했지만, 정상 상황에서는 차이가 미미해서 현재 규모에서는 도입하지 않았습니다."

### 동시성 제어 관점

> "선착순 행사 등록에서 Atomic SQL UPDATE를 사용했는데, 단일 서버에서는 DB 레벨 원자성으로 충분합니다. 실험 프로젝트에서 서버 2대 환경을 구성해 분산 락과 비교해봤는데, 단일 서버에서는 오히려 Redis 왕복 비용이 추가되어 응답 시간이 느려졌습니다. 멀티 인스턴스 전환 시점에 도입하면 됩니다."

## 우선순위

### Redis 시나리오 (IGRUS-Web 연계)

| 순위 | 시나리오 | 이유 | 소요 시간 |
|---|---|---|---|
| 1 | 게시글 캐시 (시나리오 1) | 가장 흔한 면접 주제, 임계점 그래프가 강력 | 1일 |
| 2 | 로그인 Rate Limit (시나리오 2) | 보안 + 성능을 동시에 어필, 코드 비교도 직관적 | 반나절 |
| 3 | 선착순 분산 락 (시나리오 3) | 동시성 제어는 시니어급 질문이지만 신입이 답하면 임팩트 큼 | 1일 |
| 4 | 세션 저장소 (시나리오 6) | JWT 선택 근거를 강화, 스케일아웃 이해 증명 | 반나절 |
| 5 | 작성 Rate Limit (시나리오 4) | Sliding Window 알고리즘 이해도 어필 | 반나절 |
| 6 | 조회수 Write-Behind (시나리오 5) | Write-Behind 패턴은 심화 주제, 여유 있을 때 | 1일 |

### Kafka 시나리오 (선택)

| 순위 | 시나리오 | 이유 |
|---|---|---|
| 7 | 동기 vs 비동기 (시나리오 3) | Kafka를 언급하고 싶다면 이것부터 |
| 8 | 이벤트 유실 vs 보장 (시나리오 4) | 심화 학습 |
