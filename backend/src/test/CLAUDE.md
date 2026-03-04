# 테스트 코드 작성 규칙

> backend/CLAUDE.md의 테스트 섹션(8절)과 함께 적용되는 **구체적인 구현 패턴**.

## 1. 테스트 베이스 클래스 계층

```
ServiceIntegrationTestBase                ← @SpringBootTest 통합 테스트 베이스
├── 서비스/리포지토리 통합 테스트            ← 직접 상속
└── ControllerIntegrationTestBase         ← + @AutoConfigureMockMvc (HTTP 테스트)
    └── 컨트롤러 통합 테스트

PasswordAuthControllerTestBase            ← @WebMvcTest 슬라이스 테스트 베이스 (별도 컨텍스트)
└── 컨트롤러 슬라이스 테스트
```

- 새 통합 테스트 작성 시 반드시 `ServiceIntegrationTestBase`를 상속
- HTTP 요청이 필요하면 `ControllerIntegrationTestBase`를 상속
- 컨트롤러 단위(슬라이스) 테스트는 `PasswordAuthControllerTestBase`를 상속

## 2. 외부 서비스 Mock 패턴

### @SpringBootTest (통합 테스트) — `@MockitoBean` 사용 금지

외부 서비스는 `TestExternalServiceConfig`에서 `@TestConfiguration` + `@Primary @Bean`으로 Mock을 제공한다.
테스트에서는 `@Autowired`로 주입받고, `@BeforeEach`에서 `Mockito.reset()`으로 초기화한다.

```java
// Good
@Autowired
private AuthEmailService authEmailService;

@BeforeEach
void setUp() {
    setUpBase();
    Mockito.reset(authEmailService);
}
```

```java
// Bad — 컨텍스트 캐싱이 깨져서 테스트 전체 속도가 느려짐
@MockitoBean
private AuthEmailService authEmailService;
```

**MockBean으로 등록된 외부 서비스 목록** (`TestExternalServiceConfig`):
- `AuthEmailService`
- `BaebdungiWebhookService`
- `Clock`

### @WebMvcTest (슬라이스 테스트) — `@MockitoBean` 허용

슬라이스 테스트에서는 `@MockitoBean`을 사용하며, 모든 선언은 `PasswordAuthControllerTestBase`에 집중한다.

## 3. 컨텍스트 캐싱 보호 규칙

Spring Test Context 재사용을 위해, **서브클래스에서 다음을 추가하지 않는다**:

- `@MockitoBean` (Mock 조합이 달라지면 새 컨텍스트 생성)
- `@Import` (Import 조합이 달라지면 새 컨텍스트 생성)
- `@ActiveProfiles` (프로파일이 달라지면 새 컨텍스트 생성)

어노테이션 변경이 필요하면 베이스 클래스를 수정한다.

## 4. 테스트 데이터 생성 — 베이스 클래스 헬퍼 사용

`ServiceIntegrationTestBase`가 제공하는 헬퍼 메서드를 사용한다.
테스트 클래스에 중복 로컬 헬퍼를 만들지 않는다.

```java
// Good — 베이스 클래스 메서드
User user = createAndSaveUser(studentId, email, UserRole.ASSOCIATE, UserStatus.ACTIVE);
createAndSaveCredential(user, password, UserStatus.ACTIVE);

// Bad — 로컬에 중복 헬퍼 정의
private User createAndSaveTestUser(UserRole role, UserStatus status) { ... }
```

### 주요 헬퍼 메서드

| 메서드 | 설명 |
|--------|------|
| `createUser(studentId, email, role)` | User 생성 (저장 안 함, ACTIVE) |
| `createUser(studentId, email, role, status)` | User 생성 (저장 안 함, 지정 상태) |
| `createAndSaveUser(studentId, email, role)` | User 생성 + 저장 (ACTIVE) |
| `createAndSaveUser(studentId, email, role, status)` | User 생성 + 저장 (지정 상태) |
| `createAndSaveUnverifiedUser(studentId, email, role)` | PENDING_VERIFICATION 상태로 생성 + 저장 |
| `createAndSaveCredential(user, password)` | PasswordCredential 생성 + 저장 (ACTIVE) |
| `createAndSaveCredential(user, password, status)` | PasswordCredential 생성 + 저장 (지정 상태) |

## 5. 엔티티 기본 상태

- `User.create()` → 기본 상태 **ACTIVE**
- `PasswordCredential.create()` → 기본 상태 **ACTIVE**
- PENDING_VERIFICATION 상태는 `ReflectionTestUtils.setField`로 설정 (베이스 클래스 내부에서 처리)

## 6. 코드 위생 규칙

- 미사용 import 금지
- 죽은 코드 (선언만 있고 호출되지 않는 메서드) 금지
- 주석 처리된 코드 금지 (`//assertThat(...)` 등)
- 서비스가 의존하지 않는 Mock을 `@Autowired` + `reset()` 하지 않음
- FQCN 사용 금지 — import 추가 후 단순명 사용
- `@Transactional` 테스트에 사용 금지

## 7. OpenAPI 응답 스키마 검증

컨트롤러 통합 테스트에서 2xx 성공 응답은 반드시 `OpenApiValidatorUtil.matchesOpenApiSpec()`으로 OpenAPI 스펙 일치를 검증한다.

```java
import static igrus.web.common.OpenApiValidatorUtil.matchesOpenApiSpec;

mockMvc.perform(get("/api/v1/boards").with(withAuth(user)))
    .andDo(print())
    .andExpect(status().isOk())
    .andExpect(matchesOpenApiSpec());
```

- 새 컨트롤러 테스트 작성 시 **성공 응답에 `matchesOpenApiSpec()` 추가 필수**
- 4xx/5xx 에러 응답(401, 403, 404 등)에는 추가하지 않음
- 스키마 검증 실패 시: OpenAPI 스펙(`openapi/schemas/`)과 컨트롤러 응답 중 어느 쪽이 잘못인지 확인 후 수정

## 8. 컨텍스트 캐싱 목표

위 규칙을 지키면 Spring Context 로딩 횟수를 최소화할 수 있다.
새 테스트 추가 시 기존 베이스 클래스를 상속하고 어노테이션을 추가하지 않으면 컨텍스트가 늘어나지 않는다.
